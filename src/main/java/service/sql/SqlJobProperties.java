package service.sql;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SqlJobProperties {

    private String jdbcUrl;
    private String username;
    private String password;
    private String scriptPath;
    private String outputDir;
    private String excelName;

    public static SqlJobProperties fromEnv() {
        SqlJobProperties props = new SqlJobProperties();
        props.jdbcUrl = System.getProperty("app.sql.jdbc-url",
            System.getenv().getOrDefault("APP_SQL_JDBC_URL", ""));
        props.username = System.getProperty("app.sql.username",
            System.getenv().getOrDefault("APP_SQL_USERNAME", ""));
        props.password = System.getProperty("app.sql.password",
            System.getenv().getOrDefault("APP_SQL_PASSWORD", ""));
        props.scriptPath = System.getProperty("app.sql.script-path",
            System.getenv().getOrDefault("APP_SQL_SCRIPT_PATH", ""));
        props.outputDir = System.getProperty("app.sql.output-dir",
            System.getenv().getOrDefault("APP_SQL_OUTPUT_DIR", ""));
        props.excelName = System.getProperty("app.sql.excel-name",
            System.getenv().getOrDefault("APP_SQL_EXCEL_NAME", "result.xlsx"));
        return props;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getExcelName() {
        return excelName;
    }

    public void setExcelName(String excelName) {
        this.excelName = excelName;
    }

    public Path resolveScriptPath() {
        return Paths.get(scriptPath);
    }

    public Path resolveOutputFile() {
        String fileName = excelName != null && !excelName.trim().isEmpty() ? excelName : "result.xlsx";
        return Paths.get(outputDir, fileName);
    }

    public void validate() {
        if (isBlank(jdbcUrl)) {
            throw new IllegalArgumentException("JDBC URL is required");
        }
        if (isBlank(username)) {
            throw new IllegalArgumentException("DB username is required");
        }
        if (isBlank(scriptPath)) {
            throw new IllegalArgumentException("SQL script path is required");
        }
        Path script = resolveScriptPath();
        if (!Files.isRegularFile(script)) {
            throw new IllegalArgumentException("Script file not found: " + script);
        }
        if (isBlank(outputDir)) {
            throw new IllegalArgumentException("Output directory is required");
        }
        Path outDir = Paths.get(outputDir);
        if (!Files.isDirectory(outDir)) {
            try {
                Files.createDirectories(outDir);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot create output directory: " + outDir, e);
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
