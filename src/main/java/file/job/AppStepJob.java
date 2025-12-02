package file.job;

import file.processor.FileStepParserProcessor;
import file.reader.SqlReader;
import file.writer.TextStepWriter;
import file.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * STEP별로 SQL 파일을 처리하는 Job 클래스
 *
 * 연결 구조:
 * AppStepJob -> FileStepParserProcessor -> TableStepParser
 */
public class AppStepJob {

    private final Path inputPath;
    private final SqlReader reader;
    private final FileStepParserProcessor processor;
    private final TextStepWriter writer;

    public AppStepJob(Path inputPath, SqlReader reader, FileStepParserProcessor processor, TextStepWriter writer) {
        this.inputPath = inputPath;
        this.reader = reader;
        this.processor = processor;
        this.writer = writer;
    }

    public static AppStepJob createJob() {
        Path input = Paths.get("D:", "11. Project", "11. DB", "BigQuery");
        SqlReader reader = new SqlReader(SqlReader.DEFAULT_CHARSET);
        FileStepParserProcessor processor = FileStepParserProcessor.withDefaults();
        TextStepWriter writer = new TextStepWriter(TextStepWriter.DEFAULT_OUTPUT_DIR, Charset.forName("UTF-8"));
        return new AppStepJob(input, reader, processor, writer);
    }

    public static AppStepJob createJob(String inputPath) {
        Path input = Paths.get(inputPath);
        SqlReader reader = new SqlReader(SqlReader.DEFAULT_CHARSET);
        FileStepParserProcessor processor = FileStepParserProcessor.withDefaults();
        TextStepWriter writer = new TextStepWriter(TextStepWriter.DEFAULT_OUTPUT_DIR, Charset.forName("UTF-8"));
        return new AppStepJob(input, reader, processor, writer);
    }

    public void execute() {
        if (Files.isDirectory(inputPath)) {
            System.out.println("[AppStepJob] Processing directory: " + inputPath);
            reader.run(inputPath, this::processFile);
        } else if (Files.isRegularFile(inputPath)) {
            System.out.println("[AppStepJob] Processing single file: " + inputPath);
            processSingleFile(inputPath);
        } else {
            System.err.println("[AppStepJob] Invalid path (not a file or directory): " + inputPath);
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
            Map<String, TablesInfo> stepTables = process(sql);
            write(file, stepTables);
        } catch (IOException ex) {
            System.err.println("Step file processing failed: " + file + " - " + ex.getMessage());
        }
    }

    private Map<String, TablesInfo> process(String sql) {
        Map<String, TablesInfo> stepTables = processor.parse(sql);

        // 각 STEP 처리 로그 출력
        for (Map.Entry<String, TablesInfo> entry : stepTables.entrySet()) {
            String stepName = entry.getKey();
            TablesInfo info = entry.getValue();
            System.out.println("[StepProcessor] " + stepName +
                              " - Sources: " + info.getSources().size() +
                              ", Targets: " + info.getTargets().size());
        }

        return stepTables;
    }

    private void write(Path file, Map<String, TablesInfo> stepTables) throws IOException {
        Path baseDir = Files.isDirectory(inputPath) ? inputPath : inputPath.getParent();
        writer.writeStepTables(baseDir, file, stepTables);
    }

    public static void main(String[] args) {
        AppStepJob job;
        if (args.length > 0) {
            job = createJob(args[0]);
        } else {
            job = createJob();
        }
        job.execute();
    }
}

