package service.queryParser.writer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.queryParser.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlRunWriterOracleDateLiteralTest {

    @TempDir
    Path tempDir;

    @Test
    void oracleSql_usesDateLiteralInsteadOfParseDate() throws IOException {
        Path outputDir = tempDir.resolve("out");
        SqlRunWriter writer = new SqlRunWriter(outputDir, StandardCharsets.UTF_8, "20260224");

        TablesInfo info = new TablesInfo();
        info.addSource("DW.RED_CARE_SALES");

        writer.writeSqlFiles("qa/bq_dw_red_care_sales_05", info);

        Path oracleFile = outputDir.resolve("qa/bq_dw_red_care_sales_05_oracle.sql");
        String oracleContent = Files.readString(oracleFile);

        assertFalse(oracleContent.contains("parse_date"));
        assertTrue(oracleContent.contains("'20260224'"));
    }

    @Test
    void bigQuerySql_stillUsesParseDate() throws IOException {
        Path outputDir = tempDir.resolve("out");
        SqlRunWriter writer = new SqlRunWriter(outputDir, StandardCharsets.UTF_8, "20260224");

        TablesInfo info = new TablesInfo();
        info.addSource("DW.RED_CARE_SALES");

        writer.writeSqlFiles("qa/bq_dw_red_care_sales_05", info);

        Path bqFile = outputDir.resolve("qa/bq_dw_red_care_sales_05_bq.sql");
        String bqContent = Files.readString(bqFile);

        assertTrue(bqContent.contains("parse_date('%Y%m%d', '20260224')"));
    }
}

