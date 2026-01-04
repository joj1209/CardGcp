package service.fileUtil.job;

import service.fileUtil.reader.SqlReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UtilJob {
    static SqlReader reader = new SqlReader();

    public static void main(String[] args) throws IOException {
        System.out.println("------- UtilJob started -------");

        Path path = Paths.get(args[0]);

        reader.run(path);
    }
}