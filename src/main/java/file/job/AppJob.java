package file.job;

import file.processor.FileParserProcessor;
import file.reader.SqlReader;
import file.writer.TextWriter;
import file.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppJob {

    private final Path inputPath;
    private final SqlReader reader;
    private final FileParserProcessor processor;
    private final TextWriter writer;

    public AppJob(Path inputPath, SqlReader reader, FileParserProcessor processor, TextWriter writer) {
        this.inputPath = inputPath;
        this.reader = reader;
        this.processor = processor;
        this.writer = writer;
    }

    public static AppJob createJob() {
        Path input = Paths.get("D:", "11. Project", "11. DB", "BigQuery");
        SqlReader reader = new SqlReader(SqlReader.DEFAULT_CHARSET);
        FileParserProcessor processor = FileParserProcessor.withDefaults();
        TextWriter writer = new TextWriter(TextWriter.DEFAULT_OUTPUT_DIR, Charset.forName("UTF-8"));
        return new AppJob(input, reader, processor, writer);
    }

    public static AppJob createJob(String inputPath) {
        Path input = Paths.get(inputPath);
        SqlReader reader = new SqlReader(SqlReader.DEFAULT_CHARSET);
        FileParserProcessor processor = FileParserProcessor.withDefaults();
        TextWriter writer = new TextWriter(TextWriter.DEFAULT_OUTPUT_DIR, Charset.forName("UTF-8"));
        return new AppJob(input, reader, processor, writer);
    }

    public void execute() {
        if (Files.isDirectory(inputPath)) {
            System.out.println("[AppJob] Processing directory: " + inputPath);
            reader.run(inputPath, this::processFile);
        } else if (Files.isRegularFile(inputPath)) {
            System.out.println("[AppJob] Processing single file: " + inputPath);
            processSingleFile(inputPath);
        } else {
            System.err.println("[AppJob] Invalid path (not a file or directory): " + inputPath);
        }
    }

    private void processSingleFile(Path file) {
        try {
            String sql = reader.readFile(file);
            processFile(file, sql);
        } catch (IOException ex) {
            System.err.println("File read failed: " + file + " - " + ex.getMessage());
        }
    }

    private void processFile(Path file, String sql) {
        try {
            TablesInfo info = process(sql);
            write(file, info);
        } catch (IOException ex) {
            System.err.println("File processing failed: " + file + " - " + ex.getMessage());
        }
    }

    private TablesInfo process(String sql) {
        return processor.parse(sql);
    }

    private void write(Path file, TablesInfo info) throws IOException {
        Path baseDir = Files.isDirectory(inputPath) ? inputPath : inputPath.getParent();
        writer.writeTables(baseDir, file, info);
    }

    public static void main(String[] args) {
        AppJob job;
        if (args.length > 0) {
            job = createJob(args[0]);
        } else {
            job = createJob();
        }
        job.execute();
    }
}
