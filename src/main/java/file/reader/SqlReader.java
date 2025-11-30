package file.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class SqlReader {
    private static final Path DEFAULT_INPUT_DIR = Paths.get("D:", "11. Project", "11. DB", "BigQuery");
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final Charset DEFAULT_CHARSET = UTF8;

    private final Charset charset;

    public SqlReader() {
        this(DEFAULT_CHARSET);
    }

    public SqlReader(Charset charset) {
        this.charset = charset;
    }

    public String readFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        CharsetDecoder dec = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return dec.decode(ByteBuffer.wrap(bytes)).toString();
    }

    public String readFile(String relativeFile) throws IOException {
        return readFile(DEFAULT_INPUT_DIR.resolve(relativeFile));
    }

    public void run(Path inputDir, SqlFileHandler handler) {
        try (Stream<Path> paths = Files.walk(inputDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .forEach(path -> handle(path, handler));
        } catch (IOException e) {
            throw new RuntimeException("입력 디렉터리 순회 중 오류", e);
        }
    }

    private void handle(Path path, SqlFileHandler handler) {
        try {
            handler.handle(path, readFile(path));
        } catch (IOException ex) {
            System.err.println("파일 읽기 실패: " + path + " - " + ex.getMessage());
        }
    }

    @FunctionalInterface
    public interface SqlFileHandler {
        void handle(Path path, String sql) throws IOException;
    }
}
