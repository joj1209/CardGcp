package service.scanSourceTarget.analyze;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SqlFileLoader {
    public static List<String> loadSqlFiles(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".sql") || p.toString().endsWith(".bq"))
                    .map(SqlFileLoader::readFileContent)
                    .collect(Collectors.toList());
        }
    }

    private static String readFileContent(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading file: " + path, e);
        }
    }
}
