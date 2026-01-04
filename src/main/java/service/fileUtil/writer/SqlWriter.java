package service.fileUtil.writer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class SqlWriter {

    public void write(Path outputFile, String content, Charset charset) throws IOException {
        // 출력 파일의 부모 디렉토리 생성
        if (outputFile.getParent() != null && !Files.exists(outputFile.getParent())) {
            Files.createDirectories(outputFile.getParent());
        }

        // 파일에 내용 쓰기
        Files.writeString(outputFile, content, charset);
    }

    public void writeWithLog(Path inputFile, Path outputFile, String content, Charset fromCharset, Charset toCharset) throws IOException {
        // 파일 쓰기
        write(outputFile, content, toCharset);

        // 로그 출력
        System.out.println("✓ Converted: " + inputFile.getFileName() +
                         " (" + fromCharset.name() + " -> " + toCharset.name() + ")" +
                         " -> " + outputFile.toAbsolutePath());
    }

    public void writeWithRelativePath(Path inputFile, Path inputDir, Path outputDir, String content, Charset fromCharset, Charset toCharset) throws IOException {
        // 출력 파일 경로 계산
        Path outputFile = resolveOutputFileWithRelativePath(inputFile, inputDir, outputDir);

        // 파일 쓰기 + 로그
        writeWithLog(inputFile, outputFile, content, fromCharset, toCharset);
    }

    public void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    public Path resolveOutputFile(Path inputFile, Path outputPath) {
        if (Files.isDirectory(outputPath)) {
            return outputPath.resolve(inputFile.getFileName());
        } else {
            return outputPath;
        }
    }

    public Path resolveOutputFileWithRelativePath(Path inputFile, Path inputDir, Path outputDir) {
        Path relativePath = inputDir.relativize(inputFile);
        return outputDir.resolve(relativePath);
    }
}
