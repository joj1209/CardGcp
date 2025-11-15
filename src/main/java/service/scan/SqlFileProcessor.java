package service.scan;

import java.io.IOException;
import java.nio.file.*;

/**
 * SQL ?뚯씪 泥섎━ ?대떦 ?대옒??
 */
public class SqlFileProcessor {

    private final Path srcRoot;
    private final Path outRoot;
    private final TableExtractor tableExtractor;
    private final FileReaderUtil fileReader;
    private final ReportGenerator reportGenerator;

    public SqlFileProcessor(Path srcRoot, Path outRoot) {
        this.srcRoot = srcRoot;
        this.outRoot = outRoot;
        this.tableExtractor = new TableExtractor();
        this.fileReader = new FileReaderUtil();
        this.reportGenerator = new ReportGenerator();
    }

    /**
     * 媛쒕퀎 SQL ?뚯씪 泥섎━
     */
    public void processFile(Path sqlFile) throws IOException {
        // 1) ?쎄린
        String content = fileReader.readFile(sqlFile);

        // 2) ?뚯씠釉?異붿텧
        TablesInfo tables = tableExtractor.extractTables(content);

        // 3) 異쒕젰 臾몄옄???앹꽦
        String report = reportGenerator.buildReport(sqlFile, tables);

        // 4) 異쒕젰 ?뚯씪 寃쎈줈 寃곗젙
        Path outFile = determineOutputFile(sqlFile);

        // 5) ?뚯씪 ???
        saveReport(outFile, report);

        // 6) 肄섏넄 異쒕젰
        System.out.println(report);
    }

    /**
     * 異쒕젰 ?뚯씪 寃쎈줈 寃곗젙
     */
    private Path determineOutputFile(Path sqlFile) throws IOException {
        Path relative = srcRoot.relativize(sqlFile);
        Path parent = relative.getParent();
        String fileName = relative.getFileName().toString();

        int dot = fileName.lastIndexOf('.');
        String base = (dot > 0 ? fileName.substring(0, dot) : fileName);
        String outFileName = base + ".source_target.txt";

        Path outFile;
        if (parent != null) {
            outFile = outRoot.resolve(parent).resolve(outFileName);
        } else {
            outFile = outRoot.resolve(outFileName);
        }

        // ?곸쐞 ?붾젆?좊━ ?앹꽦
        Path outParent = outFile.getParent();
        if (outParent != null && (!Files.exists(outParent) || !Files.isDirectory(outParent))) {
            Files.createDirectories(outParent);
        }

        return outFile;
    }

    /**
     * 由ы룷???뚯씪 ???
     */
    private void saveReport(Path outFile, String report) throws IOException {
        Files.write(outFile,
                report.getBytes("UTF-8"),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }
}

