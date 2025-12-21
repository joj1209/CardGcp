package service.csvCompare.io;

import service.csvCompare.model.CsvTable;
import service.csvCompare.model.DataRow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** 외부 라이브러리 없이 RFC4180 스타일 CSV 읽기 */
public class CsvReader {

    public static CsvTable read(Path path) throws IOException {
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        List<List<String>> records = parseCsv(text);

        if (records.isEmpty()) {
            throw new IllegalStateException("헤더가 없습니다: " + path);
        }

        List<String> headers = records.get(0);
        List<DataRow> rows = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            List<String> rec = records.get(i);
            Map<String, String> values = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                String col = headers.get(c);
                String val = c < rec.size() ? rec.get(c) : "";
                values.put(col, val);
            }
            rows.add(new DataRow(values));
        }
        return new CsvTable(headers, rows);
    }

    /** 간이 CSV 파서 (따옴표/콤마/개행/이중따옴표 처리, CRLF 지원) */
    static List<List<String>> parseCsv(String s) {
        List<List<String>> out = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();

        int i = 0, n = s.length();
        boolean inQuotes = false;
        boolean fieldStarted = false;

        while (i < n) {
            char ch = s.charAt(i);

            if (inQuotes) {
                if (ch == '"') {
                    // 다음이 "이면 이스케이프, 아니면 인용 종료
                    if (i + 1 < n && s.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                        continue;
                    } else {
                        inQuotes = false;
                        i++;
                        continue;
                    }
                } else {
                    field.append(ch);
                    i++;
                    continue;
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                    fieldStarted = true;
                    i++;
                    continue;
                } else if (ch == ',') {
                    record.add(field.toString());
                    field.setLength(0);
                    fieldStarted = false;
                    i++;
                    continue;
                } else if (ch == '\r') {
                    // CRLF 처리
                    if (i + 1 < n && s.charAt(i + 1) == '\n') i++;
                    record.add(field.toString());
                    out.add(record);
                    record = new ArrayList<>();
                    field.setLength(0);
                    fieldStarted = false;
                    i++;
                    continue;
                } else if (ch == '\n') {
                    record.add(field.toString());
                    out.add(record);
                    record = new ArrayList<>();
                    field.setLength(0);
                    fieldStarted = false;
                    i++;
                    continue;
                } else {
                    field.append(ch);
                    fieldStarted = true;
                    i++;
                    continue;
                }
            }
        }

        // 마지막 필드/레코드 플러시
        if (field.length() > 0 || fieldStarted || !record.isEmpty()) {
            record.add(field.toString());
            out.add(record);
        }
        return out;
    }
}

