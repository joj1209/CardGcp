package convert;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.*;

/**
 * ScanSourceTarget
 * -----------------------------------------
 * - SRC_ROOT 아래의 *.sql 파일을 모두 스캔
 * - Source / Target 테이블 추출
 * - OUT_ROOT 아래 동일한 경로 구조로 .source_target.txt 생성
 * - JDK 1.7 호환
 */
public class ScanSourceTarget {

    // 입력 폴더
    private static final Path SRC_ROOT = Paths.get("D:\\11. Project\\11. DB");

    // 출력 폴더
    private static final Path OUT_ROOT = Paths.get("D:\\11. Project\\11. DB_OUT2");

    private static final Charset INPUT_CHARSET = StandardCharsets.UTF_8;
    private static final String TARGET_EXT = ".sql";

    public static void main(String[] args) throws Exception {

        if (!Files.isDirectory(SRC_ROOT)) {
            throw new IllegalArgumentException("입력 폴더 없음: " + SRC_ROOT.toAbsolutePath());
        }

        Files.createDirectories(OUT_ROOT);

        final int[] count = {0};
        Files.walkFileTree(SRC_ROOT, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {

                if (file.getFileName().toString().toLowerCase().endsWith(TARGET_EXT)) {
                    processFile(file);
                    count[0]++;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("\n[완료] 스캔한 SQL 파일 수: " + count[0] + "개");
    }


    /** 개별 SQL 파일 처리 */
    private static void processFile(Path sqlFile) throws IOException {

        // 1) 읽기
        String content = readFile(sqlFile, INPUT_CHARSET);

        // 2) 주석 제거 후 테이블 추출
        String noComments = stripComments(content);
        Tables tables = extractTables(noComments);

        // 3) 출력 문자열 생성 (콘솔/파일 공통)
        String report = buildReport(sqlFile, tables);

        // 4) 출력 파일명 생성
        Path relative = SRC_ROOT.relativize(sqlFile);      // 예: "adw/A01.sql"
        Path parent = relative.getParent();
        String fileName = relative.getFileName().toString();

        int dot = fileName.lastIndexOf('.');
        String base = (dot > 0 ? fileName.substring(0, dot) : fileName);
        String outFileName = base + ".source_target.txt";

        Path outFile;
        if (parent != null) {
            outFile = OUT_ROOT.resolve(parent).resolve(outFileName);
        } else {
            outFile = OUT_ROOT.resolve(outFileName);
        }

        // 상위 디렉토리 생성 (파일 존재할 경우 충돌 방지)
        Path outParent = outFile.getParent();
        if (outParent != null && (!Files.exists(outParent) || !Files.isDirectory(outParent))) {
            Files.createDirectories(outParent);
        }

        // 5) 파일 쓰기
        Files.write(outFile,
                report.getBytes("UTF-8"),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        // 6) 콘솔 출력
        System.out.println(report);
    }


    /** 콘솔 + 파일 공통 출력 문자열 생성 */
    private static String buildReport(Path sqlFile, Tables tables) {

        StringBuilder sb = new StringBuilder();

        sb.append("FILE: ").append(sqlFile.toAbsolutePath()).append("\n\n");

        if (tables.targets.isEmpty() && tables.sources.isEmpty()) {
            sb.append("(추출된 테이블 없음)\n");
            return sb.toString();
        }

        if (!tables.targets.isEmpty()) {
            sb.append("[Target Tables]\n");
            int i = 1;
            for (String t : tables.targets) {
                sb.append("  ").append(i++).append(". ").append(t).append("\n");
            }
            sb.append("\n");
        }

        if (!tables.sources.isEmpty()) {
            sb.append("[Source Tables]\n");
            int i = 1;
            for (String s : tables.sources) {
                sb.append("  ").append(i++).append(". ").append(s).append("\n");
            }
        }

        return sb.toString();
    }


    /** 파일 읽기 (JDK7 호환). UTF-8로 읽은 결과에 SQL 키워드가 거의 없으면 MS949로 재시도 */
    private static String readFile(Path file, Charset cs) throws IOException {
        byte[] bytes = Files.readAllBytes(file);

        String content = decode(bytes, cs);

        // 간단한 휴리스틱: 주요 SQL 키워드가 없으면 인코딩을 MS949로 시도
        Pattern kw = Pattern.compile("(?i)\\b(INSERT|FROM|JOIN|UPDATE|DELETE|MERGE)\\b");
        if (!kw.matcher(content).find()) {
            Charset ms949 = Charset.forName("MS949");
            if (!ms949.equals(cs)) {
                String alt = decode(bytes, ms949);
                if (kw.matcher(alt).find()) {
                    content = alt;
                }
            }
        }

        return content;
    }

    private static String decode(byte[] bytes, Charset cs) {
        CharsetDecoder dec = cs.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            return dec.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            // fallback: best-effort
            return new String(bytes, cs);
        }
    }


    /** SQL에서 블록/라인 주석을 제거 (주석 내부의 백틱이 테이블 추출을 방해함) */
    private static String stripComments(String sql) {
        if (sql == null || sql.isEmpty()) return sql;
        // remove block comments /* ... */
        String noBlock = sql.replaceAll("(?s)/\\*.*?\\*/", " ");
        // remove line comments -- ... (till end of line)
        String noLine = noBlock.replaceAll("(?m)--[^\\r\\n]*", " ");
        return noLine;
    }


    /** 소스/타겟 테이블 목록 */
    private static class Tables {
        Set<String> sources = new LinkedHashSet<String>();
        Set<String> targets = new LinkedHashSet<String>();
    }


    /** 테이블 추출 */
    private static Tables extractTables(String sql) {
        Tables tables = new Tables();
        if (sql == null || sql.isEmpty()) return tables;

        // 테이블 식별자: 백틱(`...`), 큰따옴표("..."), 대괄호([...]) 또는 일반 식별자(유니코드 문자 포함) 지원.
        // schema.table 또는 table 단독을 모두 허용
        String id = "(?:`[^`]+`|\"[^\"]+\"|\\[[^\\]]+\\]|[\\p{L}0-9_\\$]+)";
        String tableId = "(" + id + "(?:\\." + id + ")?)";

        Pattern insertInto = Pattern.compile("(?is)\\bINSERT\\s+INTO\\s+" + tableId);
        Pattern updateTgt  = Pattern.compile("(?is)\\bUPDATE\\s+" + tableId);
        Pattern mergeInto  = Pattern.compile("(?is)\\bMERGE\\s+INTO\\s+" + tableId);
        Pattern deleteFrom = Pattern.compile("(?is)\\bDELETE\\s+FROM\\s+" + tableId);

        Pattern fromSrc    = Pattern.compile("(?is)\\bFROM\\s+" + tableId);
        Pattern joinSrc    = Pattern.compile("(?is)\\bJOIN\\s+" + tableId);

        // Target
        findTables(sql, insertInto, tables.targets);
        findTables(sql, updateTgt , tables.targets);
        findTables(sql, mergeInto , tables.targets);
        findTables(sql, deleteFrom, tables.targets);

        // Source
        findTables(sql, fromSrc, tables.sources);
        findTables(sql, joinSrc, tables.sources);

        return tables;
    }


    /** 패턴으로 테이블명 추출 */
    private static void findTables(String sql, Pattern p, Set<String> into) {
        Matcher m = p.matcher(sql);
        while (m.find()) {
            into.add(clean(m.group(1)));
        }
    }


    /** 테이블명 끝의 ; , ) 제거 및 따옴표/백틱/대괄호 제거, alias 제거 */
    private static String clean(String s) {
        if (s == null) return "";
        String t = s.trim();

        // remove surrounding parentheses if present
        while (t.startsWith("(") && t.endsWith(")")) {
            t = t.substring(1, t.length()-1).trim();
        }

        // remove trailing punctuation
        while (!t.isEmpty()) {
            char c = t.charAt(t.length() - 1);
            if (c == ',' || c == ';' || c == ')' || c == '\n' || c == '\r') {
                t = t.substring(0, t.length() - 1).trim();
            } else break;
        }

        // Remove alias: take only the identifier part before whitespace
        // e.g. "schema.table t" -> "schema.table"
        int sp = t.indexOf(' ');
        if (sp > 0) t = t.substring(0, sp).trim();

        // remove surrounding quotes/backticks/brackets on each part
        // split by dot and clean each segment
        String[] parts = t.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].trim();
            if ((p.startsWith("`") && p.endsWith("`")) || (p.startsWith("\"") && p.endsWith("\"")) ) {
                p = p.substring(1, p.length()-1);
            }
            if (p.startsWith("[") && p.endsWith("]")) {
                p = p.substring(1, p.length()-1);
            }
            parts[i] = p;
        }
        return String.join(".", parts);
    }
}
