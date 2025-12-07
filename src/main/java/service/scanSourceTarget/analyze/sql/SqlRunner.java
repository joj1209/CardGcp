package service.scanSourceTarget.analyze.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class SqlRunner {

    private SqlRunner() {
    }

    public static void main(String[] args) {
        SqlJobProperties props = SqlJobProperties.fromEnv();
        props.validate();

        log("SQL Runner started");
        try (Connection connection = DriverManager.getConnection(
            props.getJdbcUrl(), props.getUsername(), props.getPassword())) {

            // SQL 파일 실행 후 결과를 CSV로 변환
            SqlFileExecutor executor = new SqlFileExecutor();
            SqlFileExecutor.QueryResult result = executor.execute(props.resolveScriptPath(), connection);
            log("Executed statements. Rows fetched: " + result.rows().size());

            ExcelResultExporter exporter = new ExcelResultExporter();
            exporter.export(result, props.resolveOutputFile());
            log("Excel exported to: " + props.resolveOutputFile());
        } catch (Exception e) {
            log("Execution failed: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    // 단순 시간 기반 로그 출력
    private static void log(String message) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.printf("%s [SQL-RUNNER] %s%n", ts, message);
    }
}
