package service.scanSourceTarget.analyze.sql;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class SqlFileExecutor {

    public QueryResult execute(Path sqlFile, Connection connection) throws IOException, SQLException {
        List<String> statements = loadStatements(sqlFile, StandardCharsets.UTF_8);
        List<String> headers = null;
        List<List<Object>> rows = new ArrayList<>();

        for (String statement : statements) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try (PreparedStatement ps = connection.prepareStatement(trimmed)) {
                boolean hasResult = ps.execute();
                if (!hasResult) {
                    continue;
                }
                try (ResultSet rs = ps.getResultSet()) {
                    if (headers == null) {
                        headers = extractHeaders(rs.getMetaData());
                    }
                    while (rs.next()) {
                        List<Object> row = new ArrayList<>(headers.size());
                        for (int i = 0; i < headers.size(); i++) {
                            row.add(rs.getObject(i + 1));
                        }
                        rows.add(row);
                    }
                }
            }
        }
        if (headers == null) {
            throw new IllegalStateException("No SELECT statements produced a result set.");
        }
        return new QueryResult(headers, rows);
    }

    private List<String> extractHeaders(ResultSetMetaData metaData) throws SQLException {
        List<String> headers = new ArrayList<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            headers.add(metaData.getColumnLabel(i));
        }
        return headers;
    }

    private List<String> loadStatements(Path path, Charset charset) throws IOException {
        String sql = Files.readString(path, charset);
        return splitStatements(sql);
    }

    private List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (char c : sql.toCharArray()) {
            if (c == '\'') {
                if (!inDoubleQuote) {
                    inSingleQuote = !inSingleQuote;
                }
            } else if (c == '"') {
                if (!inSingleQuote) {
                    inDoubleQuote = !inDoubleQuote;
                }
            }

            if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                statements.add(builder.toString());
                builder.setLength(0);
            } else {
                builder.append(c);
            }
        }

        String remaining = builder.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }
        return statements;
    }

    public record QueryResult(List<String> headers, List<List<Object>> rows) {
        public String headersAsString() {
            StringJoiner joiner = new StringJoiner(", ");
            headers.forEach(joiner::add);
            return joiner.toString();
        }
    }
}
