package service.queryParser.pattern;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 테이블명 패턴 정의 클래스
 *
 * 지원 패턴:
 * - `table`           : 백틱으로 감싼 테이블명
 * - schema.`table`    : 스키마.백틱 테이블
 * - schema.table      : 스키마.테이블
 * - table             : 단순 테이블명
 */
public class TableNamePattern {

    /**
     * 테이블명 정규식 패턴
     * - 백틱으로 감싼 테이블명
     * - 스키마.백틱 테이블
     * - 스키마.테이블
     * - 단순 테이블명
     */
    public static final String TABLE_NAME_REGEX =
            "("
                    + "`[^`]+`"
                    + "|(?:[A-Za-z0-9_$\\p{L}-]+|`[^`]+`)(?:\\.(?:[A-Za-z0-9_$\\p{L}-]+|`[^`]+`))*"
                    + "|[A-Za-z0-9_$\\p{L}-]+\\.`[^`]+`"
                    + "|[A-Za-z0-9_$\\p{L}-]+\\.[A-Za-z0-9_$\\p{L}-]+"
                    + ")";


    /**
     * 제외할 SQL 키워드
     */
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "JOIN", "LEFT", "RIGHT",
        "INNER", "OUTER", "ON", "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE",
        "ORDER", "GROUP", "BY", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT",
        "AS", "INTO", "VALUES", "SET", "CASCADE", "RESTRICT", "PUBLIC", "PRIVATE",
        "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "SAVEPOINT", "TRANSACTION", "BEGIN",
        "END", "IF", "THEN", "ELSE", "CASE", "WHEN", "NULL", "TRUE", "FALSE", "USING"
    ));

    /**
     * 테이블명이 유효한지 검사
     *
     * @param tableName 테이블명
     * @return 유효한 테이블명이면 true
     */
    public static boolean isValidTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return false;
        }

        String cleaned = cleanTableName(tableName);
        String withoutBacktick = cleaned.replaceAll("`", "");

        // 키워드가 아니고, 2자 이상인 경우만 유효
        return !KEYWORDS.contains(withoutBacktick.toUpperCase()) && withoutBacktick.length() > 1;
    }

    /**
     * 테이블명 정리 (끝의 불필요한 문자 제거)
     *
     * @param name 원본 테이블명
     * @return 정리된 테이블명
     */
    public static String cleanTableName(String name) {
        if (name == null) return "";

        String cleaned = name.trim();
        while (!cleaned.isEmpty() && isTrailingChar(cleaned.charAt(cleaned.length() - 1))) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    /**
     * 제거해야 할 끝 문자 체크
     *
     * @param c 문자
     * @return 제거해야 할 문자이면 true
     */
    private static boolean isTrailingChar(char c) {
        return c == ',' || c == ';' || c == ')' || c == '\r' || c == '\n';
    }

    /**
     * 테이블명 패턴 컴파일
     *
     * @param keyword SQL 키워드 (예: "FROM", "INSERT INTO")
     * @return 컴파일된 패턴
     */
    public static Pattern buildPattern(String keyword) {
        return Pattern.compile("(?is)\\b" + keyword + "\\s+" + TABLE_NAME_REGEX);
    }
}
