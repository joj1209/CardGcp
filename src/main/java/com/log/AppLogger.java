package com.log;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 순수 Java 로그 유틸리티 (외부 라이브러리 불필요)
 * 콘솔과 파일에 동시 출력
 */
public class AppLogger {

    private final String className;
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 로그 설정
    private static final String LOG_DIR = "D:/11. Project/11. DB_OUT3/logs";
    private static final boolean ENABLE_FILE_LOG = true;
    private static final boolean ENABLE_CONSOLE_LOG = true;

    // 로그 레벨
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    private static Level currentLevel = Level.INFO;
    private static final Lock lock = new ReentrantLock();

    /**
     * Private 생성자
     */
    private AppLogger(Class<?> clazz) {
        this.className = clazz.getSimpleName();
        initLogDirectory();
    }

    /**
     * 로거 인스턴스 생성
     */
    public static AppLogger getLogger(Class<?> clazz) {
        return new AppLogger(clazz);
    }

    /**
     * 로그 레벨 설정
     */
    public static void setLevel(Level level) {
        currentLevel = level;
    }

    /**
     * 로그 디렉토리 초기화
     */
    private void initLogDirectory() {
        if (ENABLE_FILE_LOG) {
            try {
                Files.createDirectories(Paths.get(LOG_DIR));
            } catch (IOException e) {
                System.err.println("로그 디렉토리 생성 실패: " + e.getMessage());
            }
        }
    }

    // ========== 기본 로그 메소드 ==========

    public void debug(String message) {
        log(Level.DEBUG, message, null);
    }

    public void debug(String format, Object... args) {
        log(Level.DEBUG, String.format(format, args), null);
    }

    public void info(String message) {
        log(Level.INFO, message, null);
    }

    public void info(String format, Object... args) {
        log(Level.INFO, String.format(format, args), null);
    }

    public void warn(String message) {
        log(Level.WARN, message, null);
    }

    public void warn(String format, Object... args) {
        log(Level.WARN, String.format(format, args), null);
    }

    public void warn(String message, Throwable t) {
        log(Level.WARN, message, t);
    }

    public void error(String message) {
        log(Level.ERROR, message, null);
    }

    public void error(String format, Object... args) {
        log(Level.ERROR, String.format(format, args), null);
    }

    public void error(String message, Throwable t) {
        log(Level.ERROR, message, t);
    }

    /**
     * 핵심 로그 메소드
     */
    private void log(Level level, String message, Throwable t) {
        if (level.ordinal() < currentLevel.ordinal()) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String threadName = Thread.currentThread().getName();
        String logMessage = String.format("%s [%s] %-5s %s - %s",
            timestamp, threadName, level, className, message);

        lock.lock();
        try {
            // 콘솔 출력
            if (ENABLE_CONSOLE_LOG) {
                if (level == Level.ERROR) {
                    System.err.println(logMessage);
                    if (t != null) {
                        t.printStackTrace(System.err);
                    }
                } else {
                    System.out.println(logMessage);
                }
            }

            // 파일 출력
            if (ENABLE_FILE_LOG) {
                writeToFile(logMessage, t, level == Level.ERROR);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 로그 파일에 기록
     */
    private void writeToFile(String logMessage, Throwable t, boolean isError) {
        String date = LocalDateTime.now().format(DATE_FORMATTER);
        String fileName = isError ?
            String.format("application-error-%s.log", date) :
            String.format("application-%s.log", date);

        Path logFile = Paths.get(LOG_DIR, fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(logFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            writer.write(logMessage);
            writer.newLine();

            // 예외 스택 트레이스 출력
            if (t != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                writer.write(sw.toString());
            }

        } catch (IOException e) {
            System.err.println("로그 파일 쓰기 실패: " + e.getMessage());
        }
    }

    // ========== 프로그램 실행 관련 ==========

    public void start(String programName) {
        String sep = "=".repeat(80);
        info(sep);
        info("프로그램 시작: %s", programName);
        info("시작 시간: %s", LocalDateTime.now().format(TIME_FORMATTER));
        info(sep);
    }

    public void end(String programName) {
        String sep = "=".repeat(80);
        info(sep);
        info("프로그램 종료: %s", programName);
        info("종료 시간: %s", LocalDateTime.now().format(TIME_FORMATTER));
        info(sep);
    }

    public void end(String programName, int processedCount) {
        String sep = "=".repeat(80);
        info(sep);
        info("프로그램 종료: %s", programName);
        info("처리 건수: %d", processedCount);
        info("종료 시간: %s", LocalDateTime.now().format(TIME_FORMATTER));
        info(sep);
    }

    // ========== 파일 처리 관련 ==========

    public void fileStart(String fileName) {
        info("파일 처리 시작: %s", fileName);
    }

    public void fileEnd(String fileName, int lineCount) {
        info("파일 처리 완료: %s (%d건)", fileName, lineCount);
    }

    public void fileError(String fileName, Throwable t) {
        error("파일 처리 에러: %s", fileName);
        if (t != null) {
            error("", t);
        }
    }

    // ========== SQL 처리 관련 ==========

    public void sqlScanStart(String directory) {
        info("SQL 파일 스캔 시작: %s", directory);
    }

    public void sqlScanEnd(int fileCount) {
        info("SQL 파일 스캔 완료: %d개 파일", fileCount);
    }

    public void tableExtracted(String fileName, int sourceCount, int targetCount) {
        info("테이블 추출 - 파일: %s, Source: %d개, Target: %d개",
            fileName, sourceCount, targetCount);
    }

    // ========== 진행률 ==========

    public void progress(int current, int total) {
        if (total > 0) {
            int percent = (current * 100) / total;
            info("진행률: %d/%d (%d%%)", current, total, percent);
        }
    }

    public void step(int stepNumber, String description) {
        info("STEP %d: %s", stepNumber, description);
    }

    // ========== 유틸리티 ==========

    public void separator() {
        info("-".repeat(80));
    }

    public void separator(String title) {
        String line = "-".repeat(80);
        info(line);
        info(title);
        info(line);
    }
}


