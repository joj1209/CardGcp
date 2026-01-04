package service.fileUtil.processor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConvertEncoding {

    public String convert(Path inputFile, Charset fromCharset) throws IOException {
        return Files.readString(inputFile, fromCharset);
    }
}




