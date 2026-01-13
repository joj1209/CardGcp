# FileUtil 패키지 소스 코드 분석

## 1. 개요
`service.fileUtil` 패키지는 파일 처리 유틸리티 기능을 제공하는 모듈입니다. 주요 기능으로는 파일 인코딩 변환(EUC-KR <-> UTF-8), 줄 끝 공백 제거, 탭을 스페이스로 변환(2칸/4칸) 등이 있으며, 대용량 파일 및 디렉토리 단위의 일괄 처리를 지원합니다.

## 2. 패키지 구조
- **job**: 실행 진입점 및 파이프라인 제어 (`UtilJob`)
- **processor**: 데이터 변환 로직 (`ConvertStep`)
- **reader**: 파일 읽기 (`SqlReader`)
- **writer**: 파일 쓰기 (`SqlWriter`)

---

## 3. 소스 코드 상세 분석

### 3.1. service.fileUtil.job.UtilJob.java
프로그램의 메인 실행 클래스로, 사용자 입력을 받아 전체 처리 흐름(Pipeline)을 제어합니다.

```java
package service.fileUtil.job;

import service.fileUtil.processor.ConvertStep;
import service.fileUtil.reader.SqlReader;
import service.fileUtil.writer.SqlWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.stream.Stream;

public class UtilJob {
    // 각 컴포넌트 인스턴스 생성 (Reader, Writer, Processor)
    static SqlReader reader = new SqlReader();
    static SqlWriter writer = new SqlWriter();
    static ConvertStep processor = new ConvertStep();

    public static void main(String[] args) throws IOException {
        System.out.println("------- UtilJob started -------");

        // 입력 인자 검증 (입력 경로, 출력 경로 필수)
        if (args.length < 2) {
            System.err.println("Usage: java UtilJob <input_path> <output_path>");
            return;
        }

        // 입력 및 출력 경로 설정
        Path inputPath = Paths.get(args[0]);
        Path outputPath = Paths.get(args[1]);

        System.out.println("Input Path: " + inputPath.toAbsolutePath());
        System.out.println("Output Path: " + outputPath.toAbsolutePath());

        // 사용자 선택 메뉴 출력
        System.out.println("\n========================================");
        System.out.println("Select processing option:");
        System.out.println("0. No conversion (read only)");
        System.out.println("1. EUC-KR -> UTF-8");
        System.out.println("2. UTF-8 -> EUC-KR");
        System.out.println("3. Remove trailing spaces (UTF-8 -> UTF-8)");
        System.out.println("4. Convert tabs to 2 spaces (UTF-8 -> UTF-8)");
        System.out.println("5. Convert tabs to 4 spaces (UTF-8 -> UTF-8)");
        System.out.println("========================================");
        System.out.print("Enter your choice (0-5): ");

        // 사용자 입력 대기
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String choice = br.readLine().trim();

        // 선택에 따른 분기 처리
        switch (choice) {
            case "0": // 읽기 전용 모드
                System.out.println("\n>>> Read only mode (no conversion)\n");
                reader.run(inputPath);
                break;
            case "1": // EUC-KR -> UTF-8 변환
                System.out.println("\n>>> Converting: EUC-KR -> UTF-8\n");
                // processor::process 메서드 참조를 전달하여 기본 변환 수행
                processConversion(inputPath, outputPath, SqlReader.EUCKR, SqlReader.UTF8, processor::process);
                break;
            case "2": // UTF-8 -> EUC-KR 변환
                System.out.println("\n>>> Converting: UTF-8 -> EUC-KR\n");
                processConversion(inputPath, outputPath, SqlReader.UTF8, SqlReader.EUCKR, processor::process);
                break;
            case "3": // 줄 끝 공백 제거
                System.out.println("\n>>> Removing trailing spaces (UTF-8 -> UTF-8)\n");
                // processor::removeTrailingSpaces 메서드 참조를 전달하여 공백 제거 수행
                processConversion(inputPath, outputPath, SqlReader.UTF8, SqlReader.UTF8, processor::removeTrailingSpaces);
                break;
            case "4": // 탭을 2개 스페이스로 변환
                System.out.println("\n>>> Converting tabs to 2 spaces (UTF-8 -> UTF-8)\n");
                // 람다식으로 spaceCount 매개변수를 2로 전달
                processConversion(inputPath, outputPath, SqlReader.UTF8, SqlReader.UTF8,
                    content -> processor.convertTabsToSpaces(content, 2));
                break;
            case "5": // 탭을 4개 스페이스로 변환
                System.out.println("\n>>> Converting tabs to 4 spaces (UTF-8 -> UTF-8)\n");
                // 람다식으로 spaceCount 매개변수를 4로 전달
                processConversion(inputPath, outputPath, SqlReader.UTF8, SqlReader.UTF8,
                    content -> processor.convertTabsToSpaces(content, 4));
                break;
            default:
                System.err.println("Invalid choice: " + choice);
                System.err.println("Please select 0, 1, 2, 3, 4, or 5");
        }

        System.out.println("\n------- UtilJob finished -------");
    }

    // 변환 작업의 진입점 (파일/폴더 구분)
    private static void processConversion(Path inputPath, Path outputPath, Charset fromCharset, Charset toCharset, Function<String, String> transformer) throws IOException {
        if (Files.isDirectory(inputPath)) {
            processDirectory(inputPath, outputPath, fromCharset, toCharset, transformer);
        } else if (Files.isRegularFile(inputPath)) {
            processFile(inputPath, outputPath, fromCharset, toCharset, transformer);
        } else {
            throw new IllegalArgumentException("Invalid path: " + inputPath);
        }
    }

    // 디렉토리 처리 로직 (재귀적 탐색)
    private static void processDirectory(Path inputDir, Path outputDir, Charset fromCharset, Charset toCharset, Function<String, String> transformer) throws IOException {
        System.out.println("Converting directory: " + inputDir.toAbsolutePath());
        System.out.println("Output directory: " + outputDir.toAbsolutePath());
        System.out.println("From: " + fromCharset.name() + " -> To: " + toCharset.name());

        try (Stream<Path> paths = Files.walk(inputDir)) {
            paths.filter(Files::isRegularFile) // 파일만 필터링
                    .filter(p -> p.getFileName().toString().endsWith(".sql")) // .sql 확장자만 필터링
                    .forEach(inputFile -> {
                        try {
                            // Step 1: Read (파일 읽기)
                            String content = reader.read(inputFile, fromCharset);

                            // Step 2: Process (변환 로직 적용 - 주입받은 transformer 사용)
                            String processedContent = transformer.apply(content);

                            // Step 3: Write (파일 쓰기 - 상대 경로 유지)
                            writer.writeWithRelativePath(inputFile, inputDir, outputDir, processedContent, fromCharset, toCharset);
                        } catch (IOException e) {
                            System.err.println("Failed to convert file: " + inputFile + " - " + e.getMessage());
                        }
                    });
        }
    }

    // 단일 파일 처리 로직
    private static void processFile(Path inputFile, Path outputPath, Charset fromCharset, Charset toCharset, Function<String, String> transformer) throws IOException {
        // Step 1: Read
        String content = reader.read(inputFile, fromCharset);

        // Step 2: Process
        String processedContent = transformer.apply(content);

        // Step 3: Write (출력 경로 계산 후 쓰기)
        Path outputFile = writer.resolveOutputFile(inputFile, outputPath);
        writer.writeWithLog(inputFile, outputFile, processedContent, fromCharset, toCharset);
    }
}
```

