package service.fileUtil.processor;

import service.fileUtil.writer.SqlWriter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ConvertEncoding {

    private final SqlWriter writer;

    public ConvertEncoding() {
        this.writer = new SqlWriter();
    }

    public void convert(Path inputPath, Path outputPath, Charset fromCharset, Charset toCharset) throws IOException {
        // outputPath 디렉토리 생성
        writer.ensureDirectoryExists(outputPath);

        if (Files.isDirectory(inputPath)) {
            convertDirectory(inputPath, outputPath, fromCharset, toCharset);
        } else if (Files.isRegularFile(inputPath)) {
            String content = readAndConvert(inputPath, fromCharset);
            Path outputFile = resolveOutputFile(inputPath, outputPath);
            writer.write(outputFile, content, toCharset);
            System.out.println("✓ Converted: " + inputPath.getFileName() +
                             " (" + fromCharset.name() + " -> " + toCharset.name() + ")" +
                             " -> " + outputFile.toAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid path: " + inputPath);
        }
    }

    private void convertDirectory(Path inputDir, Path outputDir, Charset fromCharset, Charset toCharset) throws IOException {
        System.out.println("Converting directory: " + inputDir.toAbsolutePath());
        System.out.println("Output directory: " + outputDir.toAbsolutePath());
        System.out.println("From: " + fromCharset.name() + " -> To: " + toCharset.name());

        try (Stream<Path> paths = Files.walk(inputDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .forEach(inputFile -> {
                        try {
                            // 인코딩 변환
                            String content = readAndConvert(inputFile, fromCharset);

                            // 출력 파일 경로 계산
                            Path relativePath = inputDir.relativize(inputFile);
                            Path outputFile = outputDir.resolve(relativePath);

                            // 파일 쓰기
                            writer.write(outputFile, content, toCharset);

                            System.out.println("✓ Converted: " + inputFile.getFileName() +
                                             " (" + fromCharset.name() + " -> " + toCharset.name() + ")" +
                                             " -> " + outputFile.toAbsolutePath());
                        } catch (IOException e) {
                            System.err.println("Failed to convert file: " + inputFile + " - " + e.getMessage());
                        }
                    });
        }
    }

    private String readAndConvert(Path inputFile, Charset fromCharset) throws IOException {
        // 원본 파일 읽기 (fromCharset으로)
        return Files.readString(inputFile, fromCharset);
    }

    private Path resolveOutputFile(Path inputFile, Path outputPath) {
        if (Files.isDirectory(outputPath)) {
            return outputPath.resolve(inputFile.getFileName());
        } else {
            return outputPath;
        }
    }
}


