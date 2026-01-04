package service.fileUtil.common;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface FileProcessor {
    void process(Path file) throws IOException;
}

