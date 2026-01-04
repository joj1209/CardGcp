package service.fileUtil.converter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ConvertEncoding {

    public void convert(Path inputPath, Path outputPath, Charset fromCharset, Charset toCharset) throws IOException {
        // outputPath 디렉토리 생성
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        if (Files.isDirectory(inputPath)) {
            convertDirectory(inputPath, outputPath, fromCharset, toCharset);
        } else if (Files.isRegularFile(inputPath)) {
            convertFile(inputPath, outputPath, fromCharset, toCharset);
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
                            // 상대 경로 계산
                            Path relativePath = inputDir.relativize(inputFile);
                            Path outputFile = outputDir.resolve(relativePath);

                            // 출력 파일의 부모 디렉토리 생성
                            Files.createDirectories(outputFile.getParent());

                            convertFile(inputFile, outputFile, fromCharset, toCharset);
                        } catch (IOException e) {
                            System.err.println("Failed to convert file: " + inputFile + " - " + e.getMessage());
                        }
                    });
        }
    }

    private void convertFile(Path inputFile, Path outputFile, Charset fromCharset, Charset toCharset) throws IOException {
        String fileName = inputFile.getFileName().toString();

        // 원본 파일 읽기 (fromCharset으로)
        String content = Files.readString(inputFile, fromCharset);

        // 변환된 내용을 출력 파일에 쓰기 (toCharset으로)
        Files.writeString(outputFile, content, toCharset);

        System.out.println("✓ Converted: " + fileName +
                         " (" + fromCharset.name() + " -> " + toCharset.name() + ")" +
                         " -> " + outputFile.toAbsolutePath());
    }
}



