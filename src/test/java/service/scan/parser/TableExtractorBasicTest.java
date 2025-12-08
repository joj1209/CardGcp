package service.scan.parser;

import service.scanSourceTarget.scan.model.TablesInfo;
import service.scanSourceTarget.scan.parser.TableExtractor;

/**
 * TableExtractor 기본 기능 테스트
 *
 * 목적: Source/Target 테이블 추출 기본 기능 검증
 */
public class TableExtractorBasicTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("TableExtractor 기본 기능 테스트");
        System.out.println("========================================\n");

        testInsertInto();
        testUpdate();
        testDelete();
        testSelectFrom();
        testJoin();
        testComplexQuery();
        testCommaSeparatedFromClauseCapturesAllTables();

        System.out.println("\n========================================");
        System.out.println("테스트 완료!");
        System.out.println("========================================");
    }

    /**
     * 테스트 1: INSERT INTO (Target 테이블)
     */
    private static void testInsertInto() {
        System.out.println("[테스트 1] INSERT INTO");
        String sql = "INSERT INTO emp_master (id, name) VALUES (1, 'John');";

        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);

        System.out.println("  SQL: " + sql);
        System.out.println("  Target: " + tables.getTargets());

        boolean pass = tables.getTargets().contains("emp_master") &&
                      tables.getSources().isEmpty();
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }

    /**
     * 테스트 2: UPDATE (Target 테이블)
     */
    private static void testUpdate() {
        System.out.println("[테스트 2] UPDATE");
        String sql = "UPDATE dept_master SET name = 'IT' WHERE id = 1;";

        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);

        System.out.println("  SQL: " + sql);
        System.out.println("  Target: " + tables.getTargets());

        boolean pass = tables.getTargets().contains("dept_master");
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }

    /**
     * 테스트 3: DELETE FROM (Target 테이블)
     */
    private static void testDelete() {
        System.out.println("[테스트 3] DELETE FROM");
        String sql = "DELETE FROM temp_table WHERE created < '2023-01-01';";

        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);

        System.out.println("  SQL: " + sql);
        System.out.println("  Target: " + tables.getTargets());

        boolean pass = tables.getTargets().contains("temp_table");
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }

    /**
     * 테스트 4: SELECT FROM (Source 테이블)
     */
    private static void testSelectFrom() {
        System.out.println("[테스트 4] SELECT FROM");
        String sql = "SELECT * FROM emp_master WHERE dept_id = 1;";

        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);

        System.out.println("  SQL: " + sql);
        System.out.println("  Source: " + tables.getSources());

        boolean pass = tables.getSources().contains("emp_master") &&
                      tables.getTargets().isEmpty();
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }

    /**
     * 테스트 5: JOIN (Source 테이블)
     */
    private static void testJoin() {
        System.out.println("[테스트 5] JOIN");
        String sql =
            "SELECT a.*, b.dept_name " +
            "FROM emp_master a " +
            "JOIN dept_master b ON a.dept_id = b.dept_id;";

        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);

        System.out.println("  SQL: " + sql);
        System.out.println("  Source: " + tables.getSources());

        boolean pass = tables.getSources().contains("emp_master") &&
                      tables.getSources().contains("dept_master");
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }

    /**
     * 테스트 6: 복잡한 쿼리 (INSERT INTO ... SELECT)
     */
    private static void testComplexQuery() {
        System.out.println("[테스트 6] 복잡한 쿼리 (INSERT INTO ... SELECT)");
        String sql =
            "INSERT INTO result_table " +
            "SELECT a.id, a.name, b.dept_name " +
            "FROM emp_master a " +
            "JOIN dept_master b ON a.dept_id = b.dept_id " +
            "WHERE a.status = 'ACTIVE';";

        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);

        System.out.println("  Source: " + tables.getSources());
        System.out.println("  Target: " + tables.getTargets());

        boolean pass = tables.getTargets().contains("result_table") &&
                      tables.getSources().contains("emp_master") &&
                      tables.getSources().contains("dept_master");
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }

    /**
     * 테스트 7: 쉼표로 구분된 테이블 (FROM 절)
     */
    private static void testCommaSeparatedFromClauseCapturesAllTables() {
        System.out.println("[테스트 7] 쉼표로 구분된 테이블 (FROM 절)");
        String sql = "INSERT INTO BM.`회사` SELECT * FROM DW.`회사목록` N1, DW.`사무실` N2 WHERE N1.회사ID = N2.회사ID;";

        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);

        System.out.println("  SQL: " + sql);
        System.out.println("  Target: " + tables.getTargets());
        System.out.println("  Source: " + tables.getSources());

        boolean pass = tables.getTargets().contains("BM.`회사`") &&
                      tables.getSources().contains("DW.`회사목록`") &&
                      tables.getSources().contains("DW.`사무실`");
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }
}
