package test;

import file.parser.TableParser;
import file.vo.TablesInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * TableParser 클래스 테스트
 */
public class TestTablePattern {
    public static void main(String[] args) throws IOException {
        String sqlPath = "D:\\11. Project\\11. DB\\BigQuery\\sample002.sql";
        String sql = new String(Files.readAllBytes(Paths.get(sqlPath)), "UTF-8");

        TableParser parser = new TableParser();

        // 전체 테이블 추출
        System.out.println("=== 전체 SQL 테이블 추출 ===");
        TablesInfo allTables = parser.extractTables(sql);
        printTables(allTables);

        // 스텝별 테이블 추출
        System.out.println("\n=== 스텝별 테이블 추출 ===");
        extractBySteps(sql, parser);
    }

    private static void extractBySteps(String sql, TableParser parser) {
        String[] steps = sql.split("/\\*\\s*STEP\\d+\\s*\\*/");

        for (int i = 1; i < steps.length; i++) {
            System.out.println("\n--- STEP" + String.format("%03d", i) + " ---");
            TablesInfo stepTables = parser.extractTables(steps[i]);
            printTables(stepTables);
        }
    }

    private static void printTables(TablesInfo info) {
        System.out.println("[Source Tables]");
        if (info.getSources().isEmpty()) {
            System.out.println("(No source tables)");
        } else {
            info.getSources().forEach(System.out::println);
        }

        System.out.println("\n[Target Tables]");
        if (info.getTargets().isEmpty()) {
            System.out.println("(No target tables)");
        } else {
            info.getTargets().forEach(System.out::println);
        }
    }
}

