package service.scanSourceTarget.scan.processor;

import service.scanSourceTarget.scan.model.TablesInfo;
import service.scanSourceTarget.scan.io.FileReaderUtil;
import service.scanSourceTarget.scan.io.ReportGenerator;
import service.scanSourceTarget.scan.parser.TableExtractor;

import java.io.IOException;
import java.nio.file.*;

/**
 * SQL 파일 처리 파사드
 */
public class SqlFileProcessor {
    private final Path srcRoot;
    private final Path outRoot;
    private final TableExtractor extractor;
    private final FileReaderUtil reader;
    private final ReportGenerator reporter;

    public SqlFileProcessor(Path srcRoot, Path outRoot) {
        this(srcRoot, outRoot, new TableExtractor(), new FileReaderUtil(), new ReportGenerator());
    }

    public SqlFileProcessor(Path srcRoot, Path outRoot, TableExtractor extractor, FileReaderUtil reader, ReportGenerator reporter) {
        this.srcRoot = srcRoot;
        this.outRoot = outRoot;
        this.extractor = extractor;
        this.reader = reader;
        this.reporter = reporter;
    }

    public void processFile(Path sqlFile) throws IOException {
        String content = reader.readFile(sqlFile);
        TablesInfo tables = extractor.extractTables(content);
        String report = reporter.buildReport(sqlFile, tables);
        Path outFile = resolveOutFile(sqlFile);
        write(outFile, report);
        System.out.println(report);
    }

    private Path resolveOutFile(Path sqlFile) throws IOException {
        Path rel = srcRoot.relativize(sqlFile);
        String name = rel.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        Path target = (rel.getParent() == null)
                ? outRoot.resolve(base + ".source_target.txt")
                : outRoot.resolve(rel.getParent()).resolve(base + ".source_target.txt");
        Files.createDirectories(target.getParent());
        return target;
    }

    private void write(Path out, String s) throws IOException {
        Files.write(out, s.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
