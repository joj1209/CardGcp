package service.scan;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL?먯꽌 ?뚯씠釉?異붿텧 ?대떦 ?대옒??
 */
public class TableExtractor {

    // ?뚯씠釉??⑦꽩
    private static final String TABLE_ID =
            "([A-Za-z0-9_$.]+\\.`[^`]+`" +
            "|[A-Za-z0-9_$.]+\\.[A-Za-z0-9_$]+)";

    // ?寃??⑦꽩??
    private final Pattern insertInto;
    private final Pattern updateTgt;
    private final Pattern mergeInto;
    private final Pattern deleteFrom;

    // ?뚯뒪 ?⑦꽩??
    private final Pattern fromSrc;
    private final Pattern joinSrc;

    public TableExtractor() {
        this.insertInto = Pattern.compile("(?is)\\bINSERT\\s+INTO\\s+" + TABLE_ID);
        this.updateTgt = Pattern.compile("(?is)\\bUPDATE\\s+" + TABLE_ID);
        this.mergeInto = Pattern.compile("(?is)\\bMERGE\\s+INTO\\s+" + TABLE_ID);
        this.deleteFrom = Pattern.compile("(?is)\\bDELETE\\s+FROM\\s+" + TABLE_ID);
        this.fromSrc = Pattern.compile("(?is)\\bFROM\\s+" + TABLE_ID);
        this.joinSrc = Pattern.compile("(?is)\\bJOIN\\s+" + TABLE_ID);
    }

    /**
     * SQL?먯꽌 Source/Target ?뚯씠釉?異붿텧
     */
    public TablesInfo extractTables(String sql) {
        TablesInfo tables = new TablesInfo();

        // 釉붾줉 二쇱꽍 ?쒓굅 (INSERT /* comment */ INTO ???꾨꼍 ???
        String noBlockComments = sql.replaceAll("(?s)/\\*.*?\\*/", " ");

        // Target ?뚯씠釉?異붿텧
        findTables(noBlockComments, insertInto, tables.getTargets());
        findTables(noBlockComments, updateTgt, tables.getTargets());
        findTables(noBlockComments, mergeInto, tables.getTargets());
        findTables(noBlockComments, deleteFrom, tables.getTargets());

        // Source ?뚯씠釉?異붿텧
        findTables(noBlockComments, fromSrc, tables.getSources());
        findTables(noBlockComments, joinSrc, tables.getSources());

        return tables;
    }

    /**
     * ?⑦꽩 留ㅼ묶 ???뚯씠釉붾챸 ???
     */
    private void findTables(String sql, Pattern p, Set<String> into) {
        Matcher m = p.matcher(sql);
        while (m.find()) {
            into.add(cleanTableName(m.group(1)));
        }
    }

    /**
     * ?뚯씠釉붾챸?먯꽌 ; , ) 媛숈? ?앹옄 ?쒓굅
     */
    private String cleanTableName(String s) {
        if (s == null) return "";
        String t = s.trim();

        while (!t.isEmpty()) {
            char c = t.charAt(t.length() - 1);
            if (c == ',' || c == ';' || c == ')' || c == '\r' || c == '\n') {
                t = t.substring(0, t.length() - 1).trim();
            } else break;
        }
        return t;
    }
}

