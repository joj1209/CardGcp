package service.scan.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 파일 읽기 유틸리티 (기본 MS949)
 */
public class FileReaderUtil {
    private final Charset charset;

    public FileReaderUtil() {
        this(Charset.forName("MS949"));
    }

    public FileReaderUtil(Charset charset) {
        this.charset = charset;
    }

    public String readFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        CharsetDecoder dec = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return dec.decode(ByteBuffer.wrap(bytes)).toString();
    }
}

