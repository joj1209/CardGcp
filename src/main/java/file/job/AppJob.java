package file.job;

import file.processor.FileParserProcessor;
import file.reader.SqlReader;
import file.writer.TextWriter;
import file.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppJob {

    private final Path inputDir;
    private final SqlReader reader;
    private final FileParserProcessor processor;
    private final TextWriter writer;

    public AppJob(Path inputDir, SqlReader reader, FileParserProcessor processor, TextWriter writer) {
        this.inputDir = inputDir;
        this.reader = reader;
        this.processor = processor;
        this.writer = writer;
    }

    public static AppJob buildFactory() {
        Path input = Paths.get("D:", "11. Project", "11. DB", "BigQuery");
        SqlReader reader = new SqlReader(SqlReader.DEFAULT_CHARSET);
        FileParserProcessor processor = FileParserProcessor.withDefaults();
        TextWriter writer = new TextWriter(TextWriter.DEFAULT_OUTPUT_DIR, Charset.forName("UTF-8"));
        return new AppJob(input, reader, processor, writer);
    }

    public void stepRead() {
        reader.run(inputDir, this::handleFile);
    }

    private void handleFile(Path file, String sql) {
        try {
            TablesInfo info = stepParse(sql);
            stepWrite(file, info);
        } catch (IOException ex) {
            System.err.println("파일 처리 실패: " + file + " - " + ex.getMessage());
        }
    }

    private TablesInfo stepParse(String sql) {
        return processor.parse(sql);
    }

    private void stepWrite(Path file, TablesInfo info) throws IOException {
        writer.writeTables(inputDir, file, info);
    }

    public static void main(String[] args) {
        AppJob job = buildFactory();
        job.stepRead();
    }
}
