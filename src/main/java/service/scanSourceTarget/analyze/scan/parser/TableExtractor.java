package service.scanSourceTarget.analyze.scan.parser;

import service.scanSourceTarget.analyze.scan.model.TablesInfo;

import java.util.Set;
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
        while (m.find()) into.add(clean(m.group(1)));
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


