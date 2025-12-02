package test;

import file.parser.TableStepParser;
import file.vo.TablesInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * TableStepParser 클래스 테스트
 */
public class TestTableStepParser {
    public static void main(String[] args) throws IOException {
        String sqlPath = "D:\\11. Project\\11. DB\\BigQuery\\sample002.sql";
        String sql = new String(Files.readAllBytes(Paths.get(sqlPath)), "UTF-8");

        TableStepParser stepParser = new TableStepParser();

        // STEP 개수 확인
        int stepCount = stepParser.countSteps(sql);
        System.out.println("Total STEPs: " + stepCount);
        System.out.println();

        // STEP별 테이블 추출
        Map<String, TablesInfo> stepTables = stepParser.extractTablesByStep(sql);

        // 포맷팅된 결과 출력
        String formatted = stepParser.formatStepTables(stepTables);
        System.out.println(formatted);

        // 특정 STEP 조회 테스트
        System.out.println("--- Specific STEP Query Test ---");
        TablesInfo step005 = stepParser.extractTablesForStep(sql, "STEP005");
        if (step005 != null) {
            System.out.println("\nSTEP005 Sources: " + step005.getSources());
            System.out.println("STEP005 Targets: " + step005.getTargets());
        }

        // 전체 테이블 추출 테스트
        System.out.println("\n--- All Tables (without STEP separation) ---");
        TablesInfo allTables = stepParser.extractAllTables(sql);
        System.out.println("All Sources: " + allTables.getSources());
        System.out.println("All Targets: " + allTables.getTargets());
    }
}

