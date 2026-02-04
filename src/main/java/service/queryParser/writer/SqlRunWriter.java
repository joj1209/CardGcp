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
        String baseName = extractBaseName(relativeFileName);

        String bqContent = generateBigQuerySql(info);
        Path bqPath = outputDir.resolve(baseName + "_bq.sql");
        writeFile(bqPath, bqContent);

        String oracleContent = bqContent.replace("`", "");
        Path oraPath = outputDir.resolve(baseName + "_oracle.sql");
        writeFile(oraPath, oracleContent);

        System.out.println("✓ Generated SQL files: " + baseName + "_bq.sql, " + baseName + "_oracle.sql");
    }

    private String extractBaseName(String fileName) {
        String name = fileName.replace("\\", "/");
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
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
            sb.append(generateTableQueries(table));
            sb.append("\n");
        }

        sb.append("/*--------------------*/\n");
        sb.append("/*-- 타겟테이블 : ").append(targets.size()).append("개 --*/\n");
        sb.append("/*--------------------*/\n\n");

        index = 1;
        for (String table : targets) {
            sb.append("/*-- ").append(index++).append(") ").append(table).append(" --*/\n");
            sb.append(generateTableQueries(table));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String generateTableQueries(String fullTableName) {
        StringBuilder sb = new StringBuilder();

        String tableRef = addBackticksIfNeeded(fullTableName);

        sb.append("select * from ").append(tableRef).append(";\n");
        sb.append("select * from ").append(tableRef)
                .append(" where `파티션일자` = parse_date('%Y%m%d', '").append(baseDate).append("');\n");
        sb.append("select count(1) from ").append(tableRef).append(";\n");
        sb.append("select `파티션일자`,count(1) from ").append(tableRef)
                .append(" group by `파티션일자` order by `파티션일자` desc;\n");
        sb.append("select count(1) from ").append(tableRef)
                .append(" where `파티션일자` = parse_date('%Y%m%d', '").append(baseDate).append("');\n");

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

