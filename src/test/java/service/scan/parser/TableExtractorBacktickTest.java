package service.scan.parser;

import service.scanSourceTarget.analyze.scan.model.TablesInfo;
import service.scanSourceTarget.analyze.scan.parser.TableExtractor;

/**
 * TableExtractor 백틱 테이블명 추출 테스트
 * 
 * 목적: 백틱이 있는/없는 테이블명이 모두 정상적으로 추출되는지 검증
 */
public class TableExtractorBacktickTest {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("TableExtractor 백틱 테이블명 추출 테스트");
        System.out.println("========================================\n");
        
        testBacktickTables();
        testMixedTables();
        testSchemaWithBacktick();
        testKoreanTableNames();
        
        System.out.println("\n========================================");
        System.out.println("테스트 완료!");
        System.out.println("========================================");
    }
    
    /**
     * 테스트 1: 백틱이 있는 테이블명
     */
    private static void testBacktickTables() {
        System.out.println("[테스트 1] 백틱이 있는 테이블명");
        String sql = 
            "SELECT * FROM `사원마스터`;\n" +
            "INSERT INTO `부서마스터` VALUES (1, 'IT');\n";
        
        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);
        
        System.out.println("  SQL: " + sql.replace("\n", " "));
        System.out.println("  Source: " + tables.getSources());
        System.out.println("  Target: " + tables.getTargets());
        
        boolean pass = tables.getSources().contains("`사원마스터`") &&
                      tables.getTargets().contains("`부서마스터`");
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }
    
    /**
     * 테스트 2: 백틱 있음/없음 혼합
     */
    private static void testMixedTables() {
        System.out.println("[테스트 2] 백틱 있음/없음 혼합");
        String sql = 
            "SELECT * FROM emp_master;\n" +
            "SELECT * FROM `사원마스터`;\n" +
            "INSERT INTO dept_master VALUES (1, 'IT');\n" +
            "INSERT INTO `부서마스터` VALUES (1, 'IT');\n";
        
        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);
        
        System.out.println("  Source: " + tables.getSources());
        System.out.println("  Target: " + tables.getTargets());
        
        boolean pass = tables.getSources().contains("emp_master") &&
                      tables.getSources().contains("`사원마스터`") &&
                      tables.getTargets().contains("dept_master") &&
                      tables.getTargets().contains("`부서마스터`");
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }
    
    /**
     * 테스트 3: 스키마명 + 백틱
     */
    private static void testSchemaWithBacktick() {
        System.out.println("[테스트 3] 스키마명 + 백틱");
        String sql = 
            "SELECT * FROM mydb.`직원정보`;\n" +
            "UPDATE mydb.`급여내역` SET amount = 1000;\n";
        
        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);
        
        System.out.println("  Source: " + tables.getSources());
        System.out.println("  Target: " + tables.getTargets());
        
        boolean pass = tables.getSources().contains("mydb.`직원정보`") &&
                      tables.getTargets().contains("mydb.`급여내역`");
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }
    
    /**
     * 테스트 4: 한글 테이블명 (백틱 필수)
     */
    private static void testKoreanTableNames() {
        System.out.println("[테스트 4] 한글 테이블명");
        String sql = 
            "SELECT a.* FROM `한글테이블` a JOIN `영문테이블` b ON a.id = b.id;\n" +
            "INSERT INTO `결과테이블` SELECT * FROM `원본테이블`;\n";
        
        TableExtractor extractor = new TableExtractor();
        TablesInfo tables = extractor.extractTables(sql);
        
        System.out.println("  Source: " + tables.getSources());
        System.out.println("  Target: " + tables.getTargets());
        
        boolean pass = tables.getSources().size() >= 3 &&
                      tables.getTargets().contains("`결과테이블`");
        System.out.println("  결과: " + (pass ? "✅ PASS" : "❌ FAIL") + "\n");
    }
}

