package service.fileUtil.job;

import service.fileUtil.converter.ConvertEncoding;
import service.fileUtil.reader.SqlReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UtilJob {
    static SqlReader reader = new SqlReader();
    static ConvertEncoding converter = new ConvertEncoding();

    public static void main(String[] args) throws IOException {
        System.out.println("------- UtilJob started -------");

        if (args.length == 0) {
            System.err.println("Usage: java UtilJob <file_or_directory_path>");
            return;
        }

        Path path = Paths.get(args[0]);

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
                converter.convert(path, SqlReader.EUCKR, SqlReader.UTF8);
                break;
            case "2":
                System.out.println("\n>>> Converting: UTF-8 -> EUC-KR\n");
                converter.convert(path, SqlReader.UTF8, SqlReader.EUCKR);
                break;
            case "3":
                System.out.println("\n>>> Read only mode (no conversion)\n");
                reader.run(path);
                break;
            default:
                System.err.println("Invalid choice: " + choice);
                System.err.println("Please select 1, 2, or 3");
        }

        System.out.println("\n------- UtilJob finished -------");
    }
}