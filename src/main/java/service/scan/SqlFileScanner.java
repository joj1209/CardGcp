package service.scan;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * SQL ?뚯씪 ?ㅼ틦??- ?붾젆?좊━ ?쒗쉶 ?대떦
 */
public class SqlFileScanner {

    private static final String TARGET_EXT = ".sql";
    private final SqlFileProcessor processor;

    public SqlFileScanner(SqlFileProcessor processor) {
        this.processor = processor;
    }

    /**
     * ?붾젆?좊━ ??紐⑤뱺 SQL ?뚯씪 ?ㅼ틪
     */
    public int scanDirectory(Path srcRoot) throws IOException {
        final int[] count = {0};

        Files.walkFileTree(srcRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {

                if (file.getFileName().toString().toLowerCase().endsWith(TARGET_EXT)) {
                    try {
                        processor.processFile(file);
                        count[0]++;
                    } catch (Exception e) {
                        System.err.println("[?먮윭] ?뚯씪 泥섎━ ?ㅽ뙣: " + file.toAbsolutePath());
                        e.printStackTrace(System.err);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return count[0];
    }
}

