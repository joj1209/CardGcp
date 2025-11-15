package service.scan;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ?뚯씪 ?쎄린 ?좏떥由ы떚
 */
public class FileReaderUtil {

    private static final Charset INPUT_CHARSET = Charset.forName("MS949");

    /**
     * SQL ?뚯씪 ?쎄린
     */
    public String readFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        CharsetDecoder dec = INPUT_CHARSET.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return dec.decode(ByteBuffer.wrap(bytes)).toString();
    }
}

