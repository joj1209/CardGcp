package service.queryParser.writer;

import service.queryParser.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 지정된 출력 디렉토리로 텍스트 파일을 기록하는 유틸리티입니다.
 */
public class TextWriter {
    private final Path outputDir;
    private final Charset charset;

    public TextWriter(Path outputDir) {
        this(outputDir, StandardCharsets.UTF_8);
    }

    public TextWriter(Path outputDir, Charset charset) {
        this.outputDir = outputDir;
        this.charset = charset;
    }

    public Path write(String relativeFile, String content) throws IOException {
        Path target = outputDir.resolve(relativeFile);
        Files.createDirectories(target.getParent());
        return Files.write(target, content.getBytes(charset));
    }

    /**
     * 소스/타겟 테이블 정보를 텍스트로 정리해 파일로 기록합니다.
     */
    public Path writeTables(String relativeFile, TablesInfo info) throws IOException {
        return write(relativeFile, formatTables(info));
    }


    private String formatTables(TablesInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Source Tables]\n");
        info.getSortedSources().forEach(t -> sb.append(t).append('\n'));
        sb.append("\n[Target Tables]\n");
        info.getSortedTargets().forEach(t -> sb.append(t).append('\n'));
        return sb.toString();
    }
}
