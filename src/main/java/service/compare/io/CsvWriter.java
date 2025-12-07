package service.compare.io;

import service.compare.model.OutputRow;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/** 외부 라이브러리 없이 CSV 쓰기 (필드 인용/이스케이프) */
public class CsvWriter {

    public static void write(Path outPath, List<String> headers, List<OutputRow> rows) throws IOException {
        if (outPath.getParent() != null) {
            Files.createDirectories(outPath.getParent());
        }
        try (BufferedWriter bw = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

            writeRow(bw, headers);

            for (OutputRow r : rows) {
                // 값 + RESULT + DIFF_DETAIL
                List<String> fields = r.toOutputFields();
                writeRow(bw, fields);
            }
        }
    }

    public static void writeRow(BufferedWriter bw, List<String> fields) throws IOException {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) bw.write(',');
            writeField(bw, fields.get(i));
        }
        bw.write("\r\n"); // RFC 4180
    }

    private static void writeField(BufferedWriter bw, String s) throws IOException {
        if (s == null) s = "";
        boolean needsQuotes = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 ||
                s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (!needsQuotes) {
            bw.write(s);
            return;
        }
        bw.write('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"') bw.write("\"\"");
            else bw.write(ch);
        }
        bw.write('"');
    }
}

