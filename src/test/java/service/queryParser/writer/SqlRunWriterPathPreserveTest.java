package service.queryParser.writer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.queryParser.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlRunWriterPathPreserveTest {

    @TempDir
    Path tempDir;

    @Test
    void writeSqlFiles_preservesSubdirectoryStructure() throws IOException {
        Path outputDir = tempDir.resolve("out");
        SqlRunWriter writer = new SqlRunWriter(outputDir, StandardCharsets.UTF_8, "20260224");

        TablesInfo info = new TablesInfo();
        info.addSource("DW.RED_CARE_SALES");
        info.addTarget("DM.SERVICE");

        writer.writeSqlFiles("qa/sub/job1", info);

        assertTrue(Files.exists(outputDir.resolve("qa/sub/job1_bq.sql")));
        assertTrue(Files.exists(outputDir.resolve("qa/sub/job1_oracle.sql")));
    }
}

