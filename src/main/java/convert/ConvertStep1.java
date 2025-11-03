package com.cardgcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * SQL 파일 변환 도구
 * - 주석 내부의 백틱(`)만 제거
 * - DML 'set vs_jb_step = vs_jb_step + 1;' 문은 변경하지 않음
 * - 한글 테이블/컬럼명의 백틱은 유지
 */
public class ConvertStep1 {

    // ========== 설정 (여기서 경로 변경) ==========
    private static final Path SRC_ROOT = Paths.get("D:\\11. Project\\11. DB");
    private static final Path OUT_ROOT = Paths.get("D:\\11. Project\\11. DB_OUT");
    private static final Charset CHARSET = Charset.forName("MS949");
    private static final String TARGET_EXT = ".sql";

    public static void main(String[] args) throws Exception {
        if (!Files.isDirectory(SRC_ROOT)) {
            throw new IllegalArgumentException("입력 폴더가 없습니다: " + SRC_ROOT);
        }
        Files.createDirectories(OUT_ROOT);

        System.out.println("[시작] SRC=" + SRC_ROOT.toAbsolutePath());
        System.out.println("       OUT=" + OUT_ROOT.toAbsolutePath());

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

        System.out.println("[완료] 변환 파일 수: " + count[0] + "개");
    }

    private static void processFile(Path file, int[] count) {
        try {
            Path outputFile = OUT_ROOT.resolve(SRC_ROOT.relativize(file));
            Files.createDirectories(outputFile.getParent());

            String content = readFile(file);
            String converted = removeBackticksInComments(content);
            writeFile(outputFile, converted);

            count[0]++;
        } catch (IOException e) {
            System.err.println("파일 처리 실패: " + file.getFileName() + " - " + e.getMessage());
        }
    }

    private static String readFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        CharsetDecoder decoder = CHARSET.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    }

    private static void writeFile(Path file, String content) throws IOException {
        CharsetEncoder encoder = CHARSET.newEncoder()
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

            // 일반 문자
            result.append(ch);
        }

        return result.toString();
    }
}

