package com.cardgcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;
import java.util.HashSet;
import java.util.Set;

/**
 * SQL 파일 변환 도구
 * - 사용자가 변환 옵션을 선택할 수 있음
 * - 다중 선택 지원
 */
public class ConvertSqlStep1 {

    // ========== 설정 (여기서 경로 변경) ==========
    private static final Path SRC_ROOT = Paths.get("D:\\11. Project\\11. DB");
    private static final Path OUT_ROOT = Paths.get("D:\\11. Project\\11. DB_OUT");
    private static final String TARGET_EXT = ".sql";

    // 변환 옵션
    private enum ConversionOption {
        CHARSET_CONVERSION(1, "EUCKR -> UTF8 변환"),
        REMOVE_BACKTICKS(2, "주석 내 백틱(`) 제거");

        final int number;
        final String description;

        ConversionOption(int number, String description) {
            this.number = number;
            this.description = description;
        }
    }

    // 선택된 옵션
    private static Set<ConversionOption> selectedOptions = new HashSet<>();
    private static Charset inputCharset;
    private static Charset outputCharset;

    public static void main(String[] args) throws Exception {
        // 변환 옵션 선택
        selectConversionOptions();

        if (selectedOptions.isEmpty()) {
            System.out.println("선택된 옵션이 없습니다. 프로그램을 종료합니다.");
            return;
        }

        // 폴더 확인 및 생성
        if (!Files.isDirectory(SRC_ROOT)) {
            throw new IllegalArgumentException("입력 폴더가 없습니다: " + SRC_ROOT);
        }
        Files.createDirectories(OUT_ROOT);

        // 선택된 옵션 출력
        System.out.println("\n[선택된 변환 옵션]");
        for (ConversionOption option : selectedOptions) {
            System.out.println("  - " + option.description);
        }

        System.out.println("\n[시작] SRC=" + SRC_ROOT.toAbsolutePath());
        System.out.println("       OUT=" + OUT_ROOT.toAbsolutePath());

        // 파일 변환 실행
        final int[] count = {0};
        Files.walkFileTree(SRC_ROOT, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().toLowerCase().endsWith(TARGET_EXT)) {
                    processFile(file, count);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("\n[완료] 변환 파일 수: " + count[0] + "개");
    }

    /**
     * 사용자로부터 변환 옵션을 선택받음
     */
    private static void selectConversionOptions() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("========================================");
        System.out.println("  SQL 파일 변환 도구");
        System.out.println("========================================");
        System.out.println("\n변환할 항목을 선택하세요 (다중 선택 가능):");
        System.out.println("  0. 전체 (모든 옵션 선택)");
        System.out.println("  1. EUCKR -> UTF8 변환");
        System.out.println("  2. 주석 내 백틱(`) 제거");
        System.out.println("  q. 종료");
        System.out.println("----------------------------------------");
        System.out.print("선택 (예: 1,2 또는 0): ");

        String input = scanner.nextLine().trim();

        // 종료 처리
        if (input.equalsIgnoreCase("q")) {
            System.out.println("프로그램을 종료합니다.");
            System.exit(0);
        }

        // 전체 선택
        if (input.equals("0")) {
            selectedOptions.add(ConversionOption.CHARSET_CONVERSION);
            selectedOptions.add(ConversionOption.REMOVE_BACKTICKS);
            inputCharset = Charset.forName("EUC-KR");
            outputCharset = Charset.forName("UTF-8");
            return;
        }

        // 개별 선택 파싱
        String[] choices = input.split(",");
        for (String choice : choices) {
            choice = choice.trim();
            if (choice.equals("1")) {
                selectedOptions.add(ConversionOption.CHARSET_CONVERSION);
                // 인코딩 변환 선택 시 입출력 charset 설정
                System.out.print("입력 인코딩 (기본: EUC-KR): ");
                String inputEnc = scanner.nextLine().trim();
                inputCharset = inputEnc.isEmpty() ? Charset.forName("EUC-KR") : Charset.forName(inputEnc);

                System.out.print("출력 인코딩 (기본: UTF-8): ");
                String outputEnc = scanner.nextLine().trim();
                outputCharset = outputEnc.isEmpty() ? Charset.forName("UTF-8") : Charset.forName(outputEnc);
            } else if (choice.equals("2")) {
                selectedOptions.add(ConversionOption.REMOVE_BACKTICKS);
            }
        }

        // 인코딩 변환이 선택되지 않았으면 기본값 설정
        if (!selectedOptions.contains(ConversionOption.CHARSET_CONVERSION)) {
            inputCharset = Charset.forName("MS949");
            outputCharset = Charset.forName("MS949");
        }
    }

