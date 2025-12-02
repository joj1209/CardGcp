package com.cardgcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FileLineCounter - SQL 파일 분석 도구
 *
 * 기능:
 * - 폴더 내 파일 라인수, INSERT/STEP 키워드 카운트
 * - 타겟/소스 테이블 추출 (주석 제거, CTE 필터링)
 * - 확장자별 파일 수 요약
 * - 전체 소스 테이블 목록
 */
public class FileLineCounter {

    // ========== 설정 ==========
    private static final String DEFAULT_DIR = "D:\\11. Project\\11. DB";
    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // 소스 테이블 화이트리스트 (점이 없어도 허용)
    private static final Set<String> SOURCE_WHITELIST = new HashSet<>(Arrays.asList(
        "DBA_TABLE", "DBA_TAB_COLUMNS", "USER_TAB_COLUMNS"
    ));

    // ========== 집계 컨테이너 ==========
    private static final Map<String, Integer> extCountMap = new LinkedHashMap<>();
    private static final Set<String> allSourceTables = new LinkedHashSet<>();

    // ========== 정규식 패턴 ==========
    private static final Pattern STEP_PATTERN = Pattern.compile("(?i)vs_jb_step\\s*\\+1\\s*;");
    private static final Pattern LINE_COMMENT = Pattern.compile("--.*$", Pattern.MULTILINE);
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    // CTE 이름 추출
    private static final Pattern CTE_PATTERN = Pattern.compile(
        "(?i)\\bwith\\s+([\\w\"]+)\\s+as\\s*\\(");

    // 테이블명 패턴 (스키마.테이블 또는 테이블명)
    private static final String TABLE_NAME = "([\\w\".]+)";

    // 타겟 테이블 (쓰기)
    private static final Pattern INSERT_PATTERN = Pattern.compile(
        "(?i)\\binsert\\s+(?:into\\s+)?" + TABLE_NAME, Pattern.DOTALL);
    private static final Pattern UPDATE_PATTERN = Pattern.compile(
        "(?i)\\bupdate\\s+" + TABLE_NAME, Pattern.DOTALL);
    private static final Pattern DELETE_PATTERN = Pattern.compile(
        "(?i)\\bdelete\\s+from\\s+" + TABLE_NAME, Pattern.DOTALL);
    private static final Pattern MERGE_PATTERN = Pattern.compile(
        "(?i)\\bmerge\\s+into\\s+" + TABLE_NAME, Pattern.DOTALL);

    // 소스 테이블 (읽기)
    private static final Pattern FROM_JOIN_PATTERN = Pattern.compile(
        "(?i)\\b(?:from|join)\\s+" + TABLE_NAME);
    private static final Pattern USING_PATTERN = Pattern.compile(
        "(?i)\\busing\\s+" + TABLE_NAME);

    // ========== 메인 ==========
    public static void main(String[] args) {
        String folderPath = args.length > 0 ? args[0] : DEFAULT_DIR;
        File root = new File(folderPath);

        if (!root.exists() || !root.isDirectory()) {
            System.err.println("지정한 경로가 폴더가 아닙니다: " + folderPath);
            return;
        }

        System.out.println("===== 파일명 | 라인수 | INSERT | STEP | 확장자 | 타겟 | 소스 =====");
        processDirectory(root);

        System.out.println("\n===== 확장자별 파일 건수 요약 =====");
        printExtensionSummary();

        System.out.println("\n===== 전체 소스 테이블 (Distinct, 스키마 보존) =====");
        printAllSourceTables();
    }

