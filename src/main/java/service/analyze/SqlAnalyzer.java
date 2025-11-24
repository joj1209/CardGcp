package service.analyze;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlAnalyzer {
    private static final Pattern tablePattern = Pattern.compile("(?i)(FROM|JOIN)\\s+`?([\\w.-]+)`?");
    private static final Pattern selectStarPattern = Pattern.compile("(?i)SELECT\\s+\\*");

    public SqlStatistics analyze(String sql) {
        SqlStatistics stats = new SqlStatistics();

        Matcher matcher = tablePattern.matcher(sql);
        while (matcher.find()) {
            stats.addTable(matcher.group(2));
        }

        Matcher starMatcher = selectStarPattern.matcher(sql);
        if (starMatcher.find()) {
            stats.setUsesSelectAll(true);
        }

        // 더 많은 룰을 추가 가능: 하드코딩된 날짜, 함수 호출 등

        return stats;
    }
}

