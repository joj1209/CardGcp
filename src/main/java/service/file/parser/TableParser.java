package service.file.parser;

import service.file.pattern.TableNamePattern;
import service.file.pattern.TableSourcePattern;
import service.file.pattern.TableTargetPattern;
import service.file.vo.TablesInfo;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 쿼리에서 소스/타겟 테이블을 추출하는 파서 클래스
 *
 * TableNamePattern, TableSourcePattern, TableTargetPattern의 패턴 정의를 참조하여
 * 실제 파싱 로직을 구현합니다.
 */
public class TableParser {

    /**
     * SQL 문자열에서 소스/타겟 테이블을 추출합니다.
     *
     * @param sql SQL 문자열
     * @return 추출된 테이블 정보
     */
    public TablesInfo extractTables(String sql) {
        TablesInfo info = new TablesInfo();

        // 주석 제거
        String cleanedSql = removeComments(sql);

        // WITH 절 CTE 별칭 추출 (제외할 목록)
        Set<String> cteAliases = extractCteAliases(cleanedSql);

        // 타겟 테이블 추출
        extractTargetTables(cleanedSql, info.getTargets());

        // 소스 테이블 추출
        extractSourceTables(cleanedSql, info.getSources());

        // CTE 별칭 제외
        info.getSources().removeAll(cteAliases);

        return info;
    }

    /**
     * SQL 주석을 제거합니다.
     *
     * @param sql SQL 문자열
     * @return 주석이 제거된 SQL
     */
    private String removeComments(String sql) {
        return sql.replaceAll("(?s)/\\*.*?\\*/", " ")
                  .replaceAll("--.*?(\r?\n|$)", " ");
    }

    /**
     * WITH 절의 CTE(Common Table Expression) 별칭을 추출합니다.
     * 이 별칭들은 실제 테이블이 아니므로 소스 테이블에서 제외해야 합니다.
     *
     * 예: WITH `모수` AS (...), MOSU2 AS (...)
     * 추출: `모수`, MOSU2
     *
     * @param sql SQL 문자열
     * @return CTE 별칭 Set
     */
    private Set<String> extractCteAliases(String sql) {
        Set<String> aliases = new java.util.HashSet<>();

        // WITH alias AS 패턴
        Pattern ctePattern = Pattern.compile(
            "(?is)\\bWITH\\s+" + TableNamePattern.TABLE_NAME_REGEX + "\\s+AS\\s*\\("
        );

        Matcher matcher = ctePattern.matcher(sql);
        while (matcher.find()) {
            String alias = matcher.group(1);
            String cleaned = TableNamePattern.cleanTableName(alias);
            if (!cleaned.isEmpty()) {
                aliases.add(cleaned);
            }
        }

        // , alias AS 패턴 (WITH 절 내 추가 CTE)
        Pattern additionalCtePattern = Pattern.compile(
            "(?is),\\s*" + TableNamePattern.TABLE_NAME_REGEX + "\\s+AS\\s*\\("
        );

        matcher = additionalCtePattern.matcher(sql);
        while (matcher.find()) {
            String alias = matcher.group(1);
            String cleaned = TableNamePattern.cleanTableName(alias);
            if (!cleaned.isEmpty()) {
                aliases.add(cleaned);
            }
        }

        return aliases;
    }

    /**
     * 타겟 테이블을 추출합니다.
     * TableTargetPattern의 패턴 정의를 사용합니다.
     *
     * @param sql SQL 문자열
     * @param targets 타겟 테이블을 저장할 Set
     */
    private void extractTargetTables(String sql, Set<String> targets) {
        // MERGE INTO 패턴
        extractByPattern(sql, TableTargetPattern.MERGE_PATTERN, targets);

        // INSERT INTO 패턴
        extractByPattern(sql, TableTargetPattern.INSERT_PATTERN, targets);

        // DELETE FROM 패턴
        extractByPattern(sql, TableTargetPattern.DELETE_FROM_PATTERN, targets);

        // UPDATE 패턴
        extractByPattern(sql, TableTargetPattern.UPDATE_PATTERN, targets);

        // DELETE 패턴 (Oracle 방식 - WHERE 앞에만)
        extractDeletePattern(sql, targets);
    }