    // ========== 폴더 처리 ==========
    private static void processDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else {
                processFile(file);
            }
        }
    }

    // ========== 파일 처리 ==========
    private static void processFile(File file) {
        // 파일 크기 체크
        if (file.length() > MAX_FILE_SIZE) {
            System.err.println("파일이 너무 큽니다(건너뜀): " + file.getName());
            return;
        }

        FileAnalysisResult result = analyzeFile(file);
        if (result == null) return;

        String fileName = file.getName();
        String ext = getExtension(fileName);
        incrementExtCount(ext);

        // 결과 출력
        System.out.printf(
            "파일: %-30s | 라인수: %6d | INSERT: %4d | STEP: %4d | 확장자: %-5s | 타겟: %-20s | 소스: %-30s%n",
            truncate(fileName, 30),
            result.lineCount,
            result.insertCount,
            result.stepCount,
            ext,
            truncate(joinSet(result.targets), 20),
            truncate(joinSet(result.sources), 30)
        );

        // 전체 소스 테이블 누적
        allSourceTables.addAll(result.sources);
    }

    // ========== 파일 분석 ==========
    private static FileAnalysisResult analyzeFile(File file) {
        FileAnalysisResult result = new FileAnalysisResult();
        StringBuilder content = new StringBuilder();

        // 1. 파일 읽기 및 기본 카운트
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.lineCount++;

                if (line.toUpperCase().contains("INSERT")) {
                    result.insertCount++;
                }

                if (STEP_PATTERN.matcher(line).find()) {
                    result.stepCount++;
                }

                content.append(line).append('\n');
            }
        } catch (IOException e) {
            System.err.println("파일 읽기 오류: " + file.getName() + " - " + e.getMessage());
            return null;
        }

        if (content.length() == 0) return result;

        try {
            // 2. 주석 제거
            String sql = removeComments(content.toString());

            // 3. CTE 이름 추출
            Set<String> cteNames = extractCTENames(sql);

            // 4. 테이블 추출
            extractTargetTables(sql, result.targets);
            extractSourceTables(sql, cteNames, result.sources);

        } catch (Exception e) {
            System.err.println("파일 분석 오류: " + file.getName() + " - " + e.getMessage());
        }

        return result;
    }

    // ========== 주석 제거 ==========
    private static String removeComments(String sql) {
        // 라인 주석 제거
        sql = LINE_COMMENT.matcher(sql).replaceAll("");
        // 블록 주석 제거 (크기 제한으로 안전하게)
        if (sql.length() < 500000) { // 500KB 이하만 정규식 사용
            sql = BLOCK_COMMENT.matcher(sql).replaceAll("");
        }
        return sql;
    }

    // ========== CTE 이름 추출 ==========
    private static Set<String> extractCTENames(String sql) {
        Set<String> names = new HashSet<>();
        // 앞부분만 스캔 (CTE는 보통 앞에 위치)
        String scanPart = sql.length() > 100000 ? sql.substring(0, 100000) : sql;

        Matcher m = CTE_PATTERN.matcher(scanPart);
        while (m.find()) {
            String name = normalizeTableName(m.group(1));
            names.add(name);
        }
        return names;
    }

    // ========== 타겟 테이블 추출 ==========
    private static void extractTargetTables(String sql, Set<String> targets) {
        extractTables(sql, INSERT_PATTERN, targets);
        extractTables(sql, UPDATE_PATTERN, targets);
        extractTables(sql, DELETE_PATTERN, targets);
        extractTables(sql, MERGE_PATTERN, targets);
    }

    // ========== 소스 테이블 추출 ==========
    private static void extractSourceTables(String sql, Set<String> cteNames, Set<String> sources) {
        Set<String> candidates = new HashSet<>();
        extractTables(sql, FROM_JOIN_PATTERN, candidates);
        extractTables(sql, USING_PATTERN, candidates);

        // 필터링
        for (String table : candidates) {
            if (shouldIncludeSource(table, cteNames)) {
                sources.add(table);
            }
        }
    }

    // ========== 테이블 추출 (공통) ==========
    private static void extractTables(String sql, Pattern pattern, Set<String> tables) {
        try {
            Matcher m = pattern.matcher(sql);
            while (m.find()) {
                String raw = m.group(1);
                if (raw != null && !raw.isEmpty()) {
                    String normalized = normalizeTableName(raw);
                    tables.add(normalized);
                }
            }
        } catch (StackOverflowError e) {
            // 정규식 오버플로우 시 안전하게 무시
            System.err.println("정규식 오버플로우 발생 (건너뜀)");
        }
    }

    // ========== 소스 테이블 필터링 ==========
    private static boolean shouldIncludeSource(String table, Set<String> cteNames) {
        // DUAL 제외
        if ("DUAL".equalsIgnoreCase(table)) return false;

        // CTE 제외
        if (cteNames.contains(table)) return false;

        // WH_ 접두어 제외
        if (table.startsWith("WH_")) return false;

        // 점이 있으면 포함 (스키마.테이블)
        if (table.contains(".")) return true;

        // 화이트리스트에 있으면 포함
        return SOURCE_WHITELIST.contains(table);
    }

    // ========== 테이블명 정규화 ==========
    private static String normalizeTableName(String name) {
        if (name == null) return "";

        // trim 및 따옴표 제거
        name = name.trim().replace("\"", "");

        // 첫 토큰만 (별칭 제거)
        int space = name.indexOf(' ');
        if (space > 0) {
            name = name.substring(0, space);
        }

        // 대문자화
        return name.toUpperCase();
    }

    // ========== 유틸리티 ==========
    private static String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx <= 0 || idx == fileName.length() - 1) {
            return "noext";
        }
        return fileName.substring(idx + 1).toLowerCase();
    }

    private static void incrementExtCount(String ext) {
        extCountMap.put(ext, extCountMap.getOrDefault(ext, 0) + 1);
    }

    private static String joinSet(Set<String> set) {
        if (set.isEmpty()) return "-";
        return String.join("^", set);
    }

    private static String truncate(String str, int maxLen) {
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }

    private static void printExtensionSummary() {
        if (extCountMap.isEmpty()) {
            System.out.println("처리된 파일이 없습니다.");
            return;
        }
        for (Map.Entry<String, Integer> entry : extCountMap.entrySet()) {
            System.out.printf("확장자: %-6s | 파일수: %4d%n", entry.getKey(), entry.getValue());
        }
    }

    private static void printAllSourceTables() {
        if (allSourceTables.isEmpty()) {
            System.out.println("소스 테이블이 없습니다.");
            return;
        }
        System.out.println("전체 소스 테이블 수: " + allSourceTables.size());
        for (String table : allSourceTables) {
            System.out.println(table);
        }
    }

    // ========== 내부 클래스 ==========
    private static class FileAnalysisResult {
        int lineCount = 0;
        int insertCount = 0;
        int stepCount = 0;
        Set<String> targets = new LinkedHashSet<>();
        Set<String> sources = new LinkedHashSet<>();
    }
}

