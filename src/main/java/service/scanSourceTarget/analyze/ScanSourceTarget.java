package service.scanSourceTarget.analyze;

import service.scanSourceTarget.analyze.scan.processor.SqlFileProcessor;
import service.scanSourceTarget.analyze.scan.processor.SqlFileScanner;

import java.nio.file.*;

/**
 * ScanSourceTarget - 메인 진입점
 * -----------------------------------------
 * - SRC_ROOT 아래의 *.sql 파일을 모두 스캔
 * - Source / Target 테이블 추출
 * - OUT_ROOT 아래 동일한 경로 구조로 .source_target.txt 생성
 * - INSERT /--/  INTO 패턴 자동 지원 (블록 주석 제거 방식)
 * - 하나의 테이블이 Source/Target 둘 다에 등장 가능 (요구사항 준수)
 * - 특정 파일에서 에러 발생해도 전체 스캔 절대 중단되지 않음
 * - JDK 1.7 호환
 */
public class ScanSourceTarget {
    // 입력 폴더
    private static final Path SRC_ROOT = Paths.get("D:\\11. Project\\11. DB");

    // 출력 폴더
    private static final Path OUT_ROOT = Paths.get("D:\\11. Project\\11. DB_OUT3");

    public static void main(String[] args) throws Exception {

        if (!Files.isDirectory(SRC_ROOT)) {
            throw new IllegalArgumentException("입력 폴더 없음: " + SRC_ROOT.toAbsolutePath());
        }

        Files.createDirectories(OUT_ROOT);

        // 각 역할별 클래스 생성
        SqlFileProcessor processor = new SqlFileProcessor(SRC_ROOT, OUT_ROOT);
        SqlFileScanner scanner = new SqlFileScanner(processor);

        // 스캔 실행
        int count = scanner.scanDirectory(SRC_ROOT);

        System.out.println("\n[완료] 스캔한 SQL 파일 수: " + count + "개");
    }
}