### 3.2. service.fileUtil.processor.ConvertStep.java
실제 데이터 변환 로직을 담당하는 클래스입니다. 순수 함수 형태로 구현되어 있어 외부 의존성이 없습니다.

```java
package service.fileUtil.processor;

public class ConvertStep {

    // 기본 변환 메서드 (현재는 pass-through)
    public String process(String content) {
        // 현재는 그대로 반환
        // 추후 여기에 변환 로직 추가 가능:
        // - SQL 문법 변환
        // - 주석 제거/변환
        // - 포맷팅
        // - 특정 문자열 치환 등
        return content;
    }

    // 줄 끝 공백 제거 메서드
    public String removeTrailingSpaces(String content) {
        if (content == null) return null;
        // 라인별로 분리하여 각 라인의 끝 공백 제거 후 다시 결합
        return java.util.Arrays.stream(content.split("\n", -1))
                .map(line -> line.replaceAll("\\s+$", ""))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    // 탭을 스페이스로 변환하는 메서드
    public String convertTabsToSpaces(String content, int spaceCount) {
        if (content == null) return null;
        // 탭을 지정된 개수의 스페이스로 변환
        char[] spaces = new char[spaceCount];
        java.util.Arrays.fill(spaces, ' ');
        return content.replace("\t", new String(spaces));
    }
}
```

