package service.scan.processor;

import common.log.SimpleAppLogger;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 디렉토리 순회 스캐너
 */
public class SqlFileScanner {
    private static final SimpleAppLogger log = SimpleAppLogger.getLogger(SqlFileScanner.class);

    private final SqlFileProcessor processor;
    private final String ext;

    public SqlFileScanner(SqlFileProcessor processor) {
        this(processor, ".sql");
    }

    public SqlFileScanner(SqlFileProcessor processor, String targetExtension) {
        this.processor = processor;
        this.ext = targetExtension.toLowerCase();
    }

    public int scanDirectory(Path root) throws IOException {
        log.sqlScanStart(root.toString());

        final int[] cnt = {0};
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().toLowerCase().endsWith(ext)) {
                    try {
                        processor.processFile(file);
                        cnt[0]++;
                        if (cnt[0] % 10 == 0) {
                            log.progress(cnt[0], -1);
                        }
                    } catch (Exception e) {
                        log.fileError(file.getFileName().toString(), e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        log.sqlScanEnd(cnt[0]);
        return cnt[0];
    }
}

