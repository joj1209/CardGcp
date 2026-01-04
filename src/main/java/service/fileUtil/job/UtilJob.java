package service.fileUtil.job;

import service.fileUtil.common.FileTraverser;
import service.fileUtil.processor.ConvertStep;
import service.fileUtil.reader.SqlReader;
import service.fileUtil.writer.SqlWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

public class UtilJob {
    static SqlReader reader = new SqlReader();
    static SqlWriter writer = new SqlWriter();
    static ConvertStep processor = new ConvertStep();

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
        System.out.println("Select processing option:");
        System.out.println("0. No conversion (read only)");
        System.out.println("1. EUC-KR -> UTF-8");
        System.out.println("2. UTF-8 -> EUC-KR");
        System.out.println("3. Remove trailing spaces (UTF-8 -> UTF-8)");
        System.out.println("========================================");
        System.out.print("Enter your choice (0-3): ");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String choice = br.readLine().trim();

        switch (choice) {
            case "0":
                System.out.println("\n>>> Read only mode (no conversion)\n");
                reader.run(inputPath);
                break;
            case "1":
                System.out.println("\n>>> Converting: EUC-KR -> UTF-8\n");
                processConversion(inputPath, outputPath, SqlReader.EUCKR, SqlReader.UTF8, processor::process);
                break;
            case "2":
                System.out.println("\n>>> Converting: UTF-8 -> EUC-KR\n");
                processConversion(inputPath, outputPath, SqlReader.UTF8, SqlReader.EUCKR, processor::process);
                break;
            case "3":
                System.out.println("\n>>> Removing trailing spaces (UTF-8 -> UTF-8)\n");
                processConversion(inputPath, outputPath, SqlReader.UTF8, SqlReader.UTF8, processor::removeTrailingSpaces);
                break;
            default:
                System.err.println("Invalid choice: " + choice);
                System.err.println("Please select 0, 1, 2, or 3");
        }

        System.out.println("\n------- UtilJob finished -------");
    }

    private static void processConversion(Path inputPath, Path outputPath, Charset fromCharset, Charset toCharset, Function<String, String> transformer) throws IOException {
        FileTraverser.traverse(inputPath, inputFile -> {
            // Step 1: Read
            String content = reader.read(inputFile, fromCharset);

            // Step 2: Process
            String processedContent = transformer.apply(content);

            // Step 3: Write
            // inputPath가 디렉토리인 경우와 파일인 경우를 구분하여 상대 경로 계산
            if (inputPath.toFile().isDirectory()) {
                writer.writeWithRelativePath(inputFile, inputPath, outputPath, processedContent, fromCharset, toCharset);
            } else {
                Path outputFile = writer.resolveOutputFile(inputFile, outputPath);
                writer.writeWithLog(inputFile, outputFile, processedContent, fromCharset, toCharset);
            }
        });
    }
}

