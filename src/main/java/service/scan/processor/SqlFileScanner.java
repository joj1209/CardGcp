package service.scan.processor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 디렉토리 순회 스캐너
 */
public class SqlFileScanner {
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
        final int[] cnt = {0};
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().toLowerCase().endsWith(ext)) {
                    try {
                        processor.processFile(file);
                        cnt[0]++;
                    } catch (Exception e) {
                        System.err.println("[에러] 파일 처리 실패: " + file.toAbsolutePath());
                        e.printStackTrace(System.err);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return cnt[0];
    }
}

