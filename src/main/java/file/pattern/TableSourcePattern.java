package file.pattern;

/**
 * 소스 테이블 추출 패턴 정의 클래스
 *
 * 소스 테이블: 데이터를 읽는 테이블
 *
 * 지원 패턴:
 * 1. FROM 절
 * 2. LEFT JOIN / LEFT OUTER JOIN
 * 3. JOIN / INNER JOIN / RIGHT JOIN
 * 4. Oracle JOIN 문법 (콤마로 구분)
 * 5. WITH 절 (CTE)
 * 6. USING 절 (MERGE)
 */
public class TableSourcePattern {

    /**
     * FROM 절 패턴
     */
    public static final String FROM_PATTERN = "FROM";

    /**
     * LEFT JOIN 패턴
     */
    public static final String LEFT_JOIN_PATTERN = "LEFT\\s+(?:OUTER\\s+)?JOIN";

    /**
     * INNER JOIN 패턴
     */
    public static final String INNER_JOIN_PATTERN = "INNER\\s+JOIN";

    /**
     * RIGHT JOIN 패턴
     */
    public static final String RIGHT_JOIN_PATTERN = "RIGHT\\s+(?:OUTER\\s+)?JOIN";

    /**
     * JOIN 패턴
     */
    public static final String JOIN_PATTERN = "JOIN";

    /**
     * USING 절 패턴 (MERGE 구문)
     */
    public static final String USING_PATTERN = "USING";

    /**
     * WITH 절 패턴 (CTE)
     */
    public static final String WITH_PATTERN = "WITH";

    /**
     * Oracle 조인 문법을 위한 FROM 절 범위 패턴
     * FROM ... WHERE/GROUP/ORDER/HAVING/UNION/LIMIT/;/$
     */
    public static final String FROM_CLAUSE_RANGE_PATTERN =
        "(?is)\\bFROM\\s+(.*?)(?=\\s+WHERE|\\s+GROUP|\\s+ORDER|\\s+HAVING|\\s+UNION|\\s+LIMIT|;|$)";

    /**
     * 모든 소스 패턴 배열
     */
    public static final String[] ALL_SOURCE_PATTERNS = {
        FROM_PATTERN,
        LEFT_JOIN_PATTERN,
        INNER_JOIN_PATTERN,
        RIGHT_JOIN_PATTERN,
        JOIN_PATTERN,
        USING_PATTERN
    };
}
