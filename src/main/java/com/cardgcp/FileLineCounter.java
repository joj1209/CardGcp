package com.cardgcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * FileLineCounter (Java 7 호환)
 *
 * 기능 요약
 *  - 폴더(재귀) 내 파일을 읽어:
 *    1) 전체 라인수
 *    2) "INSERT" 포함 라인수
 *    3) "vs_jb_step +1 ;" 포함 라인수 (STEP)
 *    4) 확장자별 파일 건수 요약
 *    5) 파일별 타겟 테이블 / 소스 테이블 추출
 *       - 타겟: INSERT INTO, UPDATE ... SET, DELETE FROM, MERGE INTO
 *       - 소스: FROM/JOIN, MERGE USING
 *    6) 추출 보정/필터
 *       - 주석 제거(--, /* *)
        *       - CTE 이름(WITH name AS (...))은 소스에서 제외
 *       - DUAL 소스 제외
 *       - 소스 중 WH_ 접두어 제외 (WH_*)
 *       - 소스는 원칙적으로 스키마.테이블(점 포함)만 인정,
 *         단 점이 없더라도 화이트리스트에 있으면 포함 (예: DBA_TABLE, DBA_TAB_COLUMNS)
 *       - 스키마는 절대 무시하지 않고 보존 (중복判定 및 출력)
 *       - 한글/유니코드 식별자 허용 (\p{L}\p{N})
        *       - Pro*C 형태: "INSERT ...""INTO 스키마.테이블" 처럼 문자열 나뉨도 인식
 *
         * 출력
 *  - 파일: <파일명> | 라인수 | INSERT | STEP | 확장자 | 타겟: A^B | 소스: X^Y
 *  - 확장자별 요약
 *  - 전체 소스 테이블(Distinct, 스키마 보존) 목록
 */
public class FileLineCounter {

    /* ============== 설정값 ============== */

    // 기본 탐색 폴더 (실행 시 인자로 넘기면 그 값 사용)
    private static final String DEFAULT_DIR = "D:\\11. Project\\11. DB";

    // 소스 테이블 화이트리스트 (점이 없어도 허용할 이름들) — 반드시 대문자로 기입
    private static final Set<String> SOURCE_WHITELIST = new LinkedHashSet<String>();
    static {
        SOURCE_WHITELIST.add("DBA_TABLE");
        SOURCE_WHITELIST.add("DBA_TAB_COLUMNS");
        // 필요 시 추가: SOURCE_WHITELIST.add("USER_TAB_COLUMNS");
        // 필요 시 추가: SOURCE_WHITELIST.add("V$SESSION");
    }

    /* ============== 집계용 컨테이너 ============== */

    // 확장자별 파일 수 요약
    private static final Map<String, Integer> extCountMap = new LinkedHashMap<String, Integer>();
    // 전체 소스 테이블(스키마 보존) distinct
    private static final Set<String> allSourceTables = new LinkedHashSet<String>();

    /* ============== 정규식 ============== */

    // STEP 키워드: "vs_jb_step +1 ;" (대소문자 무시, 공백 유연)
    private static final Pattern STEP_PATTERN =
            Pattern.compile("(?i)vs_jb_step\\s*\\+1\\s*;");

    // 주석 제거용
    private static final Pattern LINE_COMMENT = Pattern.compile("--.*?$", Pattern.MULTILINE);
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    // CTE 이름: WITH cte AS ( ... )
    private static final Pattern CTE_NAME_PATTERN =
            Pattern.compile("(?i)\\bwith\\s+([\\p{L}\\p{N}_\"$#]+)\\s+as\\s*\\(");

    // CTE 스캔 한도 (정규식이 긴 입력에서 문제를 피하기 위해 앞부분만 스캔)
    private static final int CTE_SCAN_LIMIT = 200 * 1024; // 200KB

 // 테이블명 토큰 (DB 링크 없음)
 // 라틴/한글 등 유니코드 문자/숫자 + _, "$", "#", "." 허용 (스키마.테이블, "따옴표식별자" 포함)
     private static final String TABLE_NAME = "([\\p{L}\\p{N}_\"$#\\.]+)";

    // 소스 후보 (읽기): FROM/JOIN, MERGE USING
    private static final Pattern FROM_JOIN_TABLE_PATTERN =
            Pattern.compile("(?i)\\b(from|join)\\s+" + TABLE_NAME);
    private static final Pattern MERGE_USING_TABLE_PATTERN =
            Pattern.compile("(?i)\\busing\\s+" + TABLE_NAME);

    // 타겟 후보 (쓰기/변경)
    // INSERT — Pro*C 문자열 분할(따옴표/개행/힌트 포함)까지 대응
    //  예: "INSERT /*+APPEND */"  "INTO DM.TABLE_A"
    private static final Pattern INSERT_TABLE_PATTERN =
            Pattern.compile(
                    "(?i)(?:\\b|\\\")insert(?:\\b|\\\")"   // "INSERT", INSERT 모두 허용
                            + "(?:.|\\s)*?"                          // 힌트/공백/개행/따옴표 등 유연 매칭
                            + "\\binto\\b\\s+"                       // INTO
                            + TABLE_NAME                             // 테이블명 캡처
                    , Pattern.DOTALL);

    // UPDATE — 테이블명 뒤에 (옵션)별칭 + SET 이 따라야 함을 전방탐색으로 보장
    private static final Pattern UPDATE_TABLE_PATTERN =
            Pattern.compile(
                    "(?i)\\bupdate\\b(?:/\\*.*?\\*/|\\s)+"
                            + TABLE_NAME
                            + "(?=\\s+(?:[\\p{L}\\p{N}_\"$#]+\\s+)?set\\b)"
                    , Pattern.DOTALL);

    // DELETE FROM
    private static final Pattern DELETE_TABLE_PATTERN =
            Pattern.compile("(?i)\\bdelete\\b(?:/\\*.*?\\*/|\\s)+from\\s+" + TABLE_NAME, Pattern.DOTALL);

    // MERGE INTO
    private static final Pattern MERGE_INTO_TABLE_PATTERN =
            Pattern.compile("(?i)\\bmerge\\b(?:/\\*.*?\\*/|\\s)+into\\s+" + TABLE_NAME, Pattern.DOTALL);

    // 최대 SQL 길이(문자수). 이보다 큰 파일은 일부만 분석하여 regex로 인한 StackOverflow를 방지
    private static final int MAX_SQL_LENGTH = 2 * 1024 * 1024; // 2MB
    // 안전하게 정규식을 수행할 최대 길이 (이보다 길면 스캐너 기반 추출을 사용)
    private static final int SAFE_REGEX_LIMIT = 200 * 1024; // 200KB
    // 스캐너 윈도우 크기 (키워드 주변을 이 길이만큼 살펴봄)
    private static final int SCANNER_WINDOW_SIZE = 2000;
    // 키워드 탐색 시 건너뛸 최소 크기
    private static final int KEYWORD_SKIP_SIZE = 6;


    /* ============== 메인 ============== */

    public static void main(String[] args) {
        String folderPath = args.length > 0 ? args[0] : DEFAULT_DIR;
        File root = new File(folderPath);

        if (!root.exists() || !root.isDirectory()) {
            System.err.println("지정한 경로가 폴더가 아닙니다: " + folderPath);
            return;
        }

        System.out.println("===== 파일명 | 라인수 | INSERT | STEP | 확장자 | 타겟 | 소스 =====");
        walk(root);

        System.out.println("\n===== 확장자별 파일 건수 요약 =====");
        printExtSummary();

        System.out.println("\n===== 전체 소스 테이블 (Distinct, 스키마 보존) =====");
        printAllSourceTables();
    }

    /* ============== 탐색/처리 ============== */

    // 폴더 재귀 탐색
    private static void walk(File dir) {
        File[] list = dir.listFiles();
        if (list == null) return;

        for (int i = 0; i < list.length; i++) {
            File f = list[i];
            if (f.isDirectory()) {
                walk(f);
            } else {
                processFile(f);
            }
        }
    }

    // 파일 1개 처리
    private static void processFile(File file) {
        FileAnalysisResult result = analyzeFile(file);
        if (result == null) return; // 파일 읽기 오류

         // 확장자 집계
         String fileName = file.getName();
         String ext = getExtensionSafe(fileName);
        incrementExtCount(ext);

         // 파일별 출력 (다중 테이블은 ^ 구분)
        Set<String> tgtView = normalizeSet(result.targets);
        Set<String> srcView = normalizeSet(result.sources);

        System.out.printf(
                "파일: %-20s | 라인수: %6d | INSERT: %4d | STEP: %4d | 확장자: %-5s | 타겟: %s | 소스: %s%n",
                fileName,
                result.lineCount,
                result.insertLineCount,
                result.stepCount,
                ext,
                tgtView.isEmpty() ? "-" : joinWithCaret(tgtView),
                srcView.isEmpty() ? "-" : joinWithCaret(srcView)
        );

        // 전체 소스 테이블(Distinct, 스키마 보존) 누적
        for (String src : result.sources) {
             allSourceTables.add(toDistinctKeyKeepSchema(src));
         }
    }

    /**
     * 파일 분석 결과를 담는 내부 클래스
     */
    private static class FileAnalysisResult {
        int lineCount;
        int insertLineCount;
        int stepCount;
        Set<String> targets;
        Set<String> sources;

        FileAnalysisResult() {
            this.targets = new LinkedHashSet<String>();
            this.sources = new LinkedHashSet<String>();
        }
    }

    /**
     * 파일을 읽고 분석
     */
    private static FileAnalysisResult analyzeFile(File file) {
        FileAnalysisResult result = new FileAnalysisResult();

        // 1. 파일 읽기 및 라인 카운트
        StringBuilder sqlBuilder = new StringBuilder();
        boolean truncated = readFileContent(file, result, sqlBuilder);
        if (result.lineCount == 0 && sqlBuilder.length() == 0) {
            return null; // 오류 발생
        }

        // 2. SQL 전처리 (주석 제거)
        String sql = preprocessSQL(sqlBuilder.toString(), file, truncated);
        if (sql.isEmpty()) return result;

        // 3. CTE 이름 수집
        Set<String> cteNames = extractCTENames(sql, file);

        // 4. 테이블 추출
        extractTables(sql, cteNames, result, file);

        return result;
    }

    /**
     * 파일 내용을 읽고 라인별 통계 수집
     */
    private static boolean readFileContent(File file, FileAnalysisResult result, StringBuilder sqlBuilder) {
        BufferedReader br = null;
        boolean truncated = false;

        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                result.lineCount++;

                if (line.toUpperCase().contains("INSERT")) {
                    result.insertLineCount++;
                }

                if (STEP_PATTERN.matcher(line).find()) {
                    result.stepCount++;
                }

                if (!truncated) {
                    sqlBuilder.append(line).append('\n');
                    if (sqlBuilder.length() > MAX_SQL_LENGTH) {
                        sqlBuilder.setLength(MAX_SQL_LENGTH);
                        truncated = true;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("파일 읽기 오류: " + file.getAbsolutePath());
            return false;
        } finally {
            closeQuietly(br);
        }

        return truncated;
    }

    /**
     * SQL 전처리 (주석 제거)
     */
    private static String preprocessSQL(String sql, File file, boolean truncated) {
        try {
            return stripComments(sql);
        } catch (StackOverflowError err) {
            System.err.println("[경고] 주석 제거 중 StackOverflow 발생, 파일 건너뜀: " + file.getAbsolutePath());
            return "";
        }
    }

    /**
     * CTE 이름 추출
     */
    private static Set<String> extractCTENames(String sql, File file) {
        try {
            return collectCTENames(sql);
        } catch (StackOverflowError err) {
            System.err.println("[경고] CTE 추출 중 StackOverflow 발생, 파일: " + file.getAbsolutePath());
            return new HashSet<String>();
        }
    }

    /**
     * 테이블 추출 (타겟 + 소스)
     */
    private static void extractTables(String sql, Set<String> cteNames, FileAnalysisResult result, File file) {
        try {
            collectTargets(sql, result.targets);
            collectSources(sql, cteNames, result.sources);
        } catch (StackOverflowError err) {
            System.err.println("[경고] 테이블 추출 중 StackOverflow 발생, 파일: " + file.getAbsolutePath());
            result.targets.clear();
            result.sources.clear();
        }
    }

    /**
     * Set을 normalize하여 새로운 Set 반환
     */
    private static Set<String> normalizeSet(Set<String> input) {
        Set<String> result = new LinkedHashSet<String>();
        for (String item : input) {
            result.add(toDistinctKeyKeepSchema(item));
        }
        return result;
    }

    /**
     * 확장자 카운트 증가
     */
    private static void incrementExtCount(String ext) {
        Integer old = extCountMap.get(ext);
        extCountMap.put(ext, old == null ? 1 : old + 1);
    }

    /* ============== 테이블 추출 로직 ============== */

    private static void collectTargets(String sql, Set<String> outTargets) {
        safeExtractTables(sql, INSERT_TABLE_PATTERN, 1, null, outTargets, true, "INSERT");
        safeExtractTables(sql, UPDATE_TABLE_PATTERN, 1, null, outTargets, true, "UPDATE");
        safeExtractTables(sql, DELETE_TABLE_PATTERN, 1, null, outTargets, true, "DELETE");
        safeExtractTables(sql, MERGE_INTO_TABLE_PATTERN, 1, null, outTargets, true, "MERGE INTO");
    }

    private static void collectSources(String sql, Set<String> cteNames, Set<String> outSources) {
        safeExtractTables(sql, FROM_JOIN_TABLE_PATTERN, 2, cteNames, outSources, false, "FROM/JOIN");
        safeExtractTables(sql, MERGE_USING_TABLE_PATTERN, 1, cteNames, outSources, false, "MERGE USING");
    }

    /**
     * StackOverflow로부터 안전하게 테이블을 추출하는 헬퍼 메서드
     */
    private static void safeExtractTables(String sql, Pattern pattern, int groupIdx,
                                          Set<String> cteNames, Set<String> out,
                                          boolean isTarget, String patternName) {
        try {
            findTablesWithPattern(sql, pattern, groupIdx, cteNames, out, isTarget);
        } catch (StackOverflowError err) {
            System.err.println("[경고] " + patternName + " 테이블 추출 중 StackOverflow 발생, 해당 패턴 건너뜀");
        }
    }

    /**
     * 공통 테이블 추출기
     *
     * @param sql       분석할 SQL 텍스트(주석 제거 후)
     * @param p         사용할 정규식 패턴
     * @param groupIdx  테이블명 캡처 그룹 인덱스
     * @param cteNames  CTE 이름 집합(소스에서 제외 처리용)
     * @param out       결과를 넣을 Set
     * @param isTarget  true=타겟 추출, false=소스 추출
     */
    private static void findTablesWithPattern(
            String sql, Pattern p, int groupIdx,
            Set<String> cteNames, Set<String> out, boolean isTarget) {

        // 큰 입력에 대해선 복잡한 정규식 대신 안전한 스캐너 기반 추출을 사용
        // 또한 INSERT 패턴은 입력에 따라 catastrophic backtracking을 일으키는 경우가 있어
        // 항상 스캐너로 처리하도록 한다.
        if (sql.length() > SAFE_REGEX_LIMIT || p == INSERT_TABLE_PATTERN) {
            findTablesWithScanner(sql, p, groupIdx, cteNames, out, isTarget);
            return;
        }

        Matcher m = p.matcher(sql);
        while (m.find()) {
            String raw = m.group(groupIdx);
            if (raw == null) continue;

            // 별칭 제거 위해 첫 토큰만 취득 (예: DM.TABLE_A N10 -> DM.TABLE_A)
            String first = firstToken(raw);
            if (first.length() == 0) continue;

            // 표준화 (따옴표 제거, 대문자화, 스키마는 보존)
            String norm = normalizeId(first);

            if (!isTarget) {
                // 소스 필터: DUAL / CTE 제외
                if ("DUAL".equalsIgnoreCase(norm)) continue;
                if (cteNames != null && cteNames.contains(norm)) continue;
                // 소스 필터: WH_ 접두어 제외
                if (norm.startsWith("WH_")) continue;
                // 소스 필터: 스키마.테이블(점 포함)만 인정,
                //           단 점이 없어도 화이트리스트면 포함
                boolean hasDot = norm.indexOf('.') >= 0;
                if (!hasDot && !SOURCE_WHITELIST.contains(norm)) {
                    continue; // 소스에서 제외
                }
            }

            out.add(norm);
        }
    }

    // 큰 SQL에 대해 정규식을 직접 적용하지 않고 작은 윈도우에서 토큰을 추출하는 안전 스캐너
    private static void findTablesWithScanner(
            String sql, Pattern p, int groupIdx,
            Set<String> cteNames, Set<String> out, boolean isTarget) {
        String lower = sql.toLowerCase();
        int pos = 0;
        final int WINDOW = SCANNER_WINDOW_SIZE; // 키워드 주변을 이 길이만큼 살펴봄

        while (pos < lower.length()) {
            int idx = findNextKeywordIndex(lower, pos, p);
            if (idx == -1) break;

            int end = Math.min(lower.length(), idx + WINDOW);
            String window = sql.substring(idx, end); // 원본 케이스 유지

            try {
                extractTableFromWindow(window, p, cteNames, out, isTarget);
            } catch (Throwable t) {
                // 안전하게 무시하고 다음 위치로 진행
            }

            pos = idx + 1;
        }
    }

    /**
     * 키워드의 다음 등장 위치를 찾음
     */
    private static int findNextKeywordIndex(String lower, int pos, Pattern p) {
        if (p == INSERT_TABLE_PATTERN) return lower.indexOf("insert", pos);
        if (p == UPDATE_TABLE_PATTERN) return lower.indexOf("update", pos);
        if (p == DELETE_TABLE_PATTERN) return lower.indexOf("delete", pos);
        if (p == MERGE_INTO_TABLE_PATTERN) return lower.indexOf("merge", pos);
        if (p == FROM_JOIN_TABLE_PATTERN) {
            int i1 = lower.indexOf(" from ", pos);
            int i2 = lower.indexOf(" join ", pos);
            if (i1 == -1) return i2;
            if (i2 == -1) return i1;
            return Math.min(i1, i2);
        }
        if (p == MERGE_USING_TABLE_PATTERN) return lower.indexOf("using", pos);
        return -1;
    }

    /**
     * 윈도우에서 테이블명을 추출
     */
    private static void extractTableFromWindow(String window, Pattern p,
                                               Set<String> cteNames, Set<String> out, boolean isTarget) {
        if (p == INSERT_TABLE_PATTERN) {
            extractWithPattern(window, "(?i)\\binto\\b\\s+([\\p{L}\\p{N}_\"$#\\.]+)", 1, cteNames, out, isTarget);
        } else if (p == UPDATE_TABLE_PATTERN) {
            extractWithPattern(window, "(?i)\\bupdate\\b\\s*([\\p{L}\\p{N}_\"$#\\.]+)\\s+(?:[\\p{L}\\p{N}_\"$#]+\\s+)?set\\b", 1, cteNames, out, isTarget);
        } else if (p == DELETE_TABLE_PATTERN) {
            extractWithPattern(window, "(?i)\\bdelete\\b.*?\\bfrom\\b\\s+([\\p{L}\\p{N}_\"$#\\.]+)", 1, cteNames, out, isTarget);
        } else if (p == MERGE_INTO_TABLE_PATTERN) {
            extractWithPattern(window, "(?i)\\bmerge\\b.*?\\binto\\b\\s+([\\p{L}\\p{N}_\"$#\\.]+)", 1, cteNames, out, isTarget);
        } else if (p == FROM_JOIN_TABLE_PATTERN) {
            extractMultipleWithPattern(window, "(?i)\\b(from|join)\\b\\s+([\\p{L}\\p{N}_\"$#\\.]+)", 2, cteNames, out, isTarget);
        } else if (p == MERGE_USING_TABLE_PATTERN) {
            extractWithPattern(window, "(?i)\\busing\\b\\s+([\\p{L}\\p{N}_\"$#\\.]+)", 1, cteNames, out, isTarget);
        }
    }

    /**
     * 패턴으로 단일 테이블명 추출
     */
    private static void extractWithPattern(String text, String regex, int group,
                                           Set<String> cteNames, Set<String> out, boolean isTarget) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (m.find()) {
            String norm = normalizeId(firstToken(m.group(group)));
            if (shouldIncludeTable(norm, cteNames, isTarget)) {
                out.add(norm);
            }
        }
    }

    /**
     * 패턴으로 다중 테이블명 추출 (FROM/JOIN용)
     */
    private static void extractMultipleWithPattern(String text, String regex, int group,
                                                   Set<String> cteNames, Set<String> out, boolean isTarget) {
        Matcher m = Pattern.compile(regex).matcher(text);
        while (m.find()) {
            String norm = normalizeId(firstToken(m.group(group)));
            if (shouldIncludeTable(norm, cteNames, isTarget)) {
                out.add(norm);
            }
        }
    }

    /**
     * 테이블을 결과에 포함할지 판단 (소스 필터링 로직 통합)
     */
    private static boolean shouldIncludeTable(String norm, Set<String> cteNames, boolean isTarget) {
        if (isTarget) return true; // 타겟은 필터링 없음

        // 소스 필터링
        if ("DUAL".equalsIgnoreCase(norm)) return false;
        if (cteNames != null && cteNames.contains(norm)) return false;
        if (norm.startsWith("WH_")) return false;

        boolean hasDot = norm.indexOf('.') >= 0;
        return hasDot || SOURCE_WHITELIST.contains(norm);
    }

    /* ============== 유틸 ============== */

    // SQL 주석 제거: 정규식 대신 선형 스캐너로 처리하여 큰 입력에서의 StackOverflow를 방지
    //  - 문자열 리터럴('' , "") 내부는 주석 토큰으로 처리하지 않음
    private static String stripComments(String sql) {
        if (sql == null || sql.isEmpty()) return "";
        StringBuilder out = new StringBuilder(sql.length());
        int len = sql.length();
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < len; i++) {
            char c = sql.charAt(i);

            // handle end of line comment
            if (inLineComment) {
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                    out.append(c); // keep newline
                }
                continue;
            }

            // handle end of block comment
            if (inBlockComment) {
                if (c == '*' && i + 1 < len && sql.charAt(i + 1) == '/') {
                    inBlockComment = false;
                    i++; // skip '/'
                }
                continue;
            }

            // not in any comment
            // toggle string states
            if (!inSingle && c == '"' && !inDouble) {
                inDouble = true;
                out.append(c);
                continue;
            } else if (inDouble && c == '"') {
                inDouble = false;
                out.append(c);
                continue;
            }

            if (!inDouble && c == '\'' && !inSingle) {
                inSingle = true;
                out.append(c);
                continue;
            } else if (inSingle && c == '\'') {
                inSingle = false;
                out.append(c);
                continue;
            }

            // if inside a string literal, copy chars as-is
            if (inSingle || inDouble) {
                out.append(c);
                continue;
            }

            // detect start of line comment --
            if (c == '-' && i + 1 < len && sql.charAt(i + 1) == '-') {
                inLineComment = true;
                i++; // skip next '-'
                continue;
            }

            // detect start of block comment /*
            if (c == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
                inBlockComment = true;
                i++; // skip '*'
                continue;
            }

            // otherwise copy char
            out.append(c);
        }

        return out.toString();
    }

    // CTE 이름 수집: 큰 입력의 경우 앞부분만 안전하게 스캔하여 정규식 관련 문제 방지
    private static Set<String> collectCTENames(String sql) {
        Set<String> names = new HashSet<String>();
        if (sql == null || sql.isEmpty()) return names;

        String toScan = sql.length() > CTE_SCAN_LIMIT ? sql.substring(0, CTE_SCAN_LIMIT) : sql;
        Matcher m = CTE_NAME_PATTERN.matcher(toScan);
        while (m.find()) {
            String raw = m.group(1);
            String norm = normalizeId(raw);
            names.add(norm);
        }
        return names;
    }

    // 테이블/식별자 정규화
    //  - trim
    //  - 양끝 큰따옴표 제거 ("SCOTT"."EMP" → SCOTT".EMP" ... 은 firstToken에서 정리됨)
    //  - 첫 토큰만 취득(별칭 제거)
    //  - 대문자화 (한글은 영향 없음)
    //  - 보이지 않는 유니코드 공백 보정/꼬리 콤마 제거(안전)
    private static String normalizeId(String id) {
        if (id == null) return "";
        String s = id.trim()
                .replace('\u00A0', ' ')         // NBSP → space
                .replaceAll("\\p{Z}+", " ");     // 유니코드 공백 정규화
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1) {
            s = s.substring(1, s.length() - 1);
        }
        s = firstToken(s);                             // 별칭 제거
        if (s.endsWith(",")) s = s.substring(0, s.length() - 1); // 꼬리 콤마 제거
        return s.toUpperCase();
    }

    // 스키마 보존한 distinct 키 (normalize만 수행)
    private static String toDistinctKeyKeepSchema(String id) {
        return normalizeId(id);
    }

    // 첫 토큰(공백 전까지)
    private static String firstToken(String s) {
        if (s == null) return "";
        s = s.trim();
        int sp = indexOfWhitespace(s);
        if (sp > 0) return s.substring(0, sp);
        return s;
    }

    // 공백 위치 탐색
    private static int indexOfWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }

    // 확장자 안전 추출
    private static String getExtensionSafe(String fileName) {
        if (fileName.startsWith(".") && fileName.indexOf('.', 1) == -1) {
            return "noext";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx <= 0 || idx == fileName.length() - 1) {
            return "noext";
        }
        return fileName.substring(idx + 1).toLowerCase();
    }

    // 세트 → ^ 로 연결
    private static String joinWithCaret(Set<String> set) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String v : set) {
            if (!first) sb.append('^');
            sb.append(v);
            first = false;
        }
        return sb.toString();
    }

    // 확장자 요약 출력
    private static void printExtSummary() {
        if (extCountMap.isEmpty()) {
            System.out.println("처리된 파일이 없습니다.");
            return;
        }
        for (Map.Entry<String, Integer> e : extCountMap.entrySet()) {
            int cnt = e.getValue() == null ? 0 : Math.max(0, e.getValue().intValue());
            System.out.printf("확장자: %-6s | 파일수: %4d%n", e.getKey(), cnt);
        }
    }

    // 전체 소스 테이블(Distinct, 스키마 보존) 출력
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

    // 조용히 닫기
    private static void closeQuietly(BufferedReader br) {
        if (br != null) {
            try { br.close(); } catch (IOException ignore) {}
        }
    }
}
