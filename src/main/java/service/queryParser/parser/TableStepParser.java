package service.queryParser.parser;

import service.queryParser.vo.TablesInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 스크립트를 STEP별로 분할하여 각 STEP의 소스/타겟 테이블을 추출하는 클래스
 *
 * 기능:
 * 1. SQL 스크립트를 STEP 단위로 분할
 * 2. 각 STEP별로 소스/타겟 테이블 추출
 * 3. STEP 정보와 테이블 정보를 함께 관리
 */
public class TableStepParser {

    private final TableParser tableParser;

    /**
     * 기본 생성자
     */
    public TableStepParser() {
        this.tableParser = new TableParser();
    }

    /**
     * TableParser를 주입받는 생성자
     *
     * @param tableParser 테이블 파서
     */
    public TableStepParser(TableParser tableParser) {
        this.tableParser = tableParser;
    }

    /**
     * SQL 스크립트를 STEP별로 분할하고 각 STEP의 테이블을 추출합니다.
     *
     * @param sql 전체 SQL 스크립트
     * @return STEP별 테이블 정보 Map (Key: STEP명, Value: TablesInfo)
     */
    public Map<String, TablesInfo> extractTablesByStep(String sql) {
        Map<String, TablesInfo> stepTables = new LinkedHashMap<>();

        List<StepInfo> steps = splitBySteps(sql);

        for (StepInfo step : steps) {
            TablesInfo tables = tableParser.extractTables(step.sql);
            stepTables.put(step.stepName, tables);
        }

        return stepTables;
    }

    /**
     * SQL 스크립트를 STEP 단위로 분할합니다.
     *
     * 지원 패턴: STEP001, STEP002 등
     *
     * @param sql 전체 SQL 스크립트
     * @return STEP 정보 리스트
     */
    private List<StepInfo> splitBySteps(String sql) {
        List<StepInfo> steps = new ArrayList<>();

        // STEP 패턴: /* STEP001 */ 또는 -- STEP001
        Pattern stepPattern = Pattern.compile(
            "(?:/\\*\\s*STEP(\\d+)\\s*\\*/|--\\s*STEP(\\d+))",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = stepPattern.matcher(sql);

        List<StepMatch> matches = new ArrayList<>();
        while (matcher.find()) {
            String stepNumber = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matches.add(new StepMatch("STEP" + stepNumber, matcher.start(), matcher.end()));
        }

        // STEP별로 SQL 분할
        for (int i = 0; i < matches.size(); i++) {
            StepMatch current = matches.get(i);
            int sqlStart = current.end;
            int sqlEnd = (i + 1 < matches.size()) ? matches.get(i + 1).start : sql.length();

            String stepSql = sql.substring(sqlStart, sqlEnd).trim();
            if (!stepSql.isEmpty()) {
                steps.add(new StepInfo(current.stepName, stepSql));
            }
        }

        return steps;
    }

    /**
     * 전체 SQL의 테이블 정보를 추출합니다 (STEP 구분 없이).
     *
     * @param sql 전체 SQL 스크립트
     * @return 전체 테이블 정보
     */
    public TablesInfo extractAllTables(String sql) {
        return tableParser.extractTables(sql);
    }

    /**
     * STEP 정보를 담는 내부 클래스
     */
    private static class StepInfo {
        final String stepName;
        final String sql;

        StepInfo(String stepName, String sql) {
            this.stepName = stepName;
            this.sql = sql;
        }
    }

    /**
     * STEP 매칭 정보를 담는 내부 클래스
     */
    private static class StepMatch {
        final String stepName;
        final int start;
        final int end;

        StepMatch(String stepName, int start, int end) {
            this.stepName = stepName;
            this.start = start;
            this.end = end;
        }
    }

    /**
     * STEP별 테이블 정보를 포맷팅하여 문자열로 반환합니다.
     *
     * @param stepTables STEP별 테이블 정보
     * @return 포맷팅된 문자열
     */
    public String formatStepTables(Map<String, TablesInfo> stepTables) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, TablesInfo> entry : stepTables.entrySet()) {
            String stepName = entry.getKey();
            TablesInfo tables = entry.getValue();

            sb.append("=".repeat(60)).append("\n");
            sb.append(" ").append(stepName).append("\n");
            sb.append("=".repeat(60)).append("\n");

            sb.append("\n[Source Tables]\n");
            if (tables.getSources().isEmpty()) {
                sb.append("(No source tables)\n");
            } else {
                for (String source : tables.getSources()) {
                    sb.append(source).append("\n");
                }
            }

            sb.append("\n[Target Tables]\n");
            if (tables.getTargets().isEmpty()) {
                sb.append("(No target tables)\n");
            } else {
                for (String target : tables.getTargets()) {
                    sb.append(target).append("\n");
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * STEP 개수를 반환합니다.
     *
     * @param sql SQL 스크립트
     * @return STEP 개수
     */
    public int countSteps(String sql) {
        return splitBySteps(sql).size();
    }

    /**
     * 특정 STEP의 테이블 정보를 추출합니다.
     *
     * @param sql SQL 스크립트
     * @param stepName STEP 이름 (예: "STEP001")
     * @return 해당 STEP의 테이블 정보, 없으면 null
     */
    public TablesInfo extractTablesForStep(String sql, String stepName) {
        Map<String, TablesInfo> stepTables = extractTablesByStep(sql);
        return stepTables.get(stepName);
    }
}
