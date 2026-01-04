package service.fileUtil.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FileTraverser {
    public static void traverse(Path inputPath, FileProcessor processor) throws IOException {
        if (Files.isDirectory(inputPath)) {
            System.out.println("Processing directory: " + inputPath.toAbsolutePath());
            try (Stream<Path> paths = Files.walk(inputPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".sql"))
                        .forEach(path -> {
                            try {
                                processor.process(path);
                            } catch (IOException e) {
                                System.err.println("Failed to process file: " + path + " - " + e.getMessage());
                            }
                        });
            }
        } else if (Files.isRegularFile(inputPath)) {
            processor.process(inputPath);
        } else {
            throw new IllegalArgumentException("Invalid path: " + inputPath);
        }
    }
}