    /**
     * DELETE 패턴을 추출합니다 (Oracle 방식).
     * DELETE table WHERE ... 형태를 처리합니다.
     *
     * @param sql SQL 문자열
     * @param targets 타겟 테이블을 저장할 Set
     */
    private void extractDeletePattern(String sql, Set<String> targets) {
        Pattern pattern = Pattern.compile(
            "(?is)\\bDELETE\\s+" + TableNamePattern.TABLE_NAME_REGEX + "\\s+(?:WHERE|$)"
        );

        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            addTableIfValid(tableName, targets);
        }
    }

    /**
     * 소스 테이블을 추출합니다.
     * TableSourcePattern의 패턴 정의를 사용합니다.
     *
     * @param sql SQL 문자열
     * @param sources 소스 테이블을 저장할 Set
     */
    private void extractSourceTables(String sql, Set<String> sources) {
        // FROM 절
        extractByPattern(sql, TableSourcePattern.FROM_PATTERN, sources);

        // LEFT JOIN
        extractByPattern(sql, TableSourcePattern.LEFT_JOIN_PATTERN, sources);

        // INNER JOIN
        extractByPattern(sql, TableSourcePattern.INNER_JOIN_PATTERN, sources);

        // RIGHT JOIN
        extractByPattern(sql, TableSourcePattern.RIGHT_JOIN_PATTERN, sources);

        // JOIN
        extractByPattern(sql, TableSourcePattern.JOIN_PATTERN, sources);

        // USING
        extractByPattern(sql, TableSourcePattern.USING_PATTERN, sources);

        // WITH 절 (CTE)
        extractWithClause(sql, sources);

        // Oracle 조인 (콤마)
        extractOracleJoin(sql, sources);
    }

    /**
     * WITH 절에서 소스 테이블을 추출합니다.
     *
     * @param sql SQL 문자열
     * @param sources 소스 테이블을 저장할 Set
     */
    private void extractWithClause(String sql, Set<String> sources) {
        // WITH 절 내부의 FROM 절에서 테이블 추출
        Pattern withPattern = Pattern.compile(
            "(?is)\\bWITH\\s+.*?\\bFROM\\s+" + TableNamePattern.TABLE_NAME_REGEX
        );

        Matcher matcher = withPattern.matcher(sql);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            addTableIfValid(tableName, sources);
        }
    }

    /**
     * Oracle 조인 문법(콤마)에서 소스 테이블을 추출합니다.
     * FROM table1, table2 형태를 처리합니다.
     *
     * @param sql SQL 문자열
     * @param sources 소스 테이블을 저장할 Set
     */
    private void extractOracleJoin(String sql, Set<String> sources) {
        Pattern pattern = Pattern.compile(TableSourcePattern.FROM_CLAUSE_RANGE_PATTERN);
        Matcher matcher = pattern.matcher(sql);

        while (matcher.find()) {
            // 1) 문자열 리터럴 안의 FROM 은 무시
            if (isInsideSingleQuotes(sql, matcher.start())) {
                continue;
            }

            String fromClause = matcher.group(1);
            if (fromClause == null) continue;

            fromClause = fromClause.trim();
            if (fromClause.isEmpty()) continue;

            String upper = fromClause.toUpperCase();

            // 2) 서브쿼리 / 인라인뷰 / CTE 가 섞인 경우는 여기서 처리하지 않음
            if (upper.contains("SELECT") || upper.contains("WITH")) {
                continue;
            }


            // 3) JOIN 키워드 이전까지만 처리
            int joinPos = fromClause.toUpperCase().indexOf("JOIN");
            if (joinPos > 0) {
                fromClause = fromClause.substring(0, joinPos);
            }

            // 4) 콤마로 구분된 테이블 추출
            String[] tables = fromClause.split(",");
            for (String table : tables) {
                extractFirstTable(table.trim(), sources);
            }
        }
    }

    /**
     * 주어진 index 위치가 작은따옴표(') 기준으로
     * 문자열 리터럴 내부인지 여부를 판단
     */
    private boolean isInsideSingleQuotes(String sql, int index) {
        boolean inside = false;
        for (int i = 0; i < index; i++) {
            if (sql.charAt(i) == '\'') {
                inside = !inside; // ' 만날 때마다 안/밖 토글
            }
        }
        return inside;
    }

    /**
     * 문자열에서 첫 번째 테이블명을 추출합니다 (별칭 제외).
     *
     * @param text 문자열
     * @param tables 테이블을 저장할 Set
     */
    private void extractFirstTable(String text, Set<String> tables) {
        if (text == null || text.isEmpty()) return;

        Pattern pattern = Pattern.compile("^\\s*" + TableNamePattern.TABLE_NAME_REGEX);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String tableName = matcher.group(1);
            addTableIfValid(tableName, tables);
        }
    }

    /**
     * 패턴으로 테이블을 추출합니다.
     * TableNamePattern의 패턴을 사용합니다.
     *
     * @param sql SQL 문자열
     * @param keyword SQL 키워드
     * @param tables 테이블을 저장할 Set
     */
    private void extractByPattern(String sql, String keyword, Set<String> tables) {
        Pattern pattern = TableNamePattern.buildPattern(keyword);
        Matcher matcher = pattern.matcher(sql);

        while (matcher.find()) {
            String tableName = matcher.group(1);
            addTableIfValid(tableName, tables);
        }
    }

    /**
     * 유효한 테이블명인 경우 Set에 추가합니다.
     * TableNamePattern의 유효성 검사를 사용합니다.
     *
     * @param tableName 테이블명
     * @param tables 테이블을 저장할 Set
     */
    private void addTableIfValid(String tableName, Set<String> tables) {
        String cleaned = TableNamePattern.cleanTableName(tableName);

        if (TableNamePattern.isValidTableName(cleaned)) {
            tables.add(cleaned);
        }
    }
}
