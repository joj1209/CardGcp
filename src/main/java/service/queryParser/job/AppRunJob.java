package service.queryParser.job;

import service.queryParser.processor.FileParserProcessor;
import service.queryParser.reader.SqlReader;
import service.queryParser.writer.SqlRunWriter;
import service.queryParser.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppRunJob {

    private static final Path DEFAULT_INPUT_PATH = Paths.get("sql", "in");
    private static final Path DEFAULT_OUTPUT_PATH = Paths.get("sql", "out");
    private static final String DEFAULT_BASE_DATE = "20260224";

    private final Path inputDir;
    private final SqlReader reader;
    private final FileParserProcessor processor;
    private final SqlRunWriter writer;

    public AppRunJob(Path inputDir, SqlReader reader, FileParserProcessor processor, SqlRunWriter writer) {
        this.inputDir = inputDir;
        this.reader = reader;
        this.processor = processor;
        this.writer = writer;
    }

    public static AppRunJob createDefault(String baseDate) {
        SqlReader reader = new SqlReader(SqlReader.DEFAULT_CHARSET);
        FileParserProcessor processor = FileParserProcessor.withDefaults();
        SqlRunWriter writer = new SqlRunWriter(DEFAULT_OUTPUT_PATH, StandardCharsets.UTF_8, baseDate);
        return new AppRunJob(DEFAULT_INPUT_PATH, reader, processor, writer);
    }

    public void stepRead() {
        System.out.println("========================================");
        System.out.println("Starting SQL file processing for Run Job...");
        System.out.println("Input directory: " + inputDir);
        System.out.println("========================================");

        reader.run(inputDir, this::handleFile);

        System.out.println("========================================");
        System.out.println("All SQL files processed successfully.");
        System.out.println("========================================");
    }

    private void handleFile(Path file, String sql) {
        try {
            TablesInfo info = stepParse(sql);
            stepWrite(file, info);
        } catch (IOException ex) {
            System.err.println("File processing failed: " + file + " - " + ex.getMessage());
        }
    }

    private TablesInfo stepParse(String sql) {
        return processor.parse(sql);
    }

    private void stepWrite(Path file, TablesInfo info) throws IOException {
        String fileName = buildOutputFileName(file);
        writer.writeSqlFiles(fileName, info);
    }

    private String buildOutputFileName(Path file) {
        Path relative = inputDir.relativize(file);
        String name = relative.toString().replace("\\", "/");
        return name.replaceAll("\\.sql$", "");
    }

    public static void main(String[] args) {
        String baseDate = DEFAULT_BASE_DATE;

        if (args.length > 0) {
            baseDate = args[0];
            System.out.println("Using base date from argument: " + baseDate);
        } else {
            System.out.println("Using default base date: " + baseDate);
        }

        AppRunJob job = createDefault(baseDate);
        job.stepRead();
    }
}
