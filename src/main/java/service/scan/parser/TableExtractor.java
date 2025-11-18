package service.scan.parser;

import service.scan.model.TablesInfo;

import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL에서 Source/Target 테이블 추출
 */
public class TableExtractor {
    // 백틱이 있는 테이블: schema.`table` 또는 `table`
    // 백틱이 없는 테이블: schema.table 또는 table
    private static final String TABLE_ID =
            "(`[^`]+`|[A-Za-z0-9_$.]+\\.`[^`]+`|[A-Za-z0-9_$.]+\\.[A-Za-z0-9_$]+|[A-Za-z0-9_$]+)";

    // 제외할 SQL 키워드들
    private static final Set<String> SQL_KEYWORDS = new HashSet<>();
    static {
        String[] keywords = {
            "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "JOIN", "LEFT", "RIGHT",
            "INNER", "OUTER", "ON", "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE",
            "ORDER", "GROUP", "BY", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT",
            "AS", "INTO", "VALUES", "SET", "CASCADE", "RESTRICT", "PUBLIC", "PRIVATE",
            "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "SAVEPOINT", "TRANSACTION", "BEGIN",
            "END", "IF", "THEN", "ELSE", "CASE", "WHEN", "NULL", "TRUE", "FALSE",
            "STDIN", "STDOUT", "STDERR", "DUAL", "SYSDATE", "CURRENT_DATE", "CURRENT_TIME"
        };
        for (String kw : keywords) {
            SQL_KEYWORDS.add(kw.toUpperCase());
        }
    }

    private final Pattern insertInto = Pattern.compile("(?is)\\bINSERT\\s+INTO\\s+" + TABLE_ID);
    private final Pattern updateTgt  = Pattern.compile("(?is)\\bUPDATE\\s+" + TABLE_ID);
    private final Pattern mergeInto  = Pattern.compile("(?is)\\bMERGE\\s+INTO\\s+" + TABLE_ID);
    private final Pattern deleteFrom = Pattern.compile("(?is)\\bDELETE\\s+FROM\\s+" + TABLE_ID);
    private final Pattern fromSrc    = Pattern.compile("(?is)\\bFROM\\s+" + TABLE_ID);
    private final Pattern joinSrc    = Pattern.compile("(?is)\\bJOIN\\s+" + TABLE_ID);

    public TablesInfo extractTables(String sql) {
        TablesInfo t = new TablesInfo();
        String s = sql.replaceAll("(?s)/\\*.*?\\*/", " ");
        findTables(s, insertInto, t.getTargets());
        findTables(s, updateTgt , t.getTargets());
        findTables(s, mergeInto , t.getTargets());
        findTables(s, deleteFrom, t.getTargets());
        findTables(s, fromSrc   , t.getSources());
        findTables(s, joinSrc   , t.getSources());
        return t;
    }

    private void findTables(String sql, Pattern p, Set<String> into) {
        Matcher m = p.matcher(sql);
        while (m.find()) {
            String tableName = clean(m.group(1));
            // 백틱 제거하여 키워드 체크
            String nameWithoutBacktick = tableName.replaceAll("`", "");
            // SQL 키워드가 아니고, 단일 문자가 아닌 경우만 추가
            if (!SQL_KEYWORDS.contains(nameWithoutBacktick.toUpperCase()) && nameWithoutBacktick.length() > 1) {
                into.add(tableName);
            }
        }
    }

    private String clean(String s) {
        if (s == null) return "";
        String t = s.trim();
        while (!t.isEmpty()) {
            char c = t.charAt(t.length() - 1);
            if (c == ',' || c == ';' || c == ')' || c == '\r' || c == '\n') t = t.substring(0, t.length() - 1).trim();
            else break;
        }
        return t;
    }
}

