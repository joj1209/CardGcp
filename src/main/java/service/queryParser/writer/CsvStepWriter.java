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
 * STEP별 테이블 정보를 파일명을 PK로 하여 CSV로 저장하는 Writer 클래스입니다.
 * 파일별로 모든 STEP 정보를 한 행에 집계하여 저장합니다.
 */
public class CsvStepWriter {
    private final Path outputPath;
    private final Charset charset;
    private final Map<String, FileStepRecord> fileRecords;

    /**
     * CsvStepWriter 인스턴스를 생성합니다.
     *
     * @param outputPath CSV 파일이 저장될 경로 (파일 경로여야 함)
     * @param charset 파일 인코딩 (기본: UTF-8 권장)
     */
    public CsvStepWriter(Path outputPath, Charset charset) {
        this.outputPath = outputPath;
        this.charset = charset;
        this.fileRecords = new LinkedHashMap<>();
    }

    /**
     * 파일별 STEP 정보를 추가합니다.
     * 같은 파일명으로 여러 번 호출되면 STEP 정보가 누적됩니다.
     *
     * @param fileName 파일명 (PK 역할)
     * @param stepName STEP 이름 (예: STEP001, STEP002)
     * @param tablesInfo 해당 STEP의 테이블 정보
     */
    public void addStepRecord(String fileName, String stepName, TablesInfo tablesInfo) {
        FileStepRecord record = fileRecords.computeIfAbsent(fileName, k -> new FileStepRecord(fileName));
        record.addStep(stepName, tablesInfo);
    }

    /**
     * 파일의 모든 STEP 정보를 한 번에 추가합니다.
     *
     * @param fileName 파일명 (PK 역할)
     * @param stepTables STEP별 테이블 정보 맵
     */
    public void addFileSteps(String fileName, Map<String, TablesInfo> stepTables) {
        FileStepRecord record = fileRecords.computeIfAbsent(fileName, k -> new FileStepRecord(fileName));
        for (Map.Entry<String, TablesInfo> entry : stepTables.entrySet()) {
            record.addStep(entry.getKey(), entry.getValue());
        }
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
            writeHeader(writer);

            // 레코드 작성
            for (FileStepRecord record : fileRecords.values()) {
                writeRecord(writer, record);
            }

            writer.flush();
        }
    }

    /**
     * CSV 헤더를 작성합니다.
     */
    private void writeHeader(BufferedWriter writer) throws IOException {
        writer.write("File Name,Source Tables,Target Tables,Steps,Total Steps");
        writer.newLine();
    }

    /**
     * 하나의 레코드(파일)를 CSV 행으로 작성합니다.
     */
    private void writeRecord(BufferedWriter writer, FileStepRecord record) throws IOException {
        writer.write(escapeCsv(record.fileName));
        writer.write(',');
        writer.write(escapeCsv(joinTables(record.getAllSources())));
        writer.write(',');
        writer.write(escapeCsv(joinTables(record.getAllTargets())));
        writer.write(',');
        writer.write(escapeCsv(record.getStepDetails()));
        writer.write(',');
        writer.write(String.valueOf(record.getTotalSteps()));
        writer.newLine();
    }

    /**
     * CSV 필드를 이스케이프 처리합니다.
     * 쉼표, 따옴표, 줄바꿈이 포함된 경우 따옴표로 감싸고 내부 따옴표는 두 번 반복합니다.
     */
    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 테이블 Set을 세미콜론(;)으로 구분된 문자열로 변환합니다.
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
     * 현재 저장된 파일 레코드 수를 반환합니다.
     */
    public int getRecordCount() {
        return fileRecords.size();
    }

    /**
     * 파일별 STEP 정보를 저장하는 내부 클래스
     */
    private static class FileStepRecord {
        private final String fileName;
        private final Set<String> allSources;
        private final Set<String> allTargets;
        private final List<StepInfo> steps;

        public FileStepRecord(String fileName) {
            this.fileName = fileName;
            this.allSources = new TreeSet<>();
            this.allTargets = new TreeSet<>();
            this.steps = new ArrayList<>();
        }

        public void addStep(String stepName, TablesInfo tablesInfo) {
            allSources.addAll(tablesInfo.getSources());
            allTargets.addAll(tablesInfo.getTargets());
            steps.add(new StepInfo(stepName, tablesInfo));
        }

        public Set<String> getAllSources() {
            return allSources;
        }

        public Set<String> getAllTargets() {
            return allTargets;
        }

        public int getTotalSteps() {
            return steps.size();
        }

        public String getStepDetails() {
            if (steps.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < steps.size(); i++) {
                if (i > 0) {
                    sb.append("\n\n");
                }
                sb.append(steps.get(i).toDetailString());
            }
            return sb.toString();
        }
    }

    /**
     * STEP 정보를 저장하는 내부 클래스
     */
    private static class StepInfo {
        private final String stepName;
        private final TablesInfo tablesInfo;

        public StepInfo(String stepName, TablesInfo tablesInfo) {
            this.stepName = stepName;
            this.tablesInfo = tablesInfo;
        }

        /**
         * STEP의 상세 정보를 포맷팅하여 반환합니다.
         */
        public String toDetailString() {
            StringBuilder sb = new StringBuilder();
            sb.append("============================================================\n");
            sb.append(stepName).append("\n");
            sb.append("============================================================\n");

            // Source Tables
            sb.append("[Source Tables]\n");
            Set<String> sources = tablesInfo.getSortedSources();
            if (sources.isEmpty()) {
                sb.append("(No source tables)\n");
            } else {
                for (String source : sources) {
                    sb.append(source).append("\n");
                }
            }

            sb.append("\n");

            // Target Tables
            sb.append("[Target Tables]\n");
            Set<String> targets = tablesInfo.getSortedTargets();
            if (targets.isEmpty()) {
                sb.append("(No target tables)\n");
            } else {
                for (String target : targets) {
                    sb.append(target).append("\n");
                }
            }

            return sb.toString();
        }

        @Override
        public String toString() {
            return stepName + "(S:" + tablesInfo.getSources().size() + ",T:" + tablesInfo.getTargets().size() + ")";
        }
    }
}

