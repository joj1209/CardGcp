package service.scan.parser;

import service.scanSourceTarget.scan.model.TablesInfo;
import service.scanSourceTarget.scan.parser.TableExtractor;

/**
 * TableExtractor SQL 키워드 필터링 테스트
 *
 * 목적: SQL 예약어나 키워드가 테이블명으로 잘못 인식되지 않는지 검증
 */
public class TableExtractorKeywordFilterTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("TableExtractor SQL 키워드 필터링 테스트");
        System.out.println("========================================\n");

        testPostgreSQLDump();
        testSQLKeywords();
        testMixedKeywordsAndTables();
        testSingleCharacterAlias();

        System.out.println("\n========================================");
        System.out.println("테스트 완료!");
        System.out.println("========================================");
    }

    /**
     * 테스트 1: PostgreSQL 덤프 파일 (PUBLIC, CASCADE, STDIN 등)
     */
    private static void testPostgreSQLDump() {
        System.out.println("[테스트 1] PostgreSQL 덤프 파일 키워드");
        String sql =
            "REVOKE USAGE ON SCHEMA public FROM PUBLIC;\n" +
            "DELETE FROM CASCADE;\n" +
            "COPY table_name FROM STDIN;\n";

        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);

        System.out.println("  SQL: " + sql.replace("\n", " "));
        System.out.println("  Source: " + tables.getSources());
        System.out.println("  Target: " + tables.getTargets());

        // PUBLIC, CASCADE, STDIN은 제외되어야 함
        boolean pass = !tables.getSources().contains("PUBLIC") &&
                      !tables.getTargets().contains("CASCADE") &&
                      !tables.getSources().contains("STDIN");
        System.out.println("  결과: " + (pass ? "✅ PASS (키워드 필터링 성공)" : "❌ FAIL") + "\n");
    }

    /**
     * 테스트 2: SQL 키워드가 테이블로 오인되는 경우
     */
    private static void testSQLKeywords() {
        System.out.println("[테스트 2] SQL 키워드 필터링");
        String sql =
            "SELECT * FROM DUAL;\n" +
            "INSERT INTO VALUES;\n" +
            "UPDATE SET WHERE id = 1;\n";

        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);

        System.out.println("  Source: " + tables.getSources());
        System.out.println("  Target: " + tables.getTargets());

        // DUAL, VALUES, SET은 제외되어야 함
        boolean pass = !tables.getSources().contains("DUAL") &&
                      !tables.getTargets().contains("VALUES") &&
                      !tables.getTargets().contains("SET");
        System.out.println("  결과: " + (pass ? "✅ PASS (키워드 필터링 성공)" : "❌ FAIL") + "\n");
    }

    /**
     * 테스트 3: 실제 테이블명과 키워드 혼합
     */
    private static void testMixedKeywordsAndTables() {
        System.out.println("[테스트 3] 실제 테이블명과 키워드 혼합");
        String sql =
            "SELECT * FROM emp_master, DUAL;\n" +
            "INSERT INTO dept_master SELECT * FROM PUBLIC.test_table;\n";

        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);

        System.out.println("  Source: " + tables.getSources());
        System.out.println("  Target: " + tables.getTargets());

        // emp_master는 포함, DUAL과 PUBLIC은 제외되어야 함
        boolean pass = tables.getSources().contains("emp_master") &&
                      !tables.getSources().contains("DUAL") &&
                      !tables.getSources().contains("PUBLIC") &&
                      tables.getTargets().contains("dept_master");
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }

    /**
     * 테스트 4: 단일 문자 별칭(alias) 제외
     */
    private static void testSingleCharacterAlias() {
        System.out.println("[테스트 4] 단일 문자 별칭 제외");
        String sql =
            "SELECT * FROM emp_master a JOIN dept_master b ON a.dept_id = b.dept_id;\n";

        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);

        System.out.println("  Source: " + tables.getSources());

        // emp_master, dept_master는 포함, a와 b는 제외되어야 함
        boolean pass = tables.getSources().contains("emp_master") &&
                      tables.getSources().contains("dept_master") &&
                      !tables.getSources().contains("a") &&
                      !tables.getSources().contains("b");
        System.out.println("  결과: " + (pass ? "✅ PASS (단일 문자 제외 성공)" : "❌ FAIL") + "\n");
    }
}

