package file.job;

import file.processor.FileParserProcessor;
import file.reader.SqlReader;
import file.writer.TextStepWriter;
import file.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppStepJob {

    private final Path inputPath;
    private final SqlReader reader;
    private final FileParserProcessor processor;
    private final TextStepWriter writer;

    public AppStepJob(Path inputPath, SqlReader reader, FileParserProcessor processor, TextStepWriter writer) {
        this.inputPath = inputPath;
        this.reader = reader;
        this.processor = processor;
        this.writer = writer;
    }

    public static AppStepJob createJob() {
        Path input = Paths.get("D:", "11. Project", "11. DB", "BigQuery");
        SqlReader reader = new SqlReader(SqlReader.DEFAULT_CHARSET);
        FileParserProcessor processor = FileParserProcessor.withDefaults();
        TextStepWriter writer = new TextStepWriter(TextStepWriter.DEFAULT_OUTPUT_DIR, Charset.forName("UTF-8"));
        return new AppStepJob(input, reader, processor, writer);
    }

    public static AppStepJob createJob(String inputPath) {
        Path input = Paths.get(inputPath);
        SqlReader reader = new SqlReader(SqlReader.DEFAULT_CHARSET);
        FileParserProcessor processor = FileParserProcessor.withDefaults();
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
            Map<String, TablesInfo> stepTables = processSteps(sql);
            write(file, stepTables);
        } catch (IOException ex) {
            System.err.println("Step file processing failed: " + file + " - " + ex.getMessage());
        }
    }

    private Map<String, TablesInfo> processSteps(String sql) {
        Map<String, TablesInfo> stepTables = new LinkedHashMap<>();

        Pattern stepPattern = Pattern.compile(
            "(?i)--\\s*STEP(\\d+)|/\\*\\s*STEP(\\d+)\\s*\\*/",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = stepPattern.matcher(sql);

        int lastStart = 0;
        String lastStep = null;

        while (matcher.find()) {
            if (lastStep != null) {
                String stepSql = sql.substring(lastStart, matcher.start());
                if (!stepSql.trim().isEmpty()) {
                    TablesInfo info = processor.parse(stepSql);
                    stepTables.put(lastStep, info);
                    System.out.println("[StepProcessor] " + lastStep +
                                      " - Sources: " + info.getSources().size() +
                                      ", Targets: " + info.getTargets().size());
                }
            }

            String stepNum = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            lastStep = "STEP" + String.format("%03d", Integer.parseInt(stepNum));
            lastStart = matcher.end();
        }

        if (lastStep != null) {
            String stepSql = sql.substring(lastStart);
            if (!stepSql.trim().isEmpty()) {
                TablesInfo info = processor.parse(stepSql);
                stepTables.put(lastStep, info);
                System.out.println("[StepProcessor] " + lastStep +
                                  " - Sources: " + info.getSources().size() +
                                  ", Targets: " + info.getTargets().size());
            }
        }

        if (stepTables.isEmpty()) {
            TablesInfo info = processor.parse(sql);
            stepTables.put("STEP000", info);
            System.out.println("[StepProcessor] No step markers found, processing as STEP000");
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

