package service.csvCompare.process;

import service.csvCompare.model.*;

import java.util.*;

/** 두 CSV 테이블을 비교하는 서비스 */
public class CsvComparator {

    /** 헤더 합집합(파일1 순서 우선, 파일2에만 있는 컬럼 뒤에 추가) */
    public List<String> unifyHeaders(List<String> h1, List<String> h2) {
        LinkedHashSet<String> set = new LinkedHashSet<>(h1);
        for (String h : h2) set.add(h);
        return new ArrayList<>(set);
    }

    /** 비교 수행: OutputRow 리스트 반환 (출력용 값 + RESULT + DIFF_DETAIL) */
    public List<OutputRow> compareTables(CsvTable t1, CsvTable t2,
                                         KeyStrategy keyStrategy,
                                         List<String> unifiedHeaders) {

        Map<String, DataRow> map1 = toKeyedMap(t1, keyStrategy, "FILE1");
        Map<String, DataRow> map2 = toKeyedMap(t2, keyStrategy, "FILE2");

        // 키 합집합 (file1 순서 우선)
        LinkedHashSet<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(map1.keySet());
        for (String k : map2.keySet()) if (!allKeys.contains(k)) allKeys.add(k);

        List<OutputRow> out = new ArrayList<>(allKeys.size());
        for (String key : allKeys) {
            DataRow r1 = map1.get(key);
            DataRow r2 = map2.get(key);

            Map<String, String> baseValues = new LinkedHashMap<>();
            for (String h : unifiedHeaders) {
                String v = (r1 != null) ? r1.getValue(h) : (r2 != null ? r2.getValue(h) : "");
                baseValues.put(h, v == null ? "" : v);
            }

            ResultType result;
            String diffDetail = "";

            if (r1 != null && r2 != null) {
                List<String> mismatched = new ArrayList<>();
                for (String h : unifiedHeaders) {
                    String a = normalize(r1.getValue(h));
                    String b = normalize(r2.getValue(h));
                    if (!Objects.equals(a, b)) mismatched.add(h);
                }
                if (mismatched.isEmpty()) {
                    result = ResultType.MATCHED;
                } else {
                    result = ResultType.MISMATCHED;
                    diffDetail = buildDiffDetail(mismatched, r1, r2);
                }
            } else if (r1 != null) {
                result = ResultType.ONLY_IN_FILE1;
            } else {
                result = ResultType.ONLY_IN_FILE2;
            }

            out.add(new OutputRow(baseValues, result, diffDetail));
        }

        return out;
    }

    private Map<String, DataRow> toKeyedMap(CsvTable t, KeyStrategy keyStrategy, String tag) {
        Map<String, DataRow> map = new LinkedHashMap<>();
        for (DataRow row : t.getRows()) {
            String key = keyStrategy.buildKey(row.getValues());
            if (map.containsKey(key)) {
                throw new IllegalStateException("키 중복 감지(" + tag + "): " + key + " (키전략=" + keyStrategy + ")");
            }
            map.put(key, row);
        }
        return map;
    }

    private String buildDiffDetail(List<String> cols, DataRow r1, DataRow r2) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            String c = cols.get(i);
            String v1 = safe(r1.getValue(c));
            String v2 = safe(r2.getValue(c));
            if (i > 0) sb.append("; ");
            sb.append(c).append(": '").append(v1).append("' -> '").append(v2).append("'");
        }
        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String normalize(String s) { return s == null ? "" : s.trim(); }
}
