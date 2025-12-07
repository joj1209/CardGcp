package service.scanSourceTarget.analyze.BigQuery;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class BigQueryScriptAnalyzer3 {

    private static final String OUTPUT_CSV = "output.csv";
    private static final String OUTPUT_HTML = "output.html";

    public static void main(String[] args) {
        String filePath = "D:\\11. Project\\11. DB\\BigQuery\\sample_script.sql"; // ← 필요시 사용자로부터 입력받게 수정 가능

        try {
            String script = readFile(filePath);
            List<StepBlock> steps = extractNestedStepBlocks(script);

            List<AnalysisResult> results = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (StepBlock step : steps) {
                System.out.println("==== " + step.stepName + " 분석 결과 ====");

                Set<String> tables = extractTableNames(step.content);
                Set<String> columns = extractColumnNames(step.content);
                Set<String> conditions = extractConditions(step.content);
                Map<String, String> aliases = extractTableAliases(step.content);

                List<String> stepErrors = detectErrors(step.content);
                errors.addAll(stepErrors);

                AnalysisResult result = new AnalysisResult(
                        step.stepName, tables, columns, conditions, aliases, stepErrors);
                results.add(result);

                result.printToConsole();
            }

            // CSV 및 HTML 리포트 생성
            writeResultsToCSV(results);
            writeResultsToHTML(results, errors);
            System.out.println("📁 결과 저장 완료: output.csv, output.html");

        } catch (IOException e) {
            System.err.println("❗ 파일 오류: " + e.getMessage());
        }
    }

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

    private static List<StepBlock> extractNestedStepBlocks(String script) {
        List<StepBlock> blocks = new ArrayList<>();
        Pattern stepPattern = Pattern.compile("(STEP\\d{3})\\s*BEGIN", Pattern.CASE_INSENSITIVE);
        Matcher matcher = stepPattern.matcher(script);

        while (matcher.find()) {
            String stepName = matcher.group(1);
            int beginIndex = matcher.end();

            int endIndex = findMatchingEnd(script, beginIndex);
            if (endIndex == -1) continue;

            String content = script.substring(beginIndex, endIndex).trim();
            blocks.add(new StepBlock(stepName, content));
        }

        return blocks;
    }

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
            } else break;
        }
        return -1;
    }

    private static Set<String> extractTableNames(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("\\b(FROM|JOIN|INTO|UPDATE|MERGE INTO|RENAME TO)\\s+([`\\w\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            tables.add(matcher.group(2).replaceAll("[`\"']", ""));
        }
        return tables;
    }

    private static Set<String> extractColumnNames(String sql) {
        Set<String> columns = new LinkedHashSet<>();
        Pattern selectPattern = Pattern.compile("SELECT\\s+(.*?)\\s+FROM", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = selectPattern.matcher(sql);
        if (matcher.find()) {
            String[] cols = matcher.group(1).split(",");
            for (String col : cols) {
                columns.add(col.trim().replaceAll("[`\"']", ""));
            }
        }

        Pattern setPattern = Pattern.compile("SET\\s+(.*?)\\s*(WHERE|;|\\n)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        matcher = setPattern.matcher(sql);
        if (matcher.find()) {
            String[] cols = matcher.group(1).split(",");
            for (String col : cols) {
                columns.add(col.trim().split("=")[0].trim());
            }
        }

        return columns;
    }

    private static Set<String> extractConditions(String sql) {
        Set<String> conds = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("\\b(WHERE|ON)\\b\\s+(.*?)(\\bGROUP BY\\b|\\bORDER BY\\b|;|\\n|\\bWHEN\\b)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            conds.add(matcher.group(2).replaceAll("[\\n\\t]+", " ").trim());
        }
        return conds;
    }

    private static Map<String, String> extractTableAliases(String sql) {
        Map<String, String> aliasMap = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("(\\w+(?:\\.\\w+)*)\\s+AS\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            aliasMap.put(matcher.group(2), matcher.group(1));
        }
        return aliasMap;
    }

    private static List<String> detectErrors(String content) {
        List<String> errors = new ArrayList<>();
        if (!content.toLowerCase().contains("select") &&
                !content.toLowerCase().contains("insert") &&
                !content.toLowerCase().contains("merge")) {
            errors.add("쿼리 본문에 SELECT/INSERT/MERGE 없음");
        }

        if (content.split("BEGIN", -1).length != content.split("END", -1).length) {
            errors.add("BEGIN-END 블록 수 불일치");
        }

        if (!content.trim().endsWith(";")) {
            errors.add("END 구문 뒤에 세미콜론 없음");
        }

        return errors;
    }

    private static void writeResultsToCSV(List<AnalysisResult> results) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_CSV));
        writer.println("Step,Table,Column,Condition,Alias->Original,Error");

        for (AnalysisResult r : results) {
            int size = Math.max(
                    Math.max(r.tables.size(), r.columns.size()),
                    Math.max(r.conditions.size(), r.aliases.size())
            );
            size = Math.max(size, r.errors.size());

            List<String> tables = new ArrayList<>(r.tables);
            List<String> columns = new ArrayList<>(r.columns);
            List<String> conditions = new ArrayList<>(r.conditions);
            List<String> aliases = new ArrayList<>();
            for (Map.Entry<String, String> e : r.aliases.entrySet()) {
                aliases.add(e.getKey() + " -> " + e.getValue());
            }

            for (int i = 0; i < size; i++) {
                String table = i < tables.size() ? tables.get(i) : "";
                String column = i < columns.size() ? columns.get(i) : "";
                String cond = i < conditions.size() ? conditions.get(i) : "";
                String alias = i < aliases.size() ? aliases.get(i) : "";
                String error = i < r.errors.size() ? r.errors.get(i) : "";

                writer.printf("%s,%s,%s,%s,%s,%s\n", r.stepName, table, column, cond, alias, error);
            }
        }

        writer.close();
    }

    private static void writeResultsToHTML(List<AnalysisResult> results, List<String> allErrors) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_HTML));

        writer.println("<html><head><meta charset='UTF-8'><title>분석 결과</title></head><body>");
        writer.println("<h1>BigQuery 분석 리포트</h1>");

        // 요약
        writer.println("<h2>요약</h2><ul>");
        writer.println("<li>STEP 수: " + results.size() + "</li>");
        int totalTables = results.stream().mapToInt(r -> r.tables.size()).sum();
        int totalColumns = results.stream().mapToInt(r -> r.columns.size()).sum();
        int totalConditions = results.stream().mapToInt(r -> r.conditions.size()).sum();
        int totalErrors = allErrors.size();
        writer.println("<li>총 테이블 수: " + totalTables + "</li>");
        writer.println("<li>총 컬럼 수: " + totalColumns + "</li>");
        writer.println("<li>총 조건절 수: " + totalConditions + "</li>");
        writer.println("<li>발견된 오류 수: " + totalErrors + "</li>");
        writer.println("</ul>");

        // 상세
        for (AnalysisResult r : results) {
            writer.println("<hr><h3>" + r.stepName + "</h3>");
            writer.println("<b>테이블:</b><ul>");
            for (String t : r.tables) writer.println("<li>" + t + "</li>");
            writer.println("</ul><b>컬럼:</b><ul>");
            for (String c : r.columns) writer.println("<li>" + c + "</li>");
            writer.println("</ul><b>조건:</b><ul>");
            for (String c : r.conditions) writer.println("<li>" + c + "</li>");
            writer.println("</ul><b>테이블 변경 추적:</b><ul>");
            for (Map.Entry<String, String> e : r.aliases.entrySet())
                writer.println("<li>" + e.getKey() + " → " + e.getValue() + "</li>");
            writer.println("</ul><b>오류:</b><ul>");
            for (String e : r.errors) writer.println("<li style='color:red;'>" + e + "</li>");
            writer.println("</ul>");
        }

        writer.println("</body></html>");
        writer.close();
    }

    /**
     * STEP 블록 정보 구조체
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
        List<String> errors;

        AnalysisResult(String stepName, Set<String> tables, Set<String> columns,
                       Set<String> conditions, Map<String, String> aliases, List<String> errors) {
            this.stepName = stepName;
            this.tables = tables;
            this.columns = columns;
            this.conditions = conditions;
            this.aliases = aliases;
            this.errors = errors;
        }

        void printToConsole() {
            System.out.println("✔ STEP: " + stepName);
            System.out.println("📦 테이블: " + tables);
            System.out.println("📌 컬럼: " + columns);
            System.out.println("🔍 조건: " + conditions);
            System.out.println("🔄 변경 추적: " + aliases);
            if (!errors.isEmpty()) {
                System.out.println("❗ 오류:");
                for (String e : errors) System.out.println(" - " + e);
            }
        }
    }
}

