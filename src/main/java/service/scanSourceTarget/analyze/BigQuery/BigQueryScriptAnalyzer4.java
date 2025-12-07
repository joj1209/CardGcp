package service.scanSourceTarget.analyze.BigQuery;

// BigQueryScriptAnalyzer.java

// 위에 있는 기존 import 유지
import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class BigQueryScriptAnalyzer4 {

    private static final String DEFAULT_CSV = "output.csv";
    private static final String DEFAULT_HTML = "output.html";

    public static void main(String[] args) {
        AnalyzerConfig config;
        try {
            config = AnalyzerConfig.fromArgs(args);
        } catch (IllegalArgumentException ex) {
            System.err.println("❗ " + ex.getMessage());
            printUsage();
            return;
        }

        if (config.showHelp && !config.launchGui) {
            printUsage();
            return;
        }

        if (config.launchGui) {
            AnalyzerFxApp.bootstrap(config);
            return;
        }

        if (config.inputPath == null) {
            printUsage();
            return;
        }

        runCli(config);
    }

    private static void runCli(AnalyzerConfig config) {
        try {
            AnalysisSession session = analyze(config.inputPath);
            if (session.results.isEmpty()) {
                System.out.println("분석 가능한 STEP 블록이 없습니다.");
                return;
            }

            if (config.writeCsv) {
                writeResultsToCSV(session.results, config.csvPath);
                System.out.println("CSV 저장 완료: " + config.csvPath);
            }

            if (config.writeHtml) {
                writeResultsToHTML(session.results, session.allErrors, config.htmlPath);
                System.out.println("HTML 저장 완료: " + config.htmlPath);
            }

            if (config.printStats) {
                printDuplicateStats(session.results);
            }

            System.out.println(buildSummaryMessage(session));
        } catch (IOException e) {
            System.err.println("파일 오류: " + e.getMessage());
        }
    }

    private static AnalysisSession analyze(String inputPath) throws IOException {
        String script = readFile(inputPath);
        List<StepBlock> steps = extractNestedStepBlocks(script);
        List<AnalysisResult> results = analyzeSteps(steps);
        return new AnalysisSession(results, collectErrors(results));
    }

    private static String buildSummaryMessage(AnalysisSession session) {
        int stepCount = session.results.size();
        int totalTables = session.results.stream().mapToInt(r -> r.tables.size()).sum();
        int totalColumns = session.results.stream().mapToInt(r -> r.columns.size()).sum();
        int totalConditions = session.results.stream().mapToInt(r -> r.conditions.size()).sum();
        return String.format("STEP %d개, 테이블 %d개, 컬럼 %d개, 조건 %d개 분석 완료", stepCount, totalTables, totalColumns, totalConditions);
    }

    private static void printUsage() {
        System.out.println("사용법: java service.BigQuery.BigQueryScriptAnalyzer4 [옵션] <input.sql>");
        System.out.println("옵션:");
        System.out.println("  -h, --help           도움말 표시");
        System.out.println("  -i, --input <path>   입력 SQL 파일 경로");
        System.out.println("      --csv <path>     CSV 출력 경로 (기본: output.csv)");
        System.out.println("      --html <path>    HTML 출력 경로 (기본: output.html)");
        System.out.println("      --no-csv         CSV 저장 생략");
        System.out.println("      --no-html        HTML 저장 생략");
        System.out.println("      --no-stats       중복 통계 출력 생략");
        System.out.println("      --gui            JavaFX GUI 실행");
        System.out.println("예시: java ...BigQueryScriptAnalyzer4 --input sample.sql --csv result.csv");
    }

    private static String readFile(String path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private static List<StepBlock> extractNestedStepBlocks(String script) {
        List<StepBlock> blocks = new ArrayList<>();
        Pattern stepPattern = Pattern.compile("(STEP\\d{3})\\s*BEGIN", Pattern.CASE_INSENSITIVE);
        Matcher matcher = stepPattern.matcher(script);

        while (matcher.find()) {
            String stepName = matcher.group(1);
            int beginIndex = matcher.end();
            int endIndex = findMatchingEnd(script, beginIndex);
            if (endIndex == -1) {
                continue;
            }
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
                if (level == 0) {
                    return index;
                }
            } else {
                break;
            }
        }
        return -1;
    }

    private static List<AnalysisResult> analyzeSteps(List<StepBlock> steps) {
        List<AnalysisResult> results = new ArrayList<>();
        for (StepBlock step : steps) {
            Set<String> tables = extractTableNames(step.content);
            Set<String> columns = extractColumnNames(step.content);
            Set<String> conditions = extractConditions(step.content);
            Map<String, String> aliases = extractTableAliases(step.content);
            List<String> errors = detectErrors(step.content);
            results.add(new AnalysisResult(step.stepName, tables, columns, conditions, aliases, errors));
        }
        return results;
    }

    private static List<String> collectErrors(List<AnalysisResult> results) {
        List<String> allErrors = new ArrayList<>();
        for (AnalysisResult result : results) {
            allErrors.addAll(result.errors);
        }
        return allErrors;
    }

    private static Set<String> extractTableNames(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("\\b(FROM|JOIN|INTO|UPDATE|MERGE\\s+INTO|RENAME\\s+TO)\\s+([`\\w\\.]+)", Pattern.CASE_INSENSITIVE);
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
        while (matcher.find()) {
            String[] cols = matcher.group(1).split(",");
            for (String col : cols) {
                String cleaned = col.trim();
                if (!cleaned.isEmpty()) {
                    columns.add(cleaned.replaceAll("[`\"']", ""));
                }
            }
        }

        Pattern setPattern = Pattern.compile("SET\\s+(.*?)\\s*(WHERE|;|\\n)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        matcher = setPattern.matcher(sql);
        while (matcher.find()) {
            String[] cols = matcher.group(1).split(",");
            for (String col : cols) {
                String[] parts = col.split("=");
                if (parts.length > 0) {
                    columns.add(parts[0].trim());
                }
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
        Pattern pattern = Pattern.compile("(\\w+(?:\\.\\w+)*)\\s+(?:AS\\s+)?(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            aliasMap.put(matcher.group(2), matcher.group(1));
        }
        return aliasMap;
    }

    private static List<String> detectErrors(String content) {
        List<String> errors = new ArrayList<>();
        String lowered = content.toLowerCase(Locale.ROOT);
        if (!lowered.contains("select") && !lowered.contains("insert") && !lowered.contains("merge")) {
            errors.add("쿼리 본문에 SELECT/INSERT/MERGE 없음");
        }

        int beginCount = countKeyword(content, "BEGIN");
        int endCount = countKeyword(content, "END");
        if (beginCount != endCount) {
            errors.add("BEGIN-END 블록 수 불일치(" + beginCount + '/' + endCount + ")");
        }

        if (!content.trim().endsWith(";")) {
            errors.add("END 구문 뒤에 세미콜론 없음");
        }

        return errors;
    }

    private static int countKeyword(String content, String keyword) {
        Matcher matcher = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE).matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static void writeResultsToCSV(List<AnalysisResult> results, String csvPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvPath))) {
            writer.println("Step,Table,Column,Condition,Alias->Original,Error");

            for (AnalysisResult r : results) {
                int size = Math.max(Math.max(r.tables.size(), r.columns.size()), Math.max(r.conditions.size(), r.aliases.size()));
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
                    writer.printf("%s,%s,%s,%s,%s,%s%n", r.stepName, table, column, cond, alias, error);
                }
            }
        }
    }

    private static void writeResultsToHTML(List<AnalysisResult> results, List<String> allErrors, String htmlPath) throws IOException {
        Map<String, Integer> tableCounts = countOccurrences(results, r -> r.tables);
        Map<String, Integer> columnCounts = countOccurrences(results, r -> r.columns);

        try (PrintWriter writer = new PrintWriter(new FileWriter(htmlPath))) {
            writer.println("<html><head><meta charset='UTF-8'><title>분석 결과</title></head><body>");
            writer.println("<h1>BigQuery 분석 리포트</h1>");

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
            writer.println("<li>중복 테이블: " + formatDuplicates(tableCounts) + "</li>");
            writer.println("<li>중복 컬럼: " + formatDuplicates(columnCounts) + "</li>");
            writer.println("</ul>");

            for (AnalysisResult r : results) {
                writer.println("<hr><h3>" + r.stepName + "</h3>");
                writer.println("<b>테이블:</b><ul>");
                for (String t : r.tables) {
                    writer.println("<li>" + t + "</li>");
                }
                writer.println("</ul><b>컬럼:</b><ul>");
                for (String c : r.columns) {
                    writer.println("<li>" + c + "</li>");
                }
                writer.println("</ul><b>조건:</b><ul>");
                for (String c : r.conditions) {
                    writer.println("<li>" + c + "</li>");
                }
                writer.println("</ul><b>테이블 변경 추적:</b><ul>");
                for (Map.Entry<String, String> e : r.aliases.entrySet()) {
                    writer.println("<li>" + e.getKey() + " → " + e.getValue() + "</li>");
                }
                writer.println("</ul><b>오류:</b><ul>");
                for (String e : r.errors) {
                    writer.println("<li style='color:red;'>" + e + "</li>");
                }
                writer.println("</ul>");
            }

            writer.println("</body></html>");
        }
    }

    private static Map<String, Integer> countOccurrences(List<AnalysisResult> results, Function<AnalysisResult, Collection<String>> mapper) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (AnalysisResult result : results) {
            for (String value : mapper.apply(result)) {
                counts.put(value, counts.getOrDefault(value, 0) + 1);
            }
        }
        return counts;
    }

    private static String formatDuplicates(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .collect(Collectors.joining(", "));
    }

    private static void printDuplicateStats(List<AnalysisResult> results) {
        Map<String, Integer> tableCount = countOccurrences(results, r -> r.tables);
        Map<String, Integer> columnCount = countOccurrences(results, r -> r.columns);

        System.out.println("📊 중복 테이블 사용 통계:");
        if (tableCount.values().stream().noneMatch(count -> count > 1)) {
            System.out.println(" - 중복 테이블 없음");
        } else {
            tableCount.entrySet().stream()
                    .filter(e -> e.getValue() > 1)
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .forEach(e -> System.out.println(" - " + e.getKey() + ": " + e.getValue() + "회"));
        }

        System.out.println("📌 중복 컬럼 사용 통계:");
        if (columnCount.values().stream().noneMatch(count -> count > 1)) {
            System.out.println(" - 중복 컬럼 없음");
        } else {
            columnCount.entrySet().stream()
                    .filter(e -> e.getValue() > 1)
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .forEach(e -> System.out.println(" - " + e.getKey() + ": " + e.getValue() + "회"));
        }
    }

    /**
     * STEP 블록 정보 구조체
     */
    static class StepBlock {
        final String stepName;
        final String content;

        StepBlock(String stepName, String content) {
            this.stepName = stepName;
            this.content = content;
        }
    }

    /**
     * 분석 결과 구조체
     */
    static class AnalysisResult {
        final String stepName;
        final Set<String> tables;
        final Set<String> columns;
        final Set<String> conditions;
        final Map<String, String> aliases;
        final List<String> errors;

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
                for (String e : errors) {
                    System.out.println(" - " + e);
                }
            }
        }
    }

    static class AnalysisSession {
        final List<AnalysisResult> results;
        final List<String> allErrors;

        AnalysisSession(List<AnalysisResult> results, List<String> allErrors) {
            this.results = results;
            this.allErrors = allErrors;
        }
    }

    static class AnalyzerConfig {
        final String inputPath;
        final String csvPath;
        final String htmlPath;
        final boolean writeCsv;
        final boolean writeHtml;
        final boolean printStats;
        final boolean launchGui;
        final boolean showHelp;

        private AnalyzerConfig(Builder builder) {
            this.inputPath = builder.inputPath;
            this.csvPath = builder.csvPath;
            this.htmlPath = builder.htmlPath;
            this.writeCsv = builder.writeCsv;
            this.writeHtml = builder.writeHtml;
            this.printStats = builder.printStats;
            this.launchGui = builder.launchGui;
            this.showHelp = builder.showHelp;
        }

        static AnalyzerConfig fromArgs(String[] args) {
            Builder builder = new Builder();
            if (args.length == 0) {
                builder.showHelp(true);
                return builder.build();
            }

            if (args.length == 1 && !args[0].startsWith("-")) {
                builder.inputPath(args[0]);
                return builder.build();
            }

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "-h":
                    case "--help":
                        builder.showHelp(true);
                        break;
                    case "-i":
                    case "--input":
                        builder.inputPath(requireValue(args, ++i, arg));
                        break;
                    case "--csv":
                        builder.csvPath(requireValue(args, ++i, arg));
                        break;
                    case "--html":
                        builder.htmlPath(requireValue(args, ++i, arg));
                        break;
                    case "--no-csv":
                        builder.writeCsv(false);
                        break;
                    case "--no-html":
                        builder.writeHtml(false);
                        break;
                    case "--no-stats":
                        builder.printStats(false);
                        break;
                    case "--gui":
                        builder.launchGui(true);
                        break;
                    default:
                        throw new IllegalArgumentException("알 수 없는 옵션: " + arg);
                }
            }
            return builder.build();
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " 옵션에 필요한 값이 없습니다.");
            }
            return args[index];
        }

        static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private String inputPath;
            private String csvPath = DEFAULT_CSV;
            private String htmlPath = DEFAULT_HTML;
            private boolean writeCsv = true;
            private boolean writeHtml = true;
            private boolean printStats = true;
            private boolean launchGui = false;
            private boolean showHelp = false;

            Builder inputPath(String inputPath) {
                this.inputPath = inputPath;
                return this;
            }

            Builder csvPath(String csvPath) {
                this.csvPath = csvPath;
                return this;
            }

            Builder htmlPath(String htmlPath) {
                this.htmlPath = htmlPath;
                return this;
            }

            Builder writeCsv(boolean writeCsv) {
                this.writeCsv = writeCsv;
                return this;
            }

            Builder writeHtml(boolean writeHtml) {
                this.writeHtml = writeHtml;
                return this;
            }

            Builder printStats(boolean printStats) {
                this.printStats = printStats;
                return this;
            }

            Builder launchGui(boolean launchGui) {
                this.launchGui = launchGui;
                return this;
            }

            Builder showHelp(boolean showHelp) {
                this.showHelp = showHelp;
                return this;
            }

            AnalyzerConfig build() {
                return new AnalyzerConfig(this);
            }
        }
    }

    public static class AnalyzerFxApp {
        private static AnalyzerConfig initialConfig = AnalyzerConfig.builder().launchGui(true).build();
        private JTextArea outputArea;

        static void bootstrap(AnalyzerConfig config) {
            initialConfig = config;
            SwingUtilities.invokeLater(() -> new AnalyzerFxApp().createAndShowGui());
        }

        private void createAndShowGui() {
            JFrame frame = new JFrame("BigQuery Script Analyzer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(720, 480);

            JPanel panel = new JPanel();
            panel.setLayout(null);

            JLabel inputLabel = new JLabel("입력 파일");
            inputLabel.setBounds(12, 12, 100, 25);
            panel.add(inputLabel);

            JTextField inputField = new JTextField();
            inputField.setBounds(120, 12, 480, 25);
            if (initialConfig.inputPath != null) {
                inputField.setText(initialConfig.inputPath);
            }
            panel.add(inputField);

            JButton browseBtn = new JButton("찾아보기");
            browseBtn.setBounds(608, 12, 100, 25);
            browseBtn.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("SQL 파일 선택");
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    inputField.setText(file.getAbsolutePath());
                }
            });
            panel.add(browseBtn);

            JLabel csvLabel = new JLabel("CSV 경로");
            csvLabel.setBounds(12, 50, 100, 25);
            panel.add(csvLabel);

            JTextField csvField = new JTextField(initialConfig.csvPath != null ? initialConfig.csvPath : DEFAULT_CSV);
            csvField.setBounds(120, 50, 480, 25);
            panel.add(csvField);

            JLabel htmlLabel = new JLabel("HTML 경로");
            htmlLabel.setBounds(12, 88, 100, 25);
            panel.add(htmlLabel);

            JTextField htmlField = new JTextField(initialConfig.htmlPath != null ? initialConfig.htmlPath : DEFAULT_HTML);
            htmlField.setBounds(120, 88, 480, 25);
            panel.add(htmlField);

            JCheckBox csvCheck = new JCheckBox("CSV 저장");
            csvCheck.setBounds(120, 126, 100, 25);
            csvCheck.setSelected(initialConfig.writeCsv);
            panel.add(csvCheck);

            JCheckBox htmlCheck = new JCheckBox("HTML 저장");
            htmlCheck.setBounds(240, 126, 100, 25);
            htmlCheck.setSelected(initialConfig.writeHtml);
            panel.add(htmlCheck);

            JCheckBox statsCheck = new JCheckBox("중복 통계 출력");
            statsCheck.setBounds(360, 126, 150, 25);
            statsCheck.setSelected(initialConfig.printStats);
            panel.add(statsCheck);

            JButton analyzeBtn = new JButton("분석 실행");
            analyzeBtn.setBounds(520, 126, 188, 25);
            analyzeBtn.addActionListener(e -> runGuiAnalysis(
                    inputField.getText().trim(),
                    csvField.getText().trim(),
                    htmlField.getText().trim(),
                    csvCheck.isSelected(),
                    htmlCheck.isSelected(),
                    statsCheck.isSelected()));
            panel.add(analyzeBtn);

            outputArea = new JTextArea();
            outputArea.setEditable(false);
            outputArea.setLineWrap(true);
            outputArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(outputArea);
            scrollPane.setBounds(12, 164, 696, 275);
            panel.add(scrollPane);

            frame.setContentPane(panel);
            frame.setVisible(true);
        }

        private void runGuiAnalysis(String inputPath, String csvPath, String htmlPath,
                                    boolean csvEnabled, boolean htmlEnabled, boolean statsEnabled) {
            if (inputPath == null || inputPath.isEmpty()) {
                showAlert("입력 경로를 지정하세요.");
                return;
            }

            outputArea.setText("");
            new Thread(() -> {
                try {
                    AnalyzerConfig config = AnalyzerConfig.builder()
                            .inputPath(inputPath)
                            .csvPath(csvPath.isEmpty() ? DEFAULT_CSV : csvPath)
                            .htmlPath(htmlPath.isEmpty() ? DEFAULT_HTML : htmlPath)
                            .writeCsv(csvEnabled)
                            .writeHtml(htmlEnabled)
                            .printStats(statsEnabled)
                            .build();

                    AnalysisSession session = analyze(config.inputPath);
                    if (config.writeCsv) {
                        writeResultsToCSV(session.results, config.csvPath);
                    }
                    if (config.writeHtml) {
                        writeResultsToHTML(session.results, session.allErrors, config.htmlPath);
                    }
                    if (config.printStats) {
                        String stats = buildSummaryMessage(session) + "\n" +
                                formatGuiStats(countOccurrences(session.results, r -> r.tables),
                                        countOccurrences(session.results, r -> r.columns));
                        SwingUtilities.invokeLater(() -> outputArea.setText(stats));
                    } else {
                        String summary = buildSummaryMessage(session);
                        SwingUtilities.invokeLater(() -> outputArea.setText(summary));
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> outputArea.setText("오류: " + ex.getMessage()));
                }
            }).start();
        }

        private void showAlert(String message) {
            JOptionPane.showMessageDialog(null, message, "경고", JOptionPane.WARNING_MESSAGE);
        }

        private String formatGuiStats(Map<String, Integer> tableCounts, Map<String, Integer> columnCounts) {
            return "중복 테이블: " + orNone(formatDuplicates(tableCounts)) +
                    "\n중복 컬럼: " + orNone(formatDuplicates(columnCounts));
        }

        private String orNone(String value) {
            return value == null || value.isEmpty() ? "없음" : value;
        }
    }
}