    /**
     * 파일 처리
     */
    private static void processFile(Path file, int[] count) {
        try {
            Path outputFile = OUT_ROOT.resolve(SRC_ROOT.relativize(file));
            Files.createDirectories(outputFile.getParent());

            // 1. 파일 읽기
            String content = readFile(file);

            // 2. 선택된 옵션에 따라 변환
            String converted = content;

            if (selectedOptions.contains(ConversionOption.REMOVE_BACKTICKS)) {
                converted = removeBackticksInComments(converted);
            }

            // 인코딩 변환은 쓰기 시 자동 적용됨 (readFile/writeFile의 charset 사용)

            // 3. 파일 쓰기
            writeFile(outputFile, converted);

            count[0]++;
        } catch (IOException e) {
            System.err.println("파일 처리 실패: " + file.getFileName() + " - " + e.getMessage());
        }
    }

    /**
     * 파일 읽기 (입력 인코딩으로)
     */
    private static String readFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        CharsetDecoder decoder = inputCharset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    }

    /**
     * 파일 쓰기 (출력 인코딩으로)
     */
    private static void writeFile(Path file, String content) throws IOException {
        CharsetEncoder encoder = outputCharset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        ByteBuffer buffer = encoder.encode(java.nio.CharBuffer.wrap(content));
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 주석 내부의 백틱(`)만 제거
     * - 라인 주석: -- ...
     * - 블록 주석: /* ... *\/
     * - 문자열 리터럴('...', "...")은 주석으로 인식하지 않음
     * - DML 'set vs_jb_step = vs_jb_step + 1;' 문은 변경하지 않음
     */
    private static String removeBackticksInComments(String sql) {
        StringBuilder result = new StringBuilder(sql.length());
        int len = sql.length();

        boolean inString = false;  // '...' 또는 "..."
        boolean inLineComment = false;
        boolean inBlockComment = false;
        char stringChar = '\0';  // ' 또는 "

        for (int i = 0; i < len; i++) {
            char ch = sql.charAt(i);
            char next = (i + 1 < len) ? sql.charAt(i + 1) : '\0';

            // 라인 주석 처리
            if (inLineComment) {
                if (ch == '`') continue;  // 백틱 제거
                result.append(ch);
                if (ch == '\n') inLineComment = false;
                continue;
            }

            // 블록 주석 처리
            if (inBlockComment) {
                if (ch == '`') continue;  // 백틱 제거
                result.append(ch);
                if (ch == '*' && next == '/') {
                    result.append(next);
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            // 문자열 처리
            if (inString) {
                result.append(ch);
                if (ch == stringChar) inString = false;
                continue;
            }

            // 문자열 시작
            if (ch == '\'' || ch == '"') {
                result.append(ch);
                inString = true;
                stringChar = ch;
                continue;
            }

            // 주석 시작 감지
            if (ch == '-' && next == '-') {
                result.append(ch).append(next);
                inLineComment = true;
                i++;
                continue;
            }

            if (ch == '/' && next == '*') {
                result.append(ch).append(next);
                inBlockComment = true;
                i++;
                continue;
            }

            // 일반 문자 (한글 테이블/컬럼명의 백틱은 유지)
            result.append(ch);
        }

        return result.toString();
    }
}
