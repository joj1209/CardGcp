package service.file.pattern;

/**
 * 타겟 테이블 추출 패턴 정의 클래스
 *
 * 타겟 테이블: 데이터가 변경되는 테이블
 *
 * 지원 패턴:
 * 1. INSERT INTO
 * 2. UPDATE
 * 3. DELETE / DELETE FROM
 * 4. MERGE INTO
 */
public class TableTargetPattern {

    /**
     * INSERT INTO 패턴
     */
    public static final String INSERT_PATTERN = "INSERT\\s+INTO";

    /**
     * UPDATE 패턴
     */
    public static final String UPDATE_PATTERN = "UPDATE";

    /**
     * DELETE FROM 패턴
     */
    public static final String DELETE_FROM_PATTERN = "DELETE\\s+FROM";

    /**
     * DELETE 패턴 (Oracle 방식)
     */
    public static final String DELETE_PATTERN = "DELETE";

    /**
     * MERGE INTO 패턴
     */
    public static final String MERGE_PATTERN = "MERGE\\s+INTO";

    /**
     * 모든 타겟 패턴 배열
     */
    public static final String[] ALL_TARGET_PATTERNS = {
        MERGE_PATTERN,      // MERGE를 먼저 매칭 (DELETE보다 우선)
        INSERT_PATTERN,
        DELETE_FROM_PATTERN,
        UPDATE_PATTERN,
        DELETE_PATTERN
    };
}
