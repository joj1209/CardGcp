package file.writer;

import file.vo.TablesInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * CSV 형식으로 테이블 정보를 저장하는 Writer 클래스입니다.
 * 확장 가능한 구조로 설계되어 새로운 컬럼 추가가 용이합니다.
 */
public class CsvWriter {
    private final Path outputPath;
    private final Charset charset;
    private final List<String> headers;
    private final List<CsvRecord> records;
    private boolean headerWritten;

    /**
     * CsvWriter 인스턴스를 생성합니다.
     *
     * @param outputPath CSV 파일이 저장될 경로 (파일 경로여야 함)
     * @param charset 파일 인코딩 (기본: UTF-8 권장)
     */
    public CsvWriter(Path outputPath, Charset charset) {
        this.outputPath = outputPath;
        this.charset = charset;
        this.headers = new ArrayList<>();
        this.records = new ArrayList<>();
        this.headerWritten = false;
        initializeDefaultHeaders();
    }

    /**
     * 기본 헤더를 초기화합니다.
     * 새로운 컬럼을 추가하려면 이 메서드를 수정하거나 addHeader() 메서드를 사용하세요.
     */
    private void initializeDefaultHeaders() {
        headers.add("File Name");
        headers.add("Source Tables");
        headers.add("Target Tables");
    }

    /**
     * 헤더를 추가합니다. (확장 기능)
     *
     * @param headerName 추가할 헤더명
     */
    public void addHeader(String headerName) {
        if (headerWritten) {
            throw new IllegalStateException("Headers already written. Cannot add more headers.");
        }
        headers.add(headerName);
    }

    /**
     * 레코드를 추가합니다.
     *
     * @param fileName 파일명
     * @param tablesInfo 테이블 정보
     */
    public void addRecord(String fileName, TablesInfo tablesInfo) {
        CsvRecord record = new CsvRecord();
        record.put("File Name", fileName);
        record.put("Source Tables", joinTables(tablesInfo.getSortedSources()));
        record.put("Target Tables", joinTables(tablesInfo.getSortedTargets()));
        records.add(record);
    }

    /**
     * 레코드를 추가합니다. (확장 기능 - 추가 필드 포함)
     *
     * @param fileName 파일명
     * @param tablesInfo 테이블 정보
     * @param additionalFields 추가 필드 (key: 헤더명, value: 값)
     */
    public void addRecord(String fileName, TablesInfo tablesInfo, Map<String, String> additionalFields) {
        CsvRecord record = new CsvRecord();
        record.put("File Name", fileName);
        record.put("Source Tables", joinTables(tablesInfo.getSortedSources()));
        record.put("Target Tables", joinTables(tablesInfo.getSortedTargets()));

        // 추가 필드 설정
        if (additionalFields != null) {
            for (Map.Entry<String, String> entry : additionalFields.entrySet()) {
                record.put(entry.getKey(), entry.getValue());
            }
        }
        records.add(record);
    }

    /**
     * 테이블 Set을 세미콜론(;)으로 구분된 문자열로 변환합니다.
     *
     * @param tables 테이블 Set
     * @return 세미콜론으로 구분된 문자열
     */
    private String joinTables(Set<String> tables) {
        if (tables == null || tables.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = tables.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append("; ");
            }
        }
        return sb.toString();
    }

    /**
     * 모든 레코드를 CSV 파일로 저장합니다.
     * UTF-8 BOM을 추가하여 엑셀에서 한글이 정상적으로 표시되도록 합니다.
     *
     * @throws IOException 파일 쓰기 중 오류 발생 시
     */
    public void write() throws IOException {
        // 출력 디렉토리가 없으면 생성
        Path parent = outputPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, charset,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            // UTF-8 BOM 추가 (엑셀에서 한글 깨짐 방지)
            if (charset.name().equalsIgnoreCase("UTF-8")) {
                writer.write('\ufeff');
            }

            // 헤더 작성
            writeHeaders(writer);
            headerWritten = true;

            // 레코드 작성
            for (CsvRecord record : records) {
                writeRecord(writer, record);
            }

            writer.flush();
        }
    }

    /**
     * CSV 헤더를 작성합니다.
     *
     * @param writer BufferedWriter
     * @throws IOException 쓰기 오류 시
     */
    private void writeHeaders(BufferedWriter writer) throws IOException {
        writer.write(String.join(",", headers));
        writer.newLine();
    }

    /**
     * 단일 레코드를 CSV 형식으로 작성합니다.
     *
     * @param writer BufferedWriter
     * @param record CsvRecord
     * @throws IOException 쓰기 오류 시
     */
    private void writeRecord(BufferedWriter writer, CsvRecord record) throws IOException {
        List<String> values = new ArrayList<>();
        for (String header : headers) {
            String value = record.get(header);
            values.add(escapeCsv(value != null ? value : ""));
        }
        writer.write(String.join(",", values));
        writer.newLine();
    }

    /**
     * CSV 필드를 이스케이프 처리합니다.
     * 콤마, 따옴표, 줄바꿈이 포함된 경우 큰따옴표로 감싸고 내부 따옴표는 이중으로 처리합니다.
     *
     * @param value 원본 값
     * @return 이스케이프된 값
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // 콤마, 큰따옴표, 줄바꿈이 포함된 경우 큰따옴표로 감싸기
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            // 내부 큰따옴표는 두 개로 변환
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }

    /**
     * 레코드 수를 반환합니다.
     *
     * @return 현재 저장된 레코드 수
     */
    public int getRecordCount() {
        return records.size();
    }

    /**
     * CSV 레코드를 표현하는 내부 클래스입니다.
     * 각 레코드는 헤더명을 키로 하는 맵 구조를 가집니다.
     */
    private static class CsvRecord {
        private final Map<String, String> fields = new LinkedHashMap<>();

        public void put(String key, String value) {
            fields.put(key, value);
        }

        public String get(String key) {
            return fields.get(key);
        }
    }
}

