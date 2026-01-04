package service.fileUtil.job;

import service.fileUtil.processor.ConvertEncoding;
import service.fileUtil.reader.SqlReader;
import service.fileUtil.writer.SqlWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class UtilJob {
    static SqlReader reader = new SqlReader();
    static SqlWriter writer = new SqlWriter();
    static ConvertEncoding converter = new ConvertEncoding();

    public static void main(String[] args) throws IOException {
        System.out.println("------- UtilJob started -------");

        if (args.length < 2) {
            System.err.println("Usage: java UtilJob <input_path> <output_path>");
            return;
        }

        Path inputPath = Paths.get(args[0]);
        Path outputPath = Paths.get(args[1]);

        System.out.println("Input Path: " + inputPath.toAbsolutePath());
        System.out.println("Output Path: " + outputPath.toAbsolutePath());

        // 사용자 선택 프롬프트
        System.out.println("\n========================================");
        System.out.println("Select encoding conversion option:");
        System.out.println("1. EUC-KR -> UTF-8");
        System.out.println("2. UTF-8 -> EUC-KR");
        System.out.println("3. No conversion (read only)");
        System.out.println("========================================");
        System.out.print("Enter your choice (1-3): ");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String choice = br.readLine().trim();

        switch (choice) {
            case "1":
                System.out.println("\n>>> Converting: EUC-KR -> UTF-8\n");
                processConversion(inputPath, outputPath, SqlReader.EUCKR, SqlReader.UTF8);
                break;
            case "2":
                System.out.println("\n>>> Converting: UTF-8 -> EUC-KR\n");
                processConversion(inputPath, outputPath, SqlReader.UTF8, SqlReader.EUCKR);
                break;
            case "3":
                System.out.println("\n>>> Read only mode (no conversion)\n");
                reader.run(inputPath);
                break;
            default:
                System.err.println("Invalid choice: " + choice);
                System.err.println("Please select 1, 2, or 3");
        }

        System.out.println("\n------- UtilJob finished -------");
    }

    private static void processConversion(Path inputPath, Path outputPath, Charset fromCharset, Charset toCharset) throws IOException {
        if (Files.isDirectory(inputPath)) {
            processDirectory(inputPath, outputPath, fromCharset, toCharset);
        } else if (Files.isRegularFile(inputPath)) {
            processFile(inputPath, outputPath, fromCharset, toCharset);
        } else {
            throw new IllegalArgumentException("Invalid path: " + inputPath);
        }
    }

    private static void processDirectory(Path inputDir, Path outputDir, Charset fromCharset, Charset toCharset) throws IOException {
        System.out.println("Converting directory: " + inputDir.toAbsolutePath());
        System.out.println("Output directory: " + outputDir.toAbsolutePath());
        System.out.println("From: " + fromCharset.name() + " -> To: " + toCharset.name());

        try (Stream<Path> paths = Files.walk(inputDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .forEach(inputFile -> {
                        try {
                            // Step 1: Convert (Processor)
                            String content = converter.convert(inputFile, fromCharset);

                            // Step 2: Write (Writer)
                            writer.writeWithRelativePath(inputFile, inputDir, outputDir, content, fromCharset, toCharset);
                        } catch (IOException e) {
                            System.err.println("Failed to convert file: " + inputFile + " - " + e.getMessage());
                        }
                    });
        }
    }

    private static void processFile(Path inputFile, Path outputPath, Charset fromCharset, Charset toCharset) throws IOException {
        // Step 1: Convert (Processor)
        String content = converter.convert(inputFile, fromCharset);

        // Step 2: Resolve output path (Writer)
        Path outputFile = writer.resolveOutputFile(inputFile, outputPath);

        // Step 3: Write with log (Writer)
        writer.writeWithLog(inputFile, outputFile, content, fromCharset, toCharset);
    }
}

