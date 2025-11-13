package convert;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * SQL 파일 변환 도구 (Step 2 - 주석 보강)
 *
 * 기능:
 * - 터미널에서 다중 선택으로 변환 항목 선택 (1.전체, 2.EUCKR->UTF8, 3.주석내백틱제거)
 * - 입력 폴더를 재귀 탐색하여 SQL 파일만 처리
 * - 출력 폴더에 원본과 동일한 상대경로로 결과 저장
 * - UTF-8 변환 또는 주석 내 백틱 제거 선택 가능
 */
public class ConvertStep2Comment {

    // 입력 폴더 루트 (재귀 탐색 대상)
    private static final Path SRC_ROOT = Paths.get("D:\\11. Project\\11. DB");

    // 출력 폴더 루트 (상대 경로 보존하여 결과 저장)
    private static final Path OUT_ROOT = Paths.get("D:\\11. Project\\11. DB_OUT");

    // 처리 대상 확장자 (소문자 비교)
    private static final String TARGET_EXT = ".sql";

    // 입력 파일을 읽을 때 사용할 문자셋
    private static final Charset INPUT_CHARSET = Charset.forName("MS949");

    // EUCKR -> UTF-8 변환 여부
    private static boolean convertToUtf8 = false;

    // 주석 내 백틱(`) 제거 여부
    private static boolean removeBackticks = false;

    public static void main(String[] args) throws Exception {

        // 입력/출력 폴더 점검 및 생성
        if (!Files.isDirectory(SRC_ROOT)) {
            throw new IllegalArgumentException("입력 폴더가 없습니다: " + SRC_ROOT.toAbsolutePath());
        }
        Files.createDirectories(OUT_ROOT);

        // 터미널에서 변환 항목 다중 선택
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("=== SQL 파일 변환 도구 ===");
            System.out.println("변환할 항목을 선택하세요 (쉼표로 다중 선택 가능, 예: 1,3)");
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

            // 선택값을 내부 옵션 플래그로 반영
            if (selections.contains(1)) {
                convertToUtf8 = true;
                removeBackticks = true;
                System.out.println("✓ 전체 변환 선택됨 (2 + 3)");
            } else {
                if (selections.contains(2)) {
                    convertToUtf8 = true;
                    System.out.println("✓ EUCKR->UTF8 변환 선택됨");
                }
                if (selections.contains(3)) {
                    removeBackticks = true;
                    System.out.println("✓ 주석 내 백틱 제거 선택됨");
                }
            }
        }

        // 실행 정보 출력
        System.out.println("\n[시작] SRC=" + SRC_ROOT.toAbsolutePath());
        System.out.println("       OUT=" + OUT_ROOT.toAbsolutePath());
        System.out.println("       입력 인코딩=" + INPUT_CHARSET.displayName());
        System.out.println("       옵션: EUCKR->UTF8=" + convertToUtf8 + ", 주석내 백틱 제거=" + removeBackticks);

        // 파일 처리 루프 (재귀 탐색)
        final int[] count = {0};
        Files.walkFileTree(SRC_ROOT, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // 대상 확장자만 처리 (대소문자 무시)
                if (file.getFileName().toString().toLowerCase().endsWith(TARGET_EXT)) {
                    processFile(file, count);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("[완료] 변환 파일 수: " + count[0] + "개");
        System.out.println("참고: DML 'set vs_jb_step = vs_jb_step + 1;' 문은 변경하지 않습니다.");
    }

    /**
     * 쉼표로 구분된 숫자 선택 문자열을 파싱하여 1~3 범위의 정수 집합으로 변환
     */
    private static Set<Integer> parseSelections(String input) {
        Set<Integer> selections = new LinkedHashSet<>();
        if (input == null || input.isBlank()) return selections;

        String[] parts = input.split(",");
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part.trim());
                if (num >= 1 && num <= 3) {
                    selections.add(num);
                }
            } catch (NumberFormatException e) {
                // 숫자가 아니면 무시
            }
        }
        return selections;
    }

    /**
     * 단일 파일 처리 파이프라인
     */
    private static void processFile(Path file, int[] count) {
        try {
            // 출력 경로: 입력 루트 기준 상대경로를 OUT_ROOT에 매핑
            Path outputFile = OUT_ROOT.resolve(SRC_ROOT.relativize(file));
            Files.createDirectories(outputFile.getParent());

            // 파일 읽기
            String content = readFile(file);

            // 주석 내 백틱 제거 (옵션 선택 시)
            if (removeBackticks) {
                content = removeBackticksInComments(content);
            }

            // 파일 쓰기 (선택에 따라 UTF-8 또는 원본 인코딩)
            writeFile(outputFile, content);

            count[0]++;
        } catch (IOException e) {
            System.err.println("파일 처리 실패: " + file.getFileName() + " - " + e.getMessage());
        }
    }

    /**
     * 파일을 INPUT_CHARSET으로 읽어 문자열로 반환
     */
    private static String readFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        CharsetDecoder decoder = INPUT_CHARSET.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    }

    /**
     * 문자열을 선택된 문자셋으로 인코딩하여 파일로 저장
     */
    private static void writeFile(Path file, String content) throws IOException {
        Charset writeCharset = convertToUtf8 ? StandardCharsets.UTF_8 : INPUT_CHARSET;

        CharsetEncoder encoder = writeCharset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        CharBuffer cb = CharBuffer.wrap(content);
        ByteBuffer buffer = encoder.encode(cb);

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 주석 내 백틱(`) 제거
     * - 라인 주석(--) 및 블록 주석() 내부의 백틱만 제거
     * - 문자열 리터럴과 코드/식별자의 백틱은 보존
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

            // 라인 주석 범위: 백틱만 제거
            if (inLineComment) {
                if (ch == '`') continue;
                result.append(ch);
                if (ch == '\n') inLineComment = false;
                continue;
            }

            // 블록 주석 범위: 백틱만 제거
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

            // 문자열 내부: 그대로 복사
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

            // 라인 주석 시작
            if (ch == '-' && next == '-') {
                result.append(ch).append(next);
                inLineComment = true;
                i++;
                continue;
            }

            // 블록 주석 시작
            if (ch == '/' && next == '*') {
                result.append(ch).append(next);
                inBlockComment = true;
                i++;
                continue;
            }

            // 일반 코드 영역: 그대로 복사 (식별자의 백틱 유지)
            result.append(ch);
        }

        return result.toString();
    }
}

