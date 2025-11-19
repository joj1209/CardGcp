package test;

import java.com.log.AppLogger;

public class LoggerTest {
    private static final AppLogger log = AppLogger.getLogger(LoggerTest.class);

    public static void main(String[] args) {
        log.start("로그 테스트");

        log.info("INFO 레벨 테스트");
        log.debug("DEBUG 레벨 테스트");
        log.warn("WARN 레벨 테스트");

        log.step(1, "첫 번째 단계");
        log.step(2, "두 번째 단계");

        log.fileStart("test.sql");
        log.fileEnd("test.sql", 100);

        log.tableExtracted("test.sql", 5, 3);

        log.progress(50, 100);

        log.separator("구분선 테스트");

        log.end("로그 테스트", 10);
    }
}

