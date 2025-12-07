package test;

import service.file.parser.TableParser;
import service.file.vo.TablesInfo;

public class TestMergeExtraction {
    public static void main(String[] args) {
        // 테스트 1: MERGE INTO 구문
        String sql1 = "-- STEP002\n" +
                     "/* STEP002 */\n" +
                     "BEGIN\n" +
                     "  MERGE INTO DM.일별카드발급현황 AS T\n" +
                     "  USING DW.카드목록 AS S\n" +
                     "  ON T.id = S.id\n" +
                     "  WHEN MATCHED THEN UPDATE SET T.name = S.name;\n" +
                     "END;";

        TableParser parser = new TableParser();
        TablesInfo info1 = parser.extractTables(sql1);

        System.out.println("=== 테스트 1: MERGE INTO ===");
        System.out.println("\n[Target Tables]");
        for (String target : info1.getTargets()) {
            System.out.println(target);
        }

        System.out.println("\n[Source Tables]");
        for (String source : info1.getSources()) {
            System.out.println(source);
        }

        // 테스트 2: Oracle 조인 문법 (콤마로 구분)
        String sql2 = "-- STEP004\n" +
                     "/* STEP004 */\n" +
                     "BEGIN\n" +
                     "  INSERT INTO BM.`회사`\n" +
                     "  SELECT *\n" +
                     "     FROM DW.`회사목록` N1\n" +
                     "        , DW.`사무실` N2\n" +
                     "   WHERE N1.회사ID = N2.회사ID;\n" +
                     "END;";

        TablesInfo info2 = extractor.extractTables(sql2);

        System.out.println("\n\n=== 테스트 2: Oracle 조인 문법 ===");
        System.out.println("\n[Target Tables]");
        for (String target : info2.getTargets()) {
            System.out.println(target);
        }

        System.out.println("\n[Source Tables]");
        for (String source : info2.getSources()) {
            System.out.println(source);
        }
    }
}
