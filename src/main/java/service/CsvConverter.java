package service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CsvConverter {

    private static final String INPUT_CSV = "csv/CmJob.csv";
    private static final String OUTPUT_CSV = "output_result.csv";

    public static void main(String[] args) {
        CsvConverter converter = new CsvConverter();
        converter.convert();
    }

    public void convert() {
        try {
            List<String[]> rawData = readCsv(INPUT_CSV);
            List<Map<String, String>> pivotedData = pivotData(rawData);
            writeCsv(OUTPUT_CSV, pivotedData);

            System.out.println("CSV 변환 완료: " + OUTPUT_CSV);
            System.out.println("총 " + pivotedData.size() + "개의 행이 생성되었습니다.");

        } catch (Exception e) {
            System.err.println("CSV 변환 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<String[]> readCsv(String filePath) throws IOException {
        List<String[]> data = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // 헤더 스킵
                }

                // CSV 파싱 (간단한 split 사용, 따옴표 안의 쉼표는 고려하지 않음)
                String[] columns = parseCsvLine(line);
                if (columns.length >= 5) {
                    data.add(columns);
                }
            }
        }

        return data;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }

    private List<Map<String, String>> pivotData(List<String[]> rawData) {
        Map<String, Map<String, String>> pivotMap = new LinkedHashMap<>();

        for (String[] row : rawData) {
            if (row.length < 5) continue;

            String application = row[0].trim();
            String group = row[1].trim();
            String jobName = row[2].trim();
            String ctrmKey = row[3].trim();
            String ctrmData = row[4].trim();

            // 복합 키 생성 (APPLICATION_NM5 + GROUP_NM4 + JOBNAME_NM3)
            String compositeKey = application + "|" + group + "|" + jobName;

            pivotMap.putIfAbsent(compositeKey, new LinkedHashMap<>());
            Map<String, String> rowMap = pivotMap.get(compositeKey);

            // 기본 컬럼 저장
            rowMap.put("APPLICATION_NM5", application);
            rowMap.put("GROUP_NM4", group);
            rowMap.put("JOBNAME_NM3", jobName);

            // CTRM_항목명을 컬럼으로, CTRM_DATA를 값으로 저장
            rowMap.put(ctrmKey, ctrmData);
        }

        return new ArrayList<>(pivotMap.values());
    }

    private void writeCsv(String filePath, List<Map<String, String>> data) throws IOException {
        if (data.isEmpty()) {
            System.out.println("출력할 데이터가 없습니다.");
            return;
        }

        // 10개 고정 컬럼 정의 (기본 3개 + CTRM_항목명 7개)
        List<String> allColumns = new ArrayList<>();
        allColumns.add("APPLICATION_NM5");
        allColumns.add("GROUP_NM4");
        allColumns.add("JOBNAME_NM3");
        allColumns.add("CMDLINE");
        allColumns.add("DAYSCAL");
        allColumns.add("DESCRIPTION");
        allColumns.add("INCOND");
        allColumns.add("MONTH");
        allColumns.add("TIMEFROM");
        allColumns.add("TIMEUNTIL");

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            // BOM 추가 (Excel 호환성)
            bw.write('\ufeff');

            // 헤더 작성
            bw.write(String.join(",", allColumns));
            bw.newLine();

            // 데이터 작성
            for (Map<String, String> row : data) {
                List<String> values = new ArrayList<>();
                for (String column : allColumns) {
                    String value = row.getOrDefault(column, "");
                    // 쉼표나 따옴표가 포함된 경우 따옴표로 감싸기
                    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                        value = "\"" + value.replace("\"", "\"\"") + "\"";
                    }
                    values.add(value);
                }
                bw.write(String.join(",", values));
                bw.newLine();
            }
        }

        System.out.println("출력 파일: " + new File(filePath).getAbsolutePath());
        System.out.println("총 컬럼 수: " + allColumns.size());
    }
}

