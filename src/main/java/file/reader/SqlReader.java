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

/**
 * 지정한 문자셋으로 SQL 파일을 읽어들이는 유틸리티 클래스입니다.
 * 다양한 인코딩을 다루면서 안전하게 내용을 가져올 때 사용합니다.
 */
public class SqlReader {
    private static final Path DEFAULT_INPUT_DIR = Paths.get("D:", "11. Project", "11. DB", "BigQuery");
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final Charset DEFAULT_CHARSET = UTF8;

    private final Charset charset;

    /**
     * 기본 생성자: 문자셋을 UTF-8로 설정합니다.
     */
    public SqlReader() {
        this(DEFAULT_CHARSET);
    }

    /**
     * 사용자 정의 문자셋을 지정할 수 있는 생성자입니다.
     *
     * @param charset 사용할 문자셋
     */
    public SqlReader(Charset charset) {
        this.charset = charset;
    }

    /**
     * 설정된 문자셋으로 SQL 파일 전체를 읽어들이며 깨지는 문자는 대체합니다.
     *
     * @param file 읽을 파일 경로
     * @return 파일 내용 문자열
     * @throws IOException 파일 읽기 중 문제가 발생한 경우
     */
    public String readFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        CharsetDecoder dec = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return dec.decode(ByteBuffer.wrap(bytes)).toString();
    }

    /**
     * 기본 입력 디렉토리에 상대 경로로 지정된 파일을 읽어들입니다.
     *
     * @param relativeFile 읽을 파일의 상대 경로
     * @return 파일 내용 문자열
     * @throws IOException 파일 읽기 중 문제가 발생한 경우
     */
    public String readFile(String relativeFile) throws IOException {
        return readFile(DEFAULT_INPUT_DIR.resolve(relativeFile));
    }

    /**
     * 입력 디렉터리를 순회하며 SQL 파일을 읽고 핸들러에 전달합니다.
     */
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
