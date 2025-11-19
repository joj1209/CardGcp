package convert;

import java.com.log.AppLogger;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 매우 단순한 Source/Target 테이블 스캐너
 * - 입력 루트 아래의 .sql 파일을 순회하며 Source/Target 테이블을 추출
 * - 백틱(`)은 제거하지 않고 그대로 보존
 * - 최소한의 정규식만 사용 (INSERT/UPDATE/MERGE/DELETE → Target, FROM/JOIN → Source)
 * - 출력: 콘솔에 파일별 결과를 표시하고, OUT_ROOT에 .source_target.txt 저장
 */
public class SimpleSourceTarget {

    private static final AppLogger log = AppLogger.getLogger(SimpleSourceTarget.class);

    // 설정: 필요 시 경로를 수정하세요
    private static final Path SRC_ROOT = Paths.get("D:\\11. Project\\11. DB");
    private static final Path OUT_ROOT = Paths.get("D:\\11. Project\\11. DB_OUT3");
    private static final Charset INPUT_CHARSET = StandardCharsets.UTF_8;

    // 간단한 패턴 구성
    private static final String ID = "(?:`[^`]+`|\"[^\"]+\"|\\[[^\\]]+\\]|[\\p{L}0-9_\\$]+)"; // 식별자(백틱/따옴표/대괄호/유니코드 허용)
    private static final String TABLE = ID + "(?:\\s*\\.\\s*" + ID + ")?"; // schema.table 또는 table

    private static final Pattern P_INSERT = Pattern.compile("(?is)\\bINSERT\\s+INTO\\s+(" + TABLE + ")");
    private static final Pattern P_UPDATE = Pattern.compile("(?is)\\bUPDATE\\s+(" + TABLE + ")");
    private static final Pattern P_MERGE  = Pattern.compile("(?is)\\bMERGE\\s+INTO\\s+(" + TABLE + ")");
    private static final Pattern P_DELETE = Pattern.compile("(?is)\\bDELETE\\s+FROM\\s+(" + TABLE + ")");

    private static final Pattern P_FROM  = Pattern.compile("(?is)\\bFROM\\s+(" + TABLE + ")");
    private static final Pattern P_JOIN  = Pattern.compile("(?is)\\bJOIN\\s+(" + TABLE + ")");

    public static void main(String[] args) throws Exception {
        log.start("Source/Target 테이블 추출");

        if (!Files.isDirectory(SRC_ROOT)) {
            log.error("입력 폴더가 존재하지 않습니다: %s", SRC_ROOT.toAbsolutePath());
            return;
        }

        log.info("입력 폴더: %s", SRC_ROOT.toAbsolutePath());
        log.info("출력 폴더: %s", OUT_ROOT.toAbsolutePath());

        Files.createDirectories(OUT_ROOT);

        final int[] count = {0};
        log.sqlScanStart(SRC_ROOT.toString());

        Files.walkFileTree(SRC_ROOT, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().toLowerCase().endsWith(".sql")) {
                    scanFile(file);
                    count[0]++;
                    if (count[0] % 10 == 0) {
                        log.info("처리 중... (%d개 파일)", count[0]);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        log.sqlScanEnd(count[0]);
        log.end("Source/Target 테이블 추출", count[0]);
    }

    private static void scanFile(Path sqlFile) throws IOException {
        log.fileStart(sqlFile.getFileName().toString());

        String raw = new String(Files.readAllBytes(sqlFile), INPUT_CHARSET);
        String sql = stripComments(raw); // 주석 제거 (백틱은 제거하지 않음)

        Set<String> targets = new LinkedHashSet<String>();
        Set<String> sources = new LinkedHashSet<String>();

        find(sql, P_INSERT, targets);
        find(sql, P_UPDATE, targets);
        find(sql, P_MERGE , targets);
        find(sql, P_DELETE, targets);

        find(sql, P_FROM, sources);
        find(sql, P_JOIN, sources);

        // 로그 출력
        log.tableExtracted(sqlFile.getFileName().toString(), sources.size(), targets.size());

        // 리포트 생성
        StringBuilder report = new StringBuilder();
        report.append("FILE: ").append(sqlFile.toAbsolutePath()).append("\n\n");
        if (targets.isEmpty() && sources.isEmpty()) {
            report.append("(추출된 테이블 없음)\n");
        } else {
            if (!targets.isEmpty()) {
                report.append("[Target]\n");
                int i = 1; for (String t : targets) report.append("  ").append(i++).append(". ").append(t).append("\n");
                report.append("\n");
            }
            if (!sources.isEmpty()) {
                report.append("[Source]\n");
                int i = 1; for (String s : sources) report.append("  ").append(i++).append(". ").append(s).append("\n");
            }
        }

        // 출력 경로 계산 및 저장 (SRC_ROOT 상대 경로를 유지)
        Path relative = SRC_ROOT.relativize(sqlFile);
        Path parent = relative.getParent();
        String fileName = relative.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = (dot > 0) ? fileName.substring(0, dot) : fileName;
        String outFileName = base + ".source_target.txt";
        Path outFile = (parent != null) ? OUT_ROOT.resolve(parent).resolve(outFileName) : OUT_ROOT.resolve(outFileName);
        Path outParent = outFile.getParent();
        if (outParent != null) Files.createDirectories(outParent);
        Files.write(outFile, report.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.debug("결과 파일 저장: %s", outFile.getFileName());
    }

    private static void find(String sql, Pattern p, Set<String> out) {
        Matcher m = p.matcher(sql);
        while (m.find()) {
            String t = clean(m.group(1));
            if (!t.isEmpty()) out.add(t);
        }
    }

    // 주석 제거: 블록 주석(/* ... */)과 라인 주석(-- ...)
    private static String stripComments(String s) {
        if (s == null || s.isEmpty()) return s;
        String noBlock = s.replaceAll("(?s)/\\*.*?\\*/", " ");
        String noLine = noBlock.replaceAll("(?m)--[^\\r\\n]*", " ");
        return noLine;
    }

    // 식별자 정리: 양끝 공백/괄호/구분자만 제거. 백틱/따옴표/대괄호는 보존.
    private static String clean(String s) {
        if (s == null) return "";
        String t = s.trim();
        while (t.startsWith("(")) t = t.substring(1).trim();
        while (t.endsWith(")")) t = t.substring(0, t.length()-1).trim();
        while (!t.isEmpty()) {
            char c = t.charAt(t.length()-1);
            if (c == ',' || c == ';' || c == '\n' || c == '\r') t = t.substring(0, t.length()-1).trim();
            else break;
        }
        // alias 제거: 공백으로 구분된 첫 토큰만 유지
        int sp = t.indexOf(' ');
        if (sp > 0) t = t.substring(0, sp);
        return t;
    }
}
