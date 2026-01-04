package service.fileUtil.writer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class SqlWriter {

    public void write(Path outputFile, String content, Charset charset) throws IOException {
        // 출력 파일의 부모 디렉토리 생성
        if (outputFile.getParent() != null && !Files.exists(outputFile.getParent())) {
            Files.createDirectories(outputFile.getParent());
        }

        // 파일에 내용 쓰기
        Files.writeString(outputFile, content, charset);
    }

    public void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }
}
