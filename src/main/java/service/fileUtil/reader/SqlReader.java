package service.fileUtil.reader;

import service.fileUtil.common.FileTraverser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class SqlReader {
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final Charset EUCKR = Charset.forName("EUC-KR");
    public static final Charset DEFAULT_CHARSET = UTF8;

    public String read(Path inputFile, Charset charset) throws IOException {
        return Files.readString(inputFile, charset);
    }

    public void run(Path inputPath) throws IOException {
        FileTraverser.traverse(inputPath, this::processFile);
    }

    private void processFile(Path file) throws IOException {
        String filePath = file.toAbsolutePath().toString();
        String fileName = file.getFileName().toString();
        String content = Files.readString(file, DEFAULT_CHARSET);

        System.out.println("\n========================================");
        System.out.println("File: " + filePath);
        System.out.println("Name: " + fileName);
        System.out.println("Content length: " + content.length() + " characters");
        System.out.println("========================================");
    }
}
