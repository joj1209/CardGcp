package service.analyze;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {

//        Path sqlDir = Paths.get("src/main/resources/sample-queries");
        Path sqlDir = Paths.get("D:\\11. Project\\11. DB");
        List<String> queries = SqlFileLoader.loadSqlFiles(sqlDir);

        SqlAnalyzer analyzer = new SqlAnalyzer();

        for (String query : queries) {
            SqlStatistics stats = analyzer.analyze(query);
            System.out.println("Tables used: " + stats.getTablesUsed());
            if (stats.usesSelectAll()) {
                System.out.println("⚠️ Uses SELECT *");
            }
        }
    }
}

