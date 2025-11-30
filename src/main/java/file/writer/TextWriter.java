package file.writer;

import file.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 지정된 출력 디렉토리로 텍스트 파일을 기록하는 유틸리티입니다.
 */
public class TextWriter {
    public static final Path DEFAULT_OUTPUT_DIR = Paths.get("D:", "11. Project", "11. DB", "BigQuery_out");

    private final Path outputDir;
    private final Charset charset;

    public TextWriter() {
        this(DEFAULT_OUTPUT_DIR, StandardCharsets.UTF_8);
    }

    public TextWriter(Path outputDir, Charset charset) {
        this.outputDir = outputDir;
        this.charset = charset;
    }

    public Path write(String relativeFile, String content) throws IOException {
        Path target = outputDir.resolve(relativeFile);
        System.out.println("[Writer] 출력 디렉토리 생성: " + target.getParent());
        Files.createDirectories(target.getParent());
        System.out.println("[Writer] 파일 쓰기 시작: " + target.getFileName());
        Path result = Files.write(target, content.getBytes(charset));
        System.out.println("[Writer] 파일 쓰기 완료: " + target);
        return result;
    }

    /**
     * 입력 루트 대비 상대 경로를 유지하면서 테이블 정보를 파일로 기록합니다.
     */
    public Path writeTables(Path inputDir, Path file, TablesInfo info) throws IOException {
        System.out.println("[Writer] 테이블 정보 처리: " + file.getFileName());
        String relativeName = buildOutputName(inputDir, file);
        System.out.println("[Writer] 출력 파일명 생성: " + relativeName);
        System.out.println("[Writer] 소스 테이블 수: " + info.getSources().size() +
                          ", 타겟 테이블 수: " + info.getTargets().size());
        return writeTables(relativeName, info);
    }

    /**
     * 소스/타겟 테이블 정보를 텍스트로 정리해 파일로 기록합니다.
     */
    public Path writeTables(String relativeFile, TablesInfo info) throws IOException {
        return write(relativeFile, formatTables(info));
    }

    public Path writeTables(String relativeFile, service.scan.model.TablesInfo info) throws IOException {
        System.out.println("[Writer] service.scan.model.TablesInfo를 file.vo.TablesInfo로 변환 중...");
        TablesInfo converted = new TablesInfo();
        info.getSources().forEach(converted::addSource);
        info.getTargets().forEach(converted::addTarget);
        System.out.println("[Writer] 변환 완료 - 소스: " + converted.getSources().size() +
                          ", 타겟: " + converted.getTargets().size());
        return writeTables(relativeFile, converted);
    }

    private String formatTables(TablesInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Source Tables]\n");
        info.getSources().forEach(t -> sb.append(t).append('\n'));
        sb.append("\n[Target Tables]\n");
        info.getTargets().forEach(t -> sb.append(t).append('\n'));
        return sb.toString();
    }

    private String buildOutputName(Path inputDir, Path file) {
        Path relative = inputDir.relativize(file);
        return relative.toString().replace('.', '_') + "_tables.txt";
    }
}
