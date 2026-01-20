package service.queryParser.job;

import service.queryParser.processor.FileParserProcessor;
import service.queryParser.reader.SqlReader;
import service.queryParser.writer.CsvWriter;
import service.queryParser.writer.SourceTableCsvWriter;
import service.queryParser.writer.TextWriter;
import service.queryParser.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppJob {

    private static final Path DEFAULT_INPUT_PATH = Paths.get("D:", "11. Project", "11. DB", "BigQuery");
    private static final Path DEFAULT_OUTPUT_PATH = Paths.get("D:", "11. Project", "11. DB", "BigQuery_out");

    private final Path inputDir;
    private final SqlReader reader;
    private final FileParserProcessor processor;
    private final TextWriter writer;
    private final CsvWriter csvWriter;
    private final SourceTableCsvWriter sourceTableCsvWriter;

    public AppJob(Path inputDir, SqlReader reader, FileParserProcessor processor, TextWriter writer,
                  CsvWriter csvWriter, SourceTableCsvWriter sourceTableCsvWriter) {
        this.inputDir = inputDir;
        this.reader = reader;
        this.processor = processor;
        this.writer = writer;
        this.csvWriter = csvWriter;
        this.sourceTableCsvWriter = sourceTableCsvWriter;
    }

    public static AppJob createDefault() {
        SqlReader reader = new SqlReader(SqlReader.DEFAULT_CHARSET);
        FileParserProcessor processor = FileParserProcessor.withDefaults();
        TextWriter writer = new TextWriter(DEFAULT_OUTPUT_PATH, Charset.forName("UTF-8"));
        Path csvPath = DEFAULT_OUTPUT_PATH.resolve("summary.csv");
        CsvWriter csvWriter = new CsvWriter(csvPath, Charset.forName("UTF-8"));
        Path sourceTableCsvPath = DEFAULT_OUTPUT_PATH.resolve("source_table_mapping.csv");
        SourceTableCsvWriter sourceTableCsvWriter = new SourceTableCsvWriter(sourceTableCsvPath, Charset.forName("UTF-8"));
        return new AppJob(DEFAULT_INPUT_PATH, reader, processor, writer, csvWriter, sourceTableCsvWriter);
    }

    public void stepRead() {
        System.out.println("========================================");
        System.out.println("Starting SQL file processing...");
        System.out.println("Input directory: " + inputDir);
        System.out.println("========================================");

        reader.run(inputDir, this::handleFile);

        try {
            csvWriter.write();
            System.out.println("========================================");
            System.out.println("CSV file saved successfully.");
            System.out.println("Total records: " + csvWriter.getRecordCount());
            System.out.println("========================================");
        } catch (IOException ex) {
            System.err.println("Failed to save CSV file: " + ex.getMessage());
        }

        try {
            sourceTableCsvWriter.write();
            System.out.println("========================================");
            System.out.println("Source table mapping CSV file saved successfully.");
            System.out.println("Total source tables: " + sourceTableCsvWriter.getTableCount());
            System.out.println("========================================");
        } catch (IOException ex) {
            System.err.println("Failed to save source table mapping CSV file: " + ex.getMessage());
        }
    }

    private void handleFile(Path file, String sql) {
        try {
            TablesInfo info = stepParse(sql);
            stepWrite(file, info);

            String fileName = file.getFileName().toString();
            csvWriter.addRecord(fileName, info);
            sourceTableCsvWriter.addRecord(fileName, info);
        } catch (IOException ex) {
            System.err.println("File processing failed: " + file + " - " + ex.getMessage());
        }
    }

    private TablesInfo stepParse(String sql) {
        return processor.parse(sql);
    }

    private void stepWrite(Path file, TablesInfo info) throws IOException {
        String fileName = buildOutputFileName(file);
        writer.writeTables(fileName, info);
    }

    private String buildOutputFileName(Path file) {
        Path relative = inputDir.relativize(file);
        String name = relative.toString().replace("\\", "/");
        return name.replaceAll("\\.sql$", "_sql_tables.txt");
    }

    public static void main(String[] args) {
        AppJob job = createDefault();
        job.stepRead();
    }
}