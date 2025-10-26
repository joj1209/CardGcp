package com.cardgcp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class ConvertSqlStep1 {

    // ====== 사용자가 "소스에 코딩"하여 설정 ======
    // 예: Windows: Paths.get("D:/sql/in")
    //     Linux/macOS: Paths.get("/path/to/in")
    private static final Path SRC_ROOT       = Paths.get("D:\\11. Project\\11. DB");
    private static final Path OUT_ROOT       = Paths.get("D:\\11. Project\\11. DB_OUT");

    // 한글 소스라면 MS949 또는 EUC-KR 등 환경에 맞게 지정하세요.
    private static final Charset INPUT_CHARSET  = Charset.forName("MS949");
    private static final Charset OUTPUT_CHARSET = INPUT_CHARSET;

    // 처리 대상 확장자
    private static final String TARGET_EXT   = ".sql";

    public static void main(String[] args) throws Exception {
        if (!Files.isDirectory(SRC_ROOT)) {
            throw new IllegalArgumentException("입력 폴더가 없습니다: " + SRC_ROOT.toAbsolutePath());
        }
        Files.createDirectories(OUT_ROOT);

        System.out.println("[시작] SRC=" + SRC_ROOT.toAbsolutePath());
        System.out.println("       OUT=" + OUT_ROOT.toAbsolutePath());
        final int[] count = {0};

        Files.walkFileTree(SRC_ROOT, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isRegularFile(file) && file.getFileName().toString().toLowerCase().endsWith(TARGET_EXT)) {
                    Path rel = SRC_ROOT.relativize(file);
                    Path out = OUT_ROOT.resolve(rel);
                    Files.createDirectories(out.getParent());

                    // 1) 원문 읽기 (인코딩은 상수로 지정)
                    String src = Files.readString(file, INPUT_CHARSET);

                    // 2) 주석 내부의 백틱만 제거 (그 외는 변경하지 않음)
                    String converted = removeBackticksInsideComments(src);

                    // 3) 그대로 저장 (인코딩/개행 등은 그대로 문자열 기준)
                    Files.writeString(out, converted, OUTPUT_CHARSET,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                    count[0]++;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("[완료] 변환 파일 수: " + count[0] + "개");
        System.out.println("참고: DML 'set vs_jb_step = vs_jb_step + 1;' 문은 변경하지 않습니다.");
    }

    /**
     * 주석 내부(--, /* *\/)에 존재하는 백틱(`)만 제거한다.
     * 문자열 리터럴('...', "...") 내부는 주석 시작 토큰을 무시한다.
     * 그 외(코드/식별자)의 백틱은 그대로 둔다.
     */
    private static String removeBackticksInsideComments(String s) {
        StringBuilder out = new StringBuilder(s.length());

        boolean inSingle = false;        // '...'
        boolean inDouble = false;        // "..."
        boolean inLineComment = false;   // -- ... \n
        boolean inBlockComment = false;  // /* ... */
        boolean escape = false;

        int n = s.length();
        for (int i = 0; i < n; i++) {
            char ch  = s.charAt(i);
            char nxt = (i + 1 < n) ? s.charAt(i + 1) : '\0';

            // 라인 주석 처리 중: 백틱은 제거, 개행 만나면 종료
            if (inLineComment) {
                if (ch == '`') {
                    continue; // 드롭
                }
                out.append(ch);
                if (ch == '\n') inLineComment = false;
                continue;
            }

            // 블록 주석 처리 중: 백틱은 제거, '*/' 만나면 종료
            if (inBlockComment) {
                if (ch == '`') {
                    continue; // 드롭
                }
                if (ch == '*' && nxt == '/') {
                    out.append(ch).append(nxt);
                    i++;
                    inBlockComment = false;
                    continue;
                }
                out.append(ch);
                continue;
            }

            // 문자열 바깥에서만 주석 시작 토큰 인식
            if (!inSingle && !inDouble) {
                if (ch == '-' && nxt == '-') {
                    out.append(ch).append(nxt);
                    i++;
                    inLineComment = true;
                    continue;
                }
                if (ch == '/' && nxt == '*') {
                    out.append(ch).append(nxt);
                    i++;
                    inBlockComment = true;
                    continue;
                }
            }

            // 문자열 토글 (간단 이스케이프 처리)
            if (ch == '\'' && !inDouble) {
                if (!inSingle) inSingle = true;
                else if (!escape) inSingle = false;
                out.append(ch);
                escape = (ch == '\\') && !escape;
                continue;
            }
            if (ch == '\"' && !inSingle) {
                if (!inDouble) inDouble = true;
                else if (!escape) inDouble = false;
                out.append(ch);
                escape = (ch == '\\') && !escape;
                continue;
            }

            out.append(ch);
            escape = (ch == '\\') && !escape;
        }
        return out.toString();
    }
}
