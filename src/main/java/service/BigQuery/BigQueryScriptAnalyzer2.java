package service.BigQuery;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * BigQueryScriptAnalyzer v2
 * - 중첩 BEGIN-END 지원
 * - 테이블, 컬럼, 조건절 추출
 * - 테이블 변경 추적
 * - 결과 CSV 출력
 */
public class BigQueryScriptAnalyzer2 {

    private static final String OUTPUT_CSV = "output.csv";

    public static void main(String[] args) {
        String filePath = "D:\\11. Project\\11. DB\\BigQuery\\sample_script.sql"; // ← 필요시 사용자로부터 입력받게 수정 가능

        try {
            String script = readFile(filePath);
            List<StepBlock> steps = extractNestedStepBlocks(script);

            List<AnalysisResult> results = new ArrayList<>();

            for (StepBlock step : steps) {
                System.out.println("==== " + step.stepName + " 분석 결과 ====");

                // 테이블 이름 분석
                Set<String> tables = extractTableNames(step.content);

                // 컬럼, 조건절 분석
                Set<String> columns = extractColumnNames(step.content);
                Set<String> conditions = extractConditions(step.content);

                // 테이블 변경 추적
                Map<String, String> tableAliases = extractTableAliases(step.content);

                AnalysisResult result = new AnalysisResult(
                        step.stepName, tables, columns, conditions, tableAliases);
                results.add(result);

                // 콘솔 출력
                result.printToConsole();
            }

            // CSV 저장
            writeResultsToCSV(results);

            System.out.println("✅ 분석 결과가 output.csv 파일에 저장되었습니다.");

        } catch (IOException e) {
            System.err.println("❗ 파일 처리 오류: " + e.getMessage());
        }
    }

    /**
     * 파일 읽기
     */
    private static String readFile(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * 중첩 BEGIN-END 포함 STEP 블록 추출
     */
    private static List<StepBlock> extractNestedStepBlocks(String script) {
        List<StepBlock> blocks = new ArrayList<>();
        Pattern stepStartPattern = Pattern.compile("(STEP\\d{3})\\s*BEGIN", Pattern.CASE_INSENSITIVE);
        Matcher matcher = stepStartPattern.matcher(script);

        while (matcher.find()) {
            String stepName = matcher.group(1);
            int beginIndex = matcher.end();

            int endIndex = findMatchingEnd(script, beginIndex);
            if (endIndex == -1) continue;

            String blockContent = script.substring(beginIndex, endIndex).trim();
            blocks.add(new StepBlock(stepName, blockContent));
        }

        return blocks;
    }

    /**
     * BEGIN-END 중첩 처리를 위한 매칭 END 위치 찾기
     */
    private static int findMatchingEnd(String script, int start) {
        int level = 1;
        int index = start;

        while (index < script.length()) {
            String sub = script.substring(index);
            Matcher begin = Pattern.compile("\\bBEGIN\\b", Pattern.CASE_INSENSITIVE).matcher(sub);
            Matcher end = Pattern.compile("\\bEND\\b", Pattern.CASE_INSENSITIVE).matcher(sub);

            int nextBegin = begin.find() ? begin.start() : Integer.MAX_VALUE;
            int nextEnd = end.find() ? end.start() : Integer.MAX_VALUE;

            if (nextBegin < nextEnd) {
                level++;
                index += nextBegin + 5;
            } else if (nextEnd < nextBegin) {
                level--;
                index += nextEnd + 3;
                if (level == 0) return index;
            } else {
                break;
            }
        }

        return -1;
    }

    /**
     * 테이블 추출 (FROM, JOIN, UPDATE, INSERT INTO, MERGE INTO 등)
     */
    private static Set<String> extractTableNames(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        String regex = "\\b(FROM|JOIN|INTO|UPDATE|MERGE INTO|RENAME TO)\\s+([`\\w\\.]+)";
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sql);
        while (matcher.find()) {
            tables.add(matcher.group(2).replaceAll("[`\"']", ""));
        }
        return tables;
    }

