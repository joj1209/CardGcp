package service.fileUtil.processor;

import service.fileUtil.writer.SqlWriter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ConvertEncoding {

    private final SqlWriter writer;

    public ConvertEncoding(SqlWriter writer) {
        this.writer = writer;
    }

    public void processConversion(Path inputPath, Path outputPath, Charset fromCharset, Charset toCharset) throws IOException {
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
                            String content = convert(inputFile, fromCharset);
                            writer.writeWithRelativePath(inputFile, inputDir, outputDir, content, fromCharset, toCharset);
                        } catch (IOException e) {
                            System.err.println("Failed to convert file: " + inputFile + " - " + e.getMessage());
                        }
                    });
        }
    }

    private void processFile(Path inputFile, Path outputPath, Charset fromCharset, Charset toCharset) throws IOException {
        String content = convert(inputFile, fromCharset);
        Path outputFile = writer.resolveOutputFile(inputFile, outputPath);
        writer.writeWithLog(inputFile, outputFile, content, fromCharset, toCharset);
    }

    public String convert(Path inputFile, Charset fromCharset) throws IOException {
        return Files.readString(inputFile, fromCharset);
    }
}




