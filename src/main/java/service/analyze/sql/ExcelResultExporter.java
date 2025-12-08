package service.analyze.sql;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExcelResultExporter {

    public void export(SqlFileExecutor.QueryResult result, Path outputFile) throws IOException {
        if (result.headers() == null || result.headers().isEmpty()) {
            throw new IllegalStateException("No columns returned from SQL execution");
        }

        Files.createDirectories(outputFile.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writeRow(writer, result.headers());
            for (List<Object> row : result.rows()) {
                writeRow(writer, row);
            }
        }
    }

    private void writeRow(BufferedWriter writer, List<?> values) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                line.append(',');
            }
            Object value = values.get(i);
            line.append(escapeCsv(value == null ? "" : value.toString()));
        }
        writer.write(line.toString());
        writer.newLine();
    }

    private String escapeCsv(String value) {
        boolean hasQuote = value.contains("\"");
        boolean needsQuote = hasQuote || value.contains(",") || value.contains("\n") || value.contains("\r");
        String sanitized = hasQuote ? value.replace("\"", "\"\"") : value;
        return needsQuote ? '"' + sanitized + '"' : sanitized;
    }
}
