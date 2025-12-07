package test;

import service.file.parser.TableParser;
import service.file.vo.TablesInfo;

public class TestCteExclusion {
    public static void main(String[] args) {
        String sql =
            "/* STEP005 */\n" +
            "BEGIN\n" +
            "    INSERT INTO DM.`요약01`\n" +
            "    WITH `모수`  AS\n" +
            "    (\n" +
            "    SELECT TIME_START\n" +
            "    , ENT_ID AS entId\n" +
            "    FROM DW.`기지국05` AS N1\n" +
            "    WHERE N1.COL1 = 'A'\n" +
            "    ) ,\n" +
            "    MOSU2 AS\n" +
            "    (\n" +
            "    SELECT TIME_START\n" +
            "    , ENT_ID AS entId\n" +
            "    FROM DW.`부서05` AS N1\n" +
            "    WHERE N1.COL1 = 'A'\n" +
            "    )\n" +
            "    SELECT TIME_START\n" +
            "    , ENT_ID AS entId\n" +
            "    FROM `모수`\n" +
            "END;";

        TableParser parser = new TableParser();
        TablesInfo info = parser.extractTables(sql);

        System.out.println("=== STEP005 테스트 ===");
        System.out.println("\n[Source Tables]");
        for (String source : info.getSources()) {
            System.out.println(source);
        }

        System.out.println("\n[Target Tables]");
        for (String target : info.getTargets()) {
            System.out.println(target);
        }

        System.out.println("\n=== 예상 결과 ===");
        System.out.println("Sources: DW.`기지국05`, DW.`부서05` (CTE 별칭 `모수`, MOSU2는 제외되어야 함)");
        System.out.println("Targets: DM.`요약01`");
    }
}

