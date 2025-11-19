package convert;

import java.com.log.AppLogger;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * SQL 파일 변환 도구 (Step 2)
 * - 다중 변환 옵션 선택 가능
 * - 1) 전체, 2) EUCKR->UTF8 변환, 3) 주석 내 백틱 제거
 */
public class ConvertStep2 {

    private static final AppLogger log = AppLogger.getLogger(ConvertStep2.class);

    private static final Path SRC_ROOT = Paths.get("D:\\11. Project\\11. DB");
    private static final Path OUT_ROOT = Paths.get("D:\\11. Project\\11. DB_OUT");
    private static final String TARGET_EXT = ".sql";

    private static boolean convertToUtf8 = false;
    private static boolean removeBackticks = false;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== SQL 파일 변환 도구 ===");
        System.out.println("변환할 항목을 선택하세요 (쉼표로 구분, 예: 1,3):");
        System.out.println("1) 전체");
        System.out.println("2) EUCKR->UTF8로 변경");
        System.out.println("3) 주석 내 백틱 제거");
        System.out.print("선택: ");

        String input = scanner.nextLine().trim();
        Set<Integer> selections = parseSelections(input);

        if (selections.isEmpty()) {
            System.out.println("선택된 항목이 없습니다. 종료합니다.");
            return;
        }

        log.start("SQL 파일 변환 (다중 옵션)");

        // 옵션 설정
        if (selections.contains(1)) {
            convertToUtf8 = true;
            removeBackticks = true;
            log.info("✓ 전체 변환 선택됨");
        } else {
            if (selections.contains(2)) {
                convertToUtf8 = true;
                log.info("✓ EUCKR->UTF8 변환 선택됨");
            }
            if (selections.contains(3)) {
                removeBackticks = true;
                log.info("✓ 주석 내 백틱 제거 선택됨");
            }
        }

        // 폴더 확인 및 생성
        if (!Files.isDirectory(SRC_ROOT)) {
            log.error("입력 폴더가 없습니다: %s", SRC_ROOT);
            throw new IllegalArgumentException("입력 폴더가 없습니다: " + SRC_ROOT);
        }
        Files.createDirectories(OUT_ROOT);

        log.info("입력 폴더: %s", SRC_ROOT.toAbsolutePath());
        log.info("출력 폴더: %s", OUT_ROOT.toAbsolutePath());

        // 파일 처리
        final int[] count = {0};
        Files.walkFileTree(SRC_ROOT, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().toLowerCase().endsWith(TARGET_EXT)) {
                    processFile(file, count);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        log.end("SQL 파일 변환", count[0]);
        scanner.close();
    }

    private static Set<Integer> parseSelections(String input) {
        Set<Integer> selections = new HashSet<>();
        String[] parts = input.split(",");
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part.trim());
                if (num >= 1 && num <= 3) {
                    selections.add(num);
                }
            } catch (NumberFormatException e) {
                // 무시
            }
        }
        return selections;
    }

    private static void processFile(Path file, int[] count) {
        try {
            log.fileStart(file.getFileName().toString());

            Path outputFile = OUT_ROOT.resolve(SRC_ROOT.relativize(file));
            Files.createDirectories(outputFile.getParent());

            String content = readFile(file);

            if (removeBackticks) {
                content = removeBackticksInComments(content);
            }

            writeFile(outputFile, content);
            count[0]++;

            log.fileEnd(file.getFileName().toString(), 1);

            if (count[0] % 10 == 0) {
                log.info("처리 중... (%d개 파일)", count[0]);
            }
        } catch (IOException e) {
            log.fileError(file.getFileName().toString(), e);
        }
    }

    private static String readFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        Charset readCharset = Charset.forName("MS949"); // 기본 읽기 인코딩

        CharsetDecoder decoder = readCharset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    }

    private static void writeFile(Path file, String content) throws IOException {
        Charset writeCharset = convertToUtf8 ?
                Charset.forName("UTF-8") : Charset.forName("MS949");

        CharsetEncoder encoder = writeCharset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        ByteBuffer buffer = encoder.encode(java.nio.CharBuffer.wrap(content));
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 주석 내 백틱(`) 제거
     * - 라인 주석(--), 블록 주석(/* *\/)에서만 제거
     * - 문자열 리터럴과 DML 백틱은 유지
     */
    private static String removeBackticksInComments(String sql) {
        StringBuilder result = new StringBuilder(sql.length());
        int len = sql.length();

        boolean inString = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        char stringChar = '\0';

        for (int i = 0; i < len; i++) {
            char ch = sql.charAt(i);
            char next = (i + 1 < len) ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (ch == '`') continue;
                result.append(ch);
                if (ch == '\n') inLineComment = false;
                continue;
            }

            if (inBlockComment) {
                if (ch == '`') continue;
                result.append(ch);
                if (ch == '*' && next == '/') {
                    result.append(next);
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (inString) {
                result.append(ch);
                if (ch == stringChar) inString = false;
                continue;
            }

            if (ch == '\'' || ch == '"') {
                result.append(ch);
                inString = true;
                stringChar = ch;
                continue;
            }

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

            result.append(ch);
        }

        return result.toString();
    }
}

