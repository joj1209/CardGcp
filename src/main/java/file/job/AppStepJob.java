package file.job;

import file.processor.FileParserProcessor;
import file.reader.SqlReader;
import file.writer.TextStepWriter;
import file.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppStepJob {

    private final Path inputDir;
    private final SqlReader reader;
    private final FileParserProcessor processor;
    private final TextStepWriter writer;

    public AppStepJob(Path inputDir, SqlReader reader, FileParserProcessor processor, TextStepWriter writer) {
        this.inputDir = inputDir;
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

    public void execute() {
        reader.run(inputDir, this::processFile);
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

        int lastEnd = 0;
        String currentStep = "STEP000";
        StringBuilder currentSql = new StringBuilder();

        while (matcher.find()) {
            if (lastEnd > 0) {
                String stepSql = currentSql.toString();
                if (!stepSql.trim().isEmpty()) {
                    TablesInfo info = processor.parse(stepSql);
                    stepTables.put(currentStep, info);
                    System.out.println("[StepProcessor] " + currentStep +
                                      " - Sources: " + info.getSources().size() +
                                      ", Targets: " + info.getTargets().size());
                }
            }

            String stepNum = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            currentStep = "STEP" + String.format("%03d", Integer.parseInt(stepNum));
            currentSql = new StringBuilder();
            lastEnd = matcher.end();
        }

        if (lastEnd > 0) {
            String stepSql = sql.substring(lastEnd);
            if (!stepSql.trim().isEmpty()) {
                TablesInfo info = processor.parse(stepSql);
                stepTables.put(currentStep, info);
                System.out.println("[StepProcessor] " + currentStep +
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
        writer.writeStepTables(inputDir, file, stepTables);
    }

    public static void main(String[] args) {
        AppStepJob job = createJob();
        job.execute();
    }
}

