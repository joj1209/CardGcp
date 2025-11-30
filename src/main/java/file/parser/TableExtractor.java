package file.parser;

import file.vo.TablesInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL에서 소스/타겟 테이블을 추출하는 클래스
 * - DML 패턴 매칭: INSERT, UPDATE, DELETE, MERGE
 * - FROM 절 파싱: ANSI JOIN, Oracle 조인(콤마 구분) 모두 지원
 * - 한글 테이블명 지원
 */
public class TableExtractor {

    // 테이블명 패턴: schema.table, `table`, schema.`table` 등 (한글 지원)
    private static final String TABLE_PATTERN =
        "(`[^`]+`|[A-Za-z0-9_$.\\p{L}]+\\.`[^`]+`|[A-Za-z0-9_$.\\p{L}]+\\.[A-Za-z0-9_$\\p{L}]+|[A-Za-z0-9_$\\p{L}]+)";

    // 제외할 SQL 키워드
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "JOIN", "LEFT", "RIGHT",
        "INNER", "OUTER", "ON", "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE",
        "ORDER", "GROUP", "BY", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT",
        "AS", "INTO", "VALUES", "SET", "CASCADE", "RESTRICT", "PUBLIC", "PRIVATE",
        "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "SAVEPOINT", "TRANSACTION", "BEGIN",
        "END", "IF", "THEN", "ELSE", "CASE", "WHEN", "NULL", "TRUE", "FALSE"
    ));

    /**
     * SQL 문자열에서 소스/타겟 테이블 정보를 추출합니다.
     */
    public TablesInfo extractTables(String sql) {
        TablesInfo info = new TablesInfo();
        String cleanedSql = sql.replaceAll("(?s)/\\*.*?\\*/", " "); // 블록 주석 제거

        // 타겟 테이블 추출 (DML)
        extractByPattern(cleanedSql, "INSERT\\s+INTO\\s+", info.getTargets());
        extractByPattern(cleanedSql, "UPDATE\\s+", info.getTargets());
        extractByPattern(cleanedSql, "MERGE\\s+INTO\\s+", info.getTargets());
        extractByPattern(cleanedSql, "DELETE\\s+FROM\\s+", info.getTargets());

        // 소스 테이블 추출 (SELECT)
        extractByPattern(cleanedSql, "FROM\\s+", info.getSources());
        extractByPattern(cleanedSql, "JOIN\\s+", info.getSources());
        extractByPattern(cleanedSql, "USING\\s+", info.getSources());
        extractFromClause(cleanedSql, info.getSources()); // Oracle 조인(콤마 구분)

        return info;
    }

    /**
     * 정규식 패턴으로 테이블명을 추출하여 Set에 추가합니다.
     */
    private void extractByPattern(String sql, String keyword, Set<String> tables) {
        Pattern pattern = Pattern.compile("(?is)\\b" + keyword + TABLE_PATTERN);
        Matcher matcher = pattern.matcher(sql);

        while (matcher.find()) {
            addTableIfValid(matcher.group(1), tables);
        }
    }

    /**
     * FROM 절에서 콤마로 구분된 테이블들을 추출합니다 (Oracle 조인 문법).
     */
    private void extractFromClause(String sql, Set<String> sources) {
        Pattern pattern = Pattern.compile(
            "(?is)\\bFROM\\s+(.*?)(?=\\s+WHERE|\\s+GROUP|\\s+ORDER|\\s+HAVING|\\s+UNION|;|$)"
        );

        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String fromClause = matcher.group(1);

            // JOIN 이전까지만 처리
            int joinPos = fromClause.toUpperCase().indexOf("JOIN");
            if (joinPos > 0) {
                fromClause = fromClause.substring(0, joinPos);
            }

            // 콤마로 구분된 테이블 추출
            for (String part : fromClause.split(",")) {
                extractFirstTable(part.trim(), sources);
            }
        }
    }

    /**
     * 문자열의 첫 번째 테이블명을 추출합니다 (별칭 제외).
     */
    private void extractFirstTable(String text, Set<String> tables) {
        if (text.isEmpty()) return;

        Pattern pattern = Pattern.compile("^\\s*" + TABLE_PATTERN);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            addTableIfValid(matcher.group(1), tables);
        }
    }

    /**
     * 유효한 테이블명인 경우 Set에 추가합니다.
     */
    private void addTableIfValid(String tableName, Set<String> tables) {
        String cleaned = cleanTableName(tableName);
        String withoutBacktick = cleaned.replaceAll("`", "");

        // 키워드가 아니고, 2자 이상인 경우만 추가
        if (!KEYWORDS.contains(withoutBacktick.toUpperCase()) && withoutBacktick.length() > 1) {
            tables.add(cleaned);
        }
    }

    /**
     * 테이블명에서 불필요한 문자를 제거합니다.
     */
    private String cleanTableName(String name) {
        if (name == null) return "";

        String cleaned = name.trim();
        while (!cleaned.isEmpty() && isTrailingChar(cleaned.charAt(cleaned.length() - 1))) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    /**
     * 제거해야 할 끝 문자인지 확인합니다.
     */
    private boolean isTrailingChar(char c) {
        return c == ',' || c == ';' || c == ')' || c == '\r' || c == '\n';
    }
}
