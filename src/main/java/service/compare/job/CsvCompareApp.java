package service.compare.job;

import service.compare.process.CompositeKeyStrategy;
import service.compare.process.CsvComparator;
import service.compare.process.KeyStrategy;
import service.compare.io.CsvReader;
import service.compare.io.CsvWriter;
import service.compare.model.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Entry point.
 * - 외부 라이브러리 없이 작동
 * - 결과 파일: 원본1과 동일 경로에 result_<원본1파일명>
 */
public class CsvCompareApp {

    private static final String RESULT_COL = "RESULT";
    private static final String DIFF_DETAIL_COL = "DIFF_DETAIL";

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("사용법: java com.example.csvcompare.app.CsvCompareApp <file1.csv> <file2.csv> [key1,key2,...]");
            System.exit(1);
        }

        Path file1 = Paths.get(args[0]);
        Path file2 = Paths.get(args[1]);

        if (!Files.isReadable(file1) || !Files.isReadable(file2)) {
            System.err.println("입력 파일을 읽을 수 없습니다: " + file1 + " / " + file2);
            System.exit(1);
        }

        List<String> userKeys = null;
        if (args.length >= 3 && args[2] != null && !args[2].trim().isEmpty()) {
            userKeys = Arrays.stream(args[2].split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        try {
            CsvTable t1 = CsvReader.read(file1);
            CsvTable t2 = CsvReader.read(file2);

            // 키 전략 결정
            KeyStrategy keyStrategy;
            if (userKeys != null && !userKeys.isEmpty()) {
                ensureKeysExist(userKeys, t1.getHeaders(), t2.getHeaders());
                keyStrategy = new CompositeKeyStrategy(userKeys);
            } else {
                if (t1.getHeaders().isEmpty() || t2.getHeaders().isEmpty()) {
                    throw new IllegalStateException("헤더가 비어 있습니다.");
                }
                String defaultKey = t1.getHeaders().get(0);
                System.out.println("t1.getHeaders(): " + t1.getHeaders());
                System.out.println("t2.getHeaders(): " + t2.getHeaders());



                if (!new HashSet<>(t2.getHeaders()).contains(defaultKey)) {
                    throw new IllegalStateException("키 컬럼이 지정되지 않았고, 두 파일의 첫 헤더가 다릅니다. 키를 명시하세요. 예) ID 또는 ID,DATE");
                }
                System.out.println("[안내] 키 미지정: 첫 번째 헤더 컬럼을 키로 사용합니다 -> " + defaultKey);
                keyStrategy = new CompositeKeyStrategy(Collections.singletonList(defaultKey));
            }

            CsvComparator comparator = new CsvComparator();
            List<String> unifiedHeaders = comparator.unifyHeaders(t1.getHeaders(), t2.getHeaders());

            List<OutputRow> results = comparator.compareTables(t1, t2, keyStrategy, unifiedHeaders);

            // 결과 헤더: 합집합 + RESULT + DIFF_DETAIL
            List<String> outputHeaders = new ArrayList<>(unifiedHeaders);
            outputHeaders.add(RESULT_COL);
            outputHeaders.add(DIFF_DETAIL_COL);

            Path out = (file1.getParent() == null)
                    ? Paths.get("result_" + file1.getFileName().toString())
                    : file1.getParent().resolve("result_" + file1.getFileName().toString());

            CsvWriter.write(out, outputHeaders, results);

            System.out.println("완료: " + out.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("오류: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

    private static void ensureKeysExist(List<String> keys, List<String> h1, List<String> h2) {
        Set<String> s1 = new HashSet<>(h1);
        Set<String> s2 = new HashSet<>(h2);
        for (String k : keys) {
            if (!s1.contains(k) || !s2.contains(k)) {
                throw new IllegalArgumentException("키 컬럼이 양쪽 헤더에 모두 존재해야 합니다. 누락: " + k);
            }
        }
    }
}
