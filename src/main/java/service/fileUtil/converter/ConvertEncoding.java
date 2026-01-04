package service.fileUtil.converter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ConvertEncoding {

    public void convert(Path inputPath, Charset fromCharset, Charset toCharset) throws IOException {
        if (Files.isDirectory(inputPath)) {
            convertDirectory(inputPath, fromCharset, toCharset);
        } else if (Files.isRegularFile(inputPath)) {
            convertFile(inputPath, fromCharset, toCharset);
        } else {
            throw new IllegalArgumentException("Invalid path: " + inputPath);
        }
    }

    private void convertDirectory(Path directory, Charset fromCharset, Charset toCharset) throws IOException {
        System.out.println("Converting directory: " + directory.toAbsolutePath());
        System.out.println("From: " + fromCharset.name() + " -> To: " + toCharset.name());

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .forEach(path -> {
                        try {
                            convertFile(path, fromCharset, toCharset);
                        } catch (IOException e) {
                            System.err.println("Failed to convert file: " + path + " - " + e.getMessage());
                        }
                    });
        }
    }

    private void convertFile(Path file, Charset fromCharset, Charset toCharset) throws IOException {
        String fileName = file.getFileName().toString();

        // 원본 파일 읽기 (fromCharset으로)
        String content = Files.readString(file, fromCharset);

        // 변환된 내용을 같은 파일에 쓰기 (toCharset으로)
        Files.writeString(file, content, toCharset);

        System.out.println("✓ Converted: " + fileName +
                         " (" + fromCharset.name() + " -> " + toCharset.name() + ")");
    }
}

