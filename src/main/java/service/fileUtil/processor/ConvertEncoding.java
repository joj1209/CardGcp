package service.fileUtil.processor;

import service.fileUtil.reader.SqlReader;
import service.fileUtil.writer.SqlWriter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ConvertEncoding {

    private final SqlReader reader;
    private final SqlWriter writer;

    public ConvertEncoding(SqlReader reader, SqlWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

    public void process(Path inputPath, Path outputPath, Charset fromCharset, Charset toCharset) throws IOException {
        if (Files.isDirectory(inputPath)) {
            processDirectory(inputPath, outputPath, fromCharset, toCharset);
        } else if (Files.isRegularFile(inputPath)) {
            processFile(inputPath, outputPath, fromCharset, toCharset);
        } else {
            throw new IllegalArgumentException("Invalid path: " + inputPath);
        }
    }

    private void processDirectory(Path inputDir, Path outputDir, Charset fromCharset, Charset toCharset) throws IOException {
        System.out.println("Converting directory: " + inputDir.toAbsolutePath());
        System.out.println("Output directory: " + outputDir.toAbsolutePath());
        System.out.println("From: " + fromCharset.name() + " -> To: " + toCharset.name());

        try (Stream<Path> paths = Files.walk(inputDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .forEach(inputFile -> {
                        try {
                            // Step 1: Read
                            String content = reader.read(inputFile, fromCharset);

                            // Step 2: Process (현재는 그대로, 추후 변환 로직 추가 가능)
                            String processedContent = convert(content);

                            // Step 3: Write
                            writer.writeWithRelativePath(inputFile, inputDir, outputDir, processedContent, fromCharset, toCharset);
                        } catch (IOException e) {
                            System.err.println("Failed to convert file: " + inputFile + " - " + e.getMessage());
                        }
                    });
        }
    }

    private void processFile(Path inputFile, Path outputPath, Charset fromCharset, Charset toCharset) throws IOException {
        // Step 1: Read
        String content = reader.read(inputFile, fromCharset);

        // Step 2: Process (현재는 그대로, 추후 변환 로직 추가 가능)
        String processedContent = convert(content);

        // Step 3: Write
        Path outputFile = writer.resolveOutputFile(inputFile, outputPath);
        writer.writeWithLog(inputFile, outputFile, processedContent, fromCharset, toCharset);
    }

    private String convert(String content) {
        // 현재는 그대로 반환
        // 추후 여기에 변환 로직 추가 가능:
        // - SQL 문법 변환
        // - 주석 제거/변환
        // - 포맷팅
        // - 특정 문자열 치환 등
        return content;
    }
}