### 3.3. service.fileUtil.reader.SqlReader.java
파일 시스템에서 파일을 읽어오는 역할을 담당합니다.

```java
package service.fileUtil.reader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class SqlReader {
    // 자주 사용하는 문자셋 상수 정의
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final Charset EUCKR = Charset.forName("EUC-KR");
    public static final Charset DEFAULT_CHARSET = UTF8;

    // 지정된 문자셋으로 파일 내용을 읽어 문자열로 반환
    public String read(Path inputFile, Charset charset) throws IOException {
        return Files.readString(inputFile, charset);
    }

    // 읽기 전용 모드 실행 (파일 정보 출력)
    public void run(Path inputPath) throws IOException {
        if (Files.isDirectory(inputPath)) {
            processDirectory(inputPath);
        } else if (Files.isRegularFile(inputPath)) {
            processFile(inputPath);
        } else {
            throw new IllegalArgumentException("Invalid path: " + inputPath);
        }
    }

    // 디렉토리 내 파일 정보 출력
    private void processDirectory(Path directory) throws IOException {
        System.out.println("Processing directory: " + directory.toAbsolutePath());
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .forEach(path -> {
                        try {
                            processFile(path);
                        } catch (IOException e) {
                            System.err.println("Failed to process file: " + path + " - " + e.getMessage());
                        }
                    });
        }
    }

    // 단일 파일 정보 출력
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
```

### 3.4. service.fileUtil.writer.SqlWriter.java
파일 시스템에 파일을 쓰는 역할을 담당하며, 디렉토리 생성 및 경로 계산 로직을 포함합니다.

```java
package service.fileUtil.writer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class SqlWriter {

    // 파일 쓰기 (부모 디렉토리 자동 생성 포함)
    public void write(Path outputFile, String content, Charset charset) throws IOException {
        // 출력 파일의 부모 디렉토리 생성
        if (outputFile.getParent() != null && !Files.exists(outputFile.getParent())) {
            Files.createDirectories(outputFile.getParent());
        }

        // 파일에 내용 쓰기
        Files.writeString(outputFile, content, charset);
    }

    // 파일 쓰기 및 로그 출력
    public void writeWithLog(Path inputFile, Path outputFile, String content, Charset fromCharset, Charset toCharset) throws IOException {
        // 파일 쓰기
        write(outputFile, content, toCharset);

        // 로그 출력
        System.out.println("✓ Converted: " + inputFile.getFileName() +
                         " (" + fromCharset.name() + " -> " + toCharset.name() + ")" +
                         " -> " + outputFile.toAbsolutePath());
    }

    // 상대 경로를 유지하며 파일 쓰기 (디렉토리 구조 복사)
    public void writeWithRelativePath(Path inputFile, Path inputDir, Path outputDir, String content, Charset fromCharset, Charset toCharset) throws IOException {
        // 출력 파일 경로 계산
        Path outputFile = resolveOutputFileWithRelativePath(inputFile, inputDir, outputDir);

        // 파일 쓰기 + 로그
        writeWithLog(inputFile, outputFile, content, fromCharset, toCharset);
    }

    // 디렉토리 존재 확인 및 생성
    public void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    // 단일 파일 출력 경로 계산
    public Path resolveOutputFile(Path inputFile, Path outputPath) {
        if (Files.isDirectory(outputPath)) {
            return outputPath.resolve(inputFile.getFileName());
        } else {
            return outputPath;
        }
    }

    // 상대 경로를 이용한 출력 경로 계산 (디렉토리 구조 유지)
    public Path resolveOutputFileWithRelativePath(Path inputFile, Path inputDir, Path outputDir) {
        Path relativePath = inputDir.relativize(inputFile);
        return outputDir.resolve(relativePath);
    }
}
```