    /**
     * 컬럼명 추출 (SELECT 컬럼들, SET 컬럼들 등)
     */
    private static Set<String> extractColumnNames(String sql) {
        Set<String> columns = new LinkedHashSet<>();
        Pattern selectPattern = Pattern.compile("SELECT\\s+(.*?)\\s+FROM", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = selectPattern.matcher(sql);
        if (matcher.find()) {
            String columnSegment = matcher.group(1);
            for (String col : columnSegment.split(",")) {
                columns.add(col.trim().replaceAll("[`\"']", ""));
            }
        }

        // SET 구문에서도 추출
        Pattern setPattern = Pattern.compile("SET\\s+(.*?)\\s*(WHERE|;|\\n)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        matcher = setPattern.matcher(sql);
        if (matcher.find()) {
            String setSegment = matcher.group(1);
            for (String col : setSegment.split(",")) {
                columns.add(col.trim().split("=")[0].trim());
            }
        }

        return columns;
    }

    /**
     * 조건절 추출 (WHERE, ON)
     */
    private static Set<String> extractConditions(String sql) {
        Set<String> conditions = new LinkedHashSet<>();
        Pattern conditionPattern = Pattern.compile("\\b(WHERE|ON)\\b\\s+(.*?)(\\bGROUP BY\\b|\\bORDER BY\\b|;|\\n|\\bWHEN\\b)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = conditionPattern.matcher(sql);
        while (matcher.find()) {
            String cond = matcher.group(2).trim();
            conditions.add(cond.replaceAll("[\\n\\t]+", " "));
        }
        return conditions;
    }

    /**
     * 테이블 변경 추적 (AS, RENAME TO)
     */
    private static Map<String, String> extractTableAliases(String sql) {
        Map<String, String> aliasMap = new LinkedHashMap<>();
        Pattern asPattern = Pattern.compile("(\\w+(?:\\.\\w+)*)\\s+AS\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = asPattern.matcher(sql);
        while (matcher.find()) {
            aliasMap.put(matcher.group(2), matcher.group(1)); // alias → original
        }
        return aliasMap;
    }

    /**
     * CSV 파일 저장
     */
    private static void writeResultsToCSV(List<AnalysisResult> results) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_CSV));

        // Header
        writer.println("Step,Table,Column,Condition,Alias->Original");

        for (AnalysisResult result : results) {
            int maxSize = Math.max(
                    Math.max(result.tables.size(), result.columns.size()),
                    Math.max(result.conditions.size(), result.aliases.size())
            );

            List<String> tableList = new ArrayList<>(result.tables);
            List<String> columnList = new ArrayList<>(result.columns);
            List<String> conditionList = new ArrayList<>(result.conditions);
            List<String> aliasList = new ArrayList<>();

            for (Map.Entry<String, String> entry : result.aliases.entrySet()) {
                aliasList.add(entry.getKey() + " -> " + entry.getValue());
            }

            for (int i = 0; i < maxSize; i++) {
                String table = i < tableList.size() ? tableList.get(i) : "";
                String column = i < columnList.size() ? columnList.get(i) : "";
                String cond = i < conditionList.size() ? conditionList.get(i) : "";
                String alias = i < aliasList.size() ? aliasList.get(i) : "";

                writer.printf("%s,%s,%s,%s,%s\n",
                        result.stepName, table, column, cond, alias);
            }
        }

        writer.close();
    }

    /**
     * STEP 블록을 나타내는 구조체 클래스
     */
    static class StepBlock {
        String stepName;
        String content;

        StepBlock(String stepName, String content) {
            this.stepName = stepName;
            this.content = content;
        }
    }

    /**
     * 분석 결과 구조체
     */
    static class AnalysisResult {
        String stepName;
        Set<String> tables;
        Set<String> columns;
        Set<String> conditions;
        Map<String, String> aliases;

        AnalysisResult(String stepName, Set<String> tables, Set<String> columns,
                       Set<String> conditions, Map<String, String> aliases) {
            this.stepName = stepName;
            this.tables = tables;
            this.columns = columns;
            this.conditions = conditions;
            this.aliases = aliases;
        }

        void printToConsole() {
            System.out.println("✔️ STEP: " + stepName);
            System.out.println("📦 테이블:");
            for (String t : tables) System.out.println(" - " + t);
            System.out.println("📌 컬럼:");
            for (String c : columns) System.out.println(" - " + c);
            System.out.println("🔍 조건절:");
            for (String cond : conditions) System.out.println(" - " + cond);
            System.out.println("🔄 변경 추적:");
            for (Map.Entry<String, String> e : aliases.entrySet()) {
                System.out.println(" - " + e.getKey() + " → " + e.getValue());
            }
        }
    }
}

