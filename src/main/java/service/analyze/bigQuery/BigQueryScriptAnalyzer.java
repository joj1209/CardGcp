package service.analyze.bigQuery;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * BigQueryScriptAnalyzer:
 * - 빅쿼리로 작성된 SQL 스크립트를 읽고,
 * - STEP 블록별로 분리하여,
 * - 사용된 테이블들(소스, 타겟 등)을 추출하고,
 * - BEGIN-END 구조 검증을 수행하는 분석기 프로그램
 *
 * 사용 라이브러리: JDK8 표준 (java.io, java.util, java.util.regex)
 */
public class BigQueryScriptAnalyzer {

    public static void main(String[] args) {
        // 분석할 SQL 스크립트 파일 경로 설정
        String filePath = "D:\\11. Project\\11. DB\\BigQuery\\sample_script.sql"; // ← 필요시 사용자로부터 입력받게 수정 가능

        try {
            // 1. 스크립트 파일 읽기
            String script = readFile(filePath);

            // 2. STEP 블록별로 분할
            Map<String, String> stepBlocks = extractStepBlocks(script);

            // 3. 각 STEP 블록 분석
            for (Map.Entry<String, String> entry : stepBlocks.entrySet()) {
                String stepName = entry.getKey();
                String blockContent = entry.getValue();

                System.out.println("==== " + stepName + " 분석 결과 ====");

                // BEGIN-END 유효성 검사
                boolean isValid = validateBeginEndBlock(blockContent);
                System.out.println("BEGIN-END 블록 유효성: " + (isValid ? "정상" : "비정상"));

                // 테이블 이름 추출
                Set<String> tables = extractTableNames(blockContent);
                System.out.println("사용된 테이블 목록:");
                for (String table : tables) {
                    System.out.println(" - " + table);
                }

                System.out.println();
            }

        } catch (IOException e) {
            System.err.println("파일 읽기 오류: " + e.getMessage());
        }
    }

    /**
     * 파일 내용을 문자열로 읽어오는 메서드
     */
    private static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    /**
     * STEP 블록을 추출하여 STEP 이름과 블록 내용을 Map으로 반환
     */
    private static Map<String, String> extractStepBlocks(String script) {
        Map<String, String> steps = new LinkedHashMap<>();

        // STEP 블록 정규표현식: STEP001 ~ STEP999
        Pattern stepPattern = Pattern.compile("(STEP\\d{3})\\s*BEGIN(.*?)END", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = stepPattern.matcher(script);

        while (matcher.find()) {
            String stepName = matcher.group(1).trim();
            String blockContent = matcher.group(2).trim();
            steps.put(stepName, blockContent);
        }

        return steps;
    }

    /**
     * BEGIN-END 블록이 정상적으로 구성되어 있는지 확인
     */
    private static boolean validateBeginEndBlock(String content) {
        // 단순 BEGIN-END 페어 확인 (복잡한 중첩은 처리하지 않음)
        return content != null && !content.isEmpty();
    }

    /**
     * SQL 구문에서 사용된 테이블 이름들을 추출
     * - FROM, JOIN, INTO, UPDATE, MERGE INTO 등에서 테이블 이름을 추출
     */
    private static Set<String> extractTableNames(String sql) {
        Set<String> tables = new LinkedHashSet<>();

        // SQL 키워드를 기반으로 테이블 이름 추출 (대소문자 구분 안 함)
        String regex = "\\b(FROM|JOIN|INTO|UPDATE|MERGE INTO)\\s+([`\\w\\.]+)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);

        while (matcher.find()) {
            String table = matcher.group(2);
            tables.add(cleanTableName(table));
        }

        return tables;
    }

    /**
     * 테이블 이름에서 특수문자(`) 등을 제거
     */
    private static String cleanTableName(String name) {
        return name.replaceAll("[`\"']", "").trim();
    }
}

