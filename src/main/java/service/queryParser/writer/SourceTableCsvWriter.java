package service.queryParser.writer;

import service.queryParser.vo.TablesInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * 소스테이블 중심으로 프로그램 매핑 정보를 CSV 형식으로 저장하는 Writer 클래스입니다.
 * 각 소스테이블이 어떤 프로그램(파일)에서 사용되는지를 매핑합니다.
 */
public class SourceTableCsvWriter {
    private final Path outputPath;
    private final Charset charset;
    private final Map<String, TableMapping> tableMappings;

    /**
     * SourceTableCsvWriter 인스턴스를 생성합니다.
     *
     * @param outputPath CSV 파일이 저장될 경로 (파일 경로여야 함)
     * @param charset 파일 인코딩 (기본: UTF-8 권장)
     */
    public SourceTableCsvWriter(Path outputPath, Charset charset) {
        this.outputPath = outputPath;
        this.charset = charset;
        this.tableMappings = new TreeMap<>(); // 정렬을 위해 TreeMap 사용
    }

    /**
     * 프로그램(파일)의 테이블 정보를 추가합니다.
     * 소스테이블별로 프로그램을 매핑합니다.
     *
     * @param fileName 파일명 (프로그램명)
     * @param tablesInfo 테이블 정보
     */
    public void addRecord(String fileName, TablesInfo tablesInfo) {
        Set<String> sourceTables = tablesInfo.getSortedSources();
        Set<String> targetTables = tablesInfo.getSortedTargets();

        // 각 소스테이블에 대해 프로그램과 타겟테이블 매핑
        for (String sourceTable : sourceTables) {
            tableMappings.putIfAbsent(sourceTable, new TableMapping(sourceTable));
            TableMapping mapping = tableMappings.get(sourceTable);
            mapping.addProgram(fileName, targetTables);
        }
    }

    /**
     * 모든 매핑 정보를 CSV 파일로 저장합니다.
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
            writer.write("Source Table,Program,Target Tables");
            writer.newLine();

            // 각 소스테이블에 대한 매핑 정보 작성
            for (TableMapping mapping : tableMappings.values()) {
                writeMapping(writer, mapping);
            }

            writer.flush();
        }
    }

    /**
     * 단일 소스테이블의 매핑 정보를 CSV 형식으로 작성합니다.
     *
     * @param writer BufferedWriter
     * @param mapping TableMapping
     * @throws IOException 쓰기 오류 시
     */
    private void writeMapping(BufferedWriter writer, TableMapping mapping) throws IOException {
        String sourceTable = mapping.getSourceTable();

        // 프로그램별로 행 생성
        for (Map.Entry<String, Set<String>> entry : mapping.getProgramMappings().entrySet()) {
            String program = entry.getKey();
            Set<String> targetTables = entry.getValue();

            String targetTablesStr = joinTables(targetTables);

            // CSV 행 작성: 소스테이블, 프로그램, 타겟테이블들
            writer.write(escapeCsv(sourceTable) + "," +
                        escapeCsv(program) + "," +
                        escapeCsv(targetTablesStr));
            writer.newLine();
        }
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
     * 매핑된 소스테이블 수를 반환합니다.
     *
     * @return 소스테이블 수
     */
    public int getTableCount() {
        return tableMappings.size();
    }

    /**
     * 소스테이블의 매핑 정보를 표현하는 내부 클래스입니다.
     * 하나의 소스테이블이 여러 프로그램에서 사용될 수 있으며,
     * 각 프로그램별로 타겟테이블 목록을 관리합니다.
     */
    private static class TableMapping {
        private final String sourceTable;
        private final Map<String, Set<String>> programMappings; // 프로그램 -> 타겟테이블들

        public TableMapping(String sourceTable) {
            this.sourceTable = sourceTable;
            this.programMappings = new TreeMap<>(); // 정렬을 위해 TreeMap 사용
        }

        public void addProgram(String program, Set<String> targetTables) {
            programMappings.putIfAbsent(program, new TreeSet<>()); // 정렬을 위해 TreeSet 사용
            if (targetTables != null) {
                programMappings.get(program).addAll(targetTables);
            }
        }

        public String getSourceTable() {
            return sourceTable;
        }

        public Map<String, Set<String>> getProgramMappings() {
            return programMappings;
        }
    }
}

