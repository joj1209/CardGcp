package service.queryParser.writer;

import service.queryParser.vo.TablesInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class SqlRunWriter {

    private final Path outputDir;
    private final Charset charset;
    private final String baseDate;

    public SqlRunWriter(Path outputDir, Charset charset, String baseDate) {
        this.outputDir = outputDir;
        this.charset = charset;
        this.baseDate = baseDate;
    }

    public void writeSqlFiles(String relativeFileName, TablesInfo info) throws IOException {
        String relativeNoExt = normalizeRelativeNoExt(relativeFileName);

        String bqContent = generateBigQuerySql(info);
        Path bqPath = outputDir.resolve(relativeNoExt + "_bq.sql");
        writeFile(bqPath, bqContent);

        String oracleContent = generateOracleSql(info);
        Path oraPath = outputDir.resolve(relativeNoExt + "_oracle.sql");
        writeFile(oraPath, oracleContent);

        System.out.println("✓ Generated SQL files: " + relativeNoExt + "_bq.sql, " + relativeNoExt + "_oracle.sql");
    }

    /**
     * 입력 SQL 파일의 상대 경로를 그대로 유지하되, 출력 파일명 구성을 위해 확장자(.sql)만 제거합니다.
     * 예) "qa/bq_dw_red_care_sales_08" -> "qa/bq_dw_red_care_sales_08"
     * 예) "qa/bq_dw_red_care_sales_08.sql" -> "qa/bq_dw_red_care_sales_08"
     */
    private String normalizeRelativeNoExt(String relativeFileName) {
        String name = relativeFileName.replace("\\", "/");
        // AppRunJob에서 이미 .sql 제거 후 전달하지만, 안전하게 중복 제거
        return name.replaceAll("\\.sql$", "");
    }

    private String generateBigQuerySql(TablesInfo info) {
        StringBuilder sb = new StringBuilder();

        Set<String> sources = info.getSortedSources();
        Set<String> targets = info.getSortedTargets();

        sb.append("/*--------------------*/\n");
        sb.append("/*-- 소스테이블 : ").append(sources.size()).append("개 --*/\n");
        sb.append("/*--------------------*/\n\n");

        int index = 1;
        for (String table : sources) {
            sb.append("/*-- ").append(index++).append(") ").append(table).append(" --*/\n");
            sb.append(generateTableQueries(table, true));
            sb.append("\n");
        }

        sb.append("/*--------------------*/\n");
        sb.append("/*-- 타겟테이블 : ").append(targets.size()).append("개 --*/\n");
        sb.append("/*--------------------*/\n\n");

        index = 1;
        for (String table : targets) {
            sb.append("/*-- ").append(index++).append(") ").append(table).append(" --*/\n");
            sb.append(generateTableQueries(table, true));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String generateOracleSql(TablesInfo info) {
        StringBuilder sb = new StringBuilder();

        Set<String> sources = info.getSortedSources();
        Set<String> targets = info.getSortedTargets();

        sb.append("/*--------------------*/\n");
        sb.append("/*-- 소스테이블 : ").append(sources.size()).append("개 --*/\n");
        sb.append("/*--------------------*/\n\n");

        int index = 1;
        for (String table : sources) {
            sb.append("/*-- ").append(index++).append(") ").append(table).append(" --*/\n");
            sb.append(generateTableQueries(table, false));
            sb.append("\n");
        }

        sb.append("/*--------------------*/\n");
        sb.append("/*-- 타겟테이블 : ").append(targets.size()).append("개 --*/\n");
        sb.append("/*--------------------*/\n\n");

        index = 1;
        for (String table : targets) {
            sb.append("/*-- ").append(index++).append(") ").append(table).append(" --*/\n");
            sb.append(generateTableQueries(table, false));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String generateTableQueries(String fullTableName, boolean isBigQuery) {
        StringBuilder sb = new StringBuilder();

        // 테이블명에서 실제 테이블명 부분 추출 (스키마 제외)
        String tableName = fullTableName;
        if (fullTableName.contains(".")) {
            tableName = fullTableName.substring(fullTableName.lastIndexOf(".") + 1);
        }
        // 백틱 제거하여 순수 테이블명 확인
        tableName = tableName.replace("`", "");

        // 날짜 컬럼명 결정
        String dateColumnName;
        if (isBigQuery) {
            // BigQuery: 일 포함 여부로 결정
            dateColumnName = tableName.contains("일") ? "파티션일자" : "기준일자";
        } else {
            // Oracle: 일 포함시 파티션일자, 월 포함시 기준년월, 나머지는 기준일자
            if (tableName.contains("일")) {
                dateColumnName = "파티션일자";
            } else if (tableName.contains("월")) {
                dateColumnName = "기준년월";
            } else {
                dateColumnName = "기준일자";
            }
        }

        // 테이블 참조명 (BigQuery는 백틱 추가, Oracle은 백틱 제거)
        String tableRef = isBigQuery ? addBackticksIfNeeded(fullTableName) : fullTableName.replace("`", "");

        // 날짜 컬럼 참조 (BigQuery는 백틱 추가, Oracle은 백틱 제거)
        String dateColumnRef = isBigQuery ? "`" + dateColumnName + "`" : dateColumnName;

        // 날짜 조건 값 (BigQuery vs Oracle)
        String dateValueExpr = isBigQuery
                ? "parse_date('%Y%m%d', '" + baseDate + "')"
                : "'" + baseDate + "'";

        sb.append("select * from ").append(tableRef).append(";\n");
        sb.append("select * from ").append(tableRef)
                .append(" where ").append(dateColumnRef).append(" = ").append(dateValueExpr).append(";\n");
        sb.append("select count(1) from ").append(tableRef).append(";\n");
        sb.append("select ").append(dateColumnRef).append(",count(1) from ").append(tableRef)
                .append(" group by ").append(dateColumnRef).append(" order by ").append(dateColumnRef).append(" desc;\n");
        sb.append("select count(1) from ").append(tableRef)
                .append(" where ").append(dateColumnRef).append(" = ").append(dateValueExpr).append(";\n");

        return sb.toString();
    }

    private String addBackticksIfNeeded(String tableName) {
        if (tableName.contains("`")) {
            return tableName;
        }

        String[] parts = tableName.split("\\.");
        if (parts.length != 2) {
            return tableName;
        }

        String schema = parts[0];
        String table = parts[1];

        if (containsKorean(table)) {
            return schema + ".`" + table + "`";
        } else {
            return tableName;
        }
    }

    private boolean containsKorean(String text) {
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO ||
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_JAMO) {
                return true;
            }
        }
        return false;
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path, charset)) {
            writer.write(content);
        }
    }
}
