package test;

import file.parser.TablePattern;
import file.vo.TablesInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * TablePattern 클래스 테스트
 */
public class TestTablePattern {
    public static void main(String[] args) throws IOException {
        String sqlPath = "D:\\11. Project\\11. DB\\BigQuery\\sample002.sql";
        String sql = new String(Files.readAllBytes(Paths.get(sqlPath)), "UTF-8");

        TablePattern pattern = new TablePattern();

        // 전체 테이블 추출
        System.out.println("=== 전체 SQL 테이블 추출 ===");
        TablesInfo allTables = pattern.extractTables(sql);
        printTables(allTables);

        // 스텝별 테이블 추출
        System.out.println("\n=== 스텝별 테이블 추출 ===");
        extractBySteps(sql, pattern);
    }

    private static void extractBySteps(String sql, TablePattern pattern) {
        String[] steps = sql.split("/\\*\\s*STEP\\d+\\s*\\*/");

        for (int i = 1; i < steps.length; i++) {
            System.out.println("\n--- STEP" + String.format("%03d", i) + " ---");
            TablesInfo stepTables = pattern.extractTables(steps[i]);
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

