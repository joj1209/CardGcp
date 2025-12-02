package file.parser;

import file.vo.TablesInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 쿼리에서 소스/타겟 테이블을 패턴별로 추출하는 클래스
 *
 * 주요 기능:
 * 1. 타겟 테이블 추출: INSERT, UPDATE, DELETE, MERGE 패턴
 * 2. 소스 테이블 추출: FROM, JOIN, USING, WITH 절 패턴
 * 3. 스텝별/전체 테이블 추출 지원
 */
public class TablePattern {

    // 테이블명 패턴: `table`, schema.`table`, schema.table, table
    private static final String TABLE_PATTERN =
        "(`[^`]+`|[A-Za-z0-9_$.\\p{L}]+\\.`[^`]+`|[A-Za-z0-9_$.\\p{L}]+\\.[A-Za-z0-9_$\\p{L}]+|[A-Za-z0-9_$\\p{L}]+)";

    // SQL 키워드 제외 목록
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "JOIN", "LEFT", "RIGHT",
        "INNER", "OUTER", "ON", "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE",
        "ORDER", "GROUP", "BY", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT",
        "AS", "INTO", "VALUES", "SET", "CASCADE", "RESTRICT", "PUBLIC", "PRIVATE",
        "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "SAVEPOINT", "TRANSACTION", "BEGIN",
        "END", "IF", "THEN", "ELSE", "CASE", "WHEN", "NULL", "TRUE", "FALSE", "USING"
    ));

    /**
     * SQL 문자열에서 소스/타겟 테이블을 추출합니다.
     */
    public TablesInfo extractTables(String sql) {
        TablesInfo info = new TablesInfo();
        String cleanedSql = removeComments(sql);

        // 타겟 테이블 추출
        extractTargetTables(cleanedSql, info);

        // 소스 테이블 추출
        extractSourceTables(cleanedSql, info);

        return info;
    }

    /**
     * SQL 주석을 제거합니다.
     */
    private String removeComments(String sql) {
        return sql.replaceAll("(?s)/\\*.*?\\*/", " ")
                  .replaceAll("--.*?(\r?\n|$)", " ");
    }

    // ========================================
    // 타겟 테이블 추출 메소드들
    // ========================================

    /**
     * 타겟 테이블을 추출합니다 (INSERT, UPDATE, DELETE, MERGE).
     */
    private void extractTargetTables(String sql, TablesInfo info) {
        extractInsertTargets(sql, info.getTargets());
        extractUpdateTargets(sql, info.getTargets());
        extractDeleteTargets(sql, info.getTargets());
        extractMergeTargets(sql, info.getTargets());
    }

    /**
     * INSERT INTO 패턴에서 타겟 테이블 추출
     */
    private void extractInsertTargets(String sql, Set<String> targets) {
        Pattern pattern = Pattern.compile("(?is)\\bINSERT\\s+INTO\\s+" + TABLE_PATTERN);
        extractTablesByPattern(sql, pattern, targets);
    }

    /**
     * UPDATE 패턴에서 타겟 테이블 추출
     */
    private void extractUpdateTargets(String sql, Set<String> targets) {
        Pattern pattern = Pattern.compile("(?is)\\bUPDATE\\s+" + TABLE_PATTERN);
        extractTablesByPattern(sql, pattern, targets);
    }

    /**
     * DELETE FROM 패턴에서 타겟 테이블 추출
     */
    private void extractDeleteTargets(String sql, Set<String> targets) {
        // DELETE FROM table
        Pattern pattern1 = Pattern.compile("(?is)\\bDELETE\\s+FROM\\s+" + TABLE_PATTERN);
        extractTablesByPattern(sql, pattern1, targets);

        // DELETE table (Oracle 방식)
        Pattern pattern2 = Pattern.compile("(?is)\\bDELETE\\s+" + TABLE_PATTERN + "\\s+(?:WHERE|$)");
        extractTablesByPattern(sql, pattern2, targets);
    }

    /**
     * MERGE INTO 패턴에서 타겟 테이블 추출
     */
    private void extractMergeTargets(String sql, Set<String> targets) {
        Pattern pattern = Pattern.compile("(?is)\\bMERGE\\s+INTO\\s+" + TABLE_PATTERN);
        extractTablesByPattern(sql, pattern, targets);
    }

    // ========================================
    // 소스 테이블 추출 메소드들
    // ========================================

    /**
     * 소스 테이블을 추출합니다 (FROM, JOIN, USING, WITH).
     */
    private void extractSourceTables(String sql, TablesInfo info) {
        extractFromTables(sql, info.getSources());
        extractJoinTables(sql, info.getSources());
        extractUsingTables(sql, info.getSources());
        extractWithTables(sql, info.getSources());
        extractOracleJoinTables(sql, info.getSources());
    }

    /**
     * FROM 절에서 소스 테이블 추출
     */
    private void extractFromTables(String sql, Set<String> sources) {
        Pattern pattern = Pattern.compile("(?is)\\bFROM\\s+" + TABLE_PATTERN);
        extractTablesByPattern(sql, pattern, sources);
    }

    /**
     * JOIN 절에서 소스 테이블 추출 (LEFT JOIN, INNER JOIN, JOIN)
     */
    private void extractJoinTables(String sql, Set<String> sources) {
        // LEFT [OUTER] JOIN
        Pattern leftJoin = Pattern.compile("(?is)\\bLEFT\\s+(?:OUTER\\s+)?JOIN\\s+" + TABLE_PATTERN);
        extractTablesByPattern(sql, leftJoin, sources);

        // INNER JOIN
        Pattern innerJoin = Pattern.compile("(?is)\\bINNER\\s+JOIN\\s+" + TABLE_PATTERN);
        extractTablesByPattern(sql, innerJoin, sources);

        // RIGHT [OUTER] JOIN
        Pattern rightJoin = Pattern.compile("(?is)\\bRIGHT\\s+(?:OUTER\\s+)?JOIN\\s+" + TABLE_PATTERN);
        extractTablesByPattern(sql, rightJoin, sources);

        // JOIN (일반)
        Pattern join = Pattern.compile("(?is)\\bJOIN\\s+" + TABLE_PATTERN);
        extractTablesByPattern(sql, join, sources);
    }

    /**
     * USING 절에서 소스 테이블 추출 (MERGE 구문)
     */
    private void extractUsingTables(String sql, Set<String> sources) {
        Pattern pattern = Pattern.compile("(?is)\\bUSING\\s+" + TABLE_PATTERN);
        extractTablesByPattern(sql, pattern, sources);
    }

    /**
     * WITH 절 (CTE)에서 소스 테이블 추출
     */
    private void extractWithTables(String sql, Set<String> sources) {
        // WITH 절 내부의 FROM, JOIN에서 테이블 추출
        Pattern withPattern = Pattern.compile("(?is)\\bWITH\\s+.*?\\bFROM\\s+" + TABLE_PATTERN);
        Matcher matcher = withPattern.matcher(sql);

        while (matcher.find()) {
            String tableName = matcher.group(1);
            addTableIfValid(tableName, sources);
        }
    }

    /**
     * Oracle 조인 문법에서 소스 테이블 추출 (FROM table1, table2)
     */
    private void extractOracleJoinTables(String sql, Set<String> sources) {
        Pattern pattern = Pattern.compile(
            "(?is)\\bFROM\\s+(.*?)(?=\\s+WHERE|\\s+GROUP|\\s+ORDER|\\s+HAVING|\\s+UNION|\\s+LIMIT|;|$)"
        );

        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String fromClause = matcher.group(1);

            // JOIN 키워드 이전까지만 처리
            int joinPos = fromClause.toUpperCase().indexOf("JOIN");
            if (joinPos > 0) {
                fromClause = fromClause.substring(0, joinPos);
            }

            // 콤마로 구분된 테이블 추출
            String[] tables = fromClause.split(",");
            for (String table : tables) {
                extractFirstTableFromClause(table.trim(), sources);
            }
        }
    }

    /**
     * 절에서 첫 번째 테이블명 추출 (별칭 제외)
     */
    private void extractFirstTableFromClause(String clause, Set<String> sources) {
        if (clause.isEmpty()) return;

        Pattern pattern = Pattern.compile("^\\s*" + TABLE_PATTERN);
        Matcher matcher = pattern.matcher(clause);

        if (matcher.find()) {
            String tableName = matcher.group(1);
            addTableIfValid(tableName, sources);
        }
    }

    // ========================================
    // 공통 유틸리티 메소드들
    // ========================================

    /**
     * 정규식 패턴으로 테이블명을 추출하여 Set에 추가
     */
    private void extractTablesByPattern(String sql, Pattern pattern, Set<String> tables) {
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            addTableIfValid(tableName, tables);
        }
    }

    /**
     * 유효한 테이블명인 경우 Set에 추가
     */
    private void addTableIfValid(String tableName, Set<String> tables) {
        String cleaned = cleanTableName(tableName);
        String withoutBacktick = cleaned.replaceAll("`", "");

        // 키워드 제외, 길이 체크
        if (!KEYWORDS.contains(withoutBacktick.toUpperCase()) && withoutBacktick.length() > 1) {
            tables.add(cleaned);
        }
    }

    /**
     * 테이블명 정리 (끝의 불필요한 문자 제거)
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
     * 제거해야 할 끝 문자 체크
     */
    private boolean isTrailingChar(char c) {
        return c == ',' || c == ';' || c == ')' || c == '\r' || c == '\n';
    }
}

