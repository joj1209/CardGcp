package service.queryParser.writer;

import service.queryParser.vo.TablesInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class TargetTableCsvWriter {
    private final Path outputPath;
    private final Charset charset;
    private final Map<String, TableMapping> tableMappings;

    public TargetTableCsvWriter(Path outputPath, Charset charset) {
        this.outputPath = outputPath;
        this.charset = charset;
        this.tableMappings = new TreeMap<>();
    }

    public void addRecord(String fileName, TablesInfo tablesInfo) {
        Set<String> sourceTables = tablesInfo.getSortedSources();
        Set<String> targetTables = tablesInfo.getSortedTargets();

        for (String targetTable : targetTables) {
            tableMappings.putIfAbsent(targetTable, new TableMapping(targetTable));
            TableMapping mapping = tableMappings.get(targetTable);
            mapping.addProgram(fileName, sourceTables);
        }
    }

    public void write() throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, charset,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            if (charset.name().equalsIgnoreCase("UTF-8")) {
                writer.write('\ufeff');
            }

            writer.write("Program,Target Table,Source Tables");
            writer.newLine();

            for (TableMapping mapping : tableMappings.values()) {
                writeMapping(writer, mapping);
            }

            writer.flush();
        }
    }

    private void writeMapping(BufferedWriter writer, TableMapping mapping) throws IOException {
        String targetTable = mapping.getTargetTable();

        for (Map.Entry<String, Set<String>> entry : mapping.getProgramMappings().entrySet()) {
            String program = entry.getKey();
            Set<String> sourceTables = entry.getValue();

            String sourceTablesStr = joinTables(sourceTables);

            writer.write(escapeCsv(program) + "," +
                        escapeCsv(targetTable) + "," +
                        escapeCsv(sourceTablesStr));
            writer.newLine();
        }
    }

    private String joinTables(Set<String> tables) {
        if (tables == null || tables.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = tables.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append("; ");
            }
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }

    public int getTableCount() {
        return tableMappings.size();
    }

    private static class TableMapping {
        private final String targetTable;
        private final Map<String, Set<String>> programMappings;

        public TableMapping(String targetTable) {
            this.targetTable = targetTable;
            this.programMappings = new TreeMap<>();
        }

        public void addProgram(String program, Set<String> sourceTables) {
            programMappings.putIfAbsent(program, new TreeSet<>());
            if (sourceTables != null) {
                programMappings.get(program).addAll(sourceTables);
            }
        }

        public String getTargetTable() {
            return targetTable;
        }

        public Map<String, Set<String>> getProgramMappings() {
            return programMappings;
        }
    }
}

