package test;

import service.file.job.AppStepJob;

/**
 * AppStepJob 테스트
 *
 * 연결 구조 검증:
 * AppStepJob -> FileStepParserProcessor -> TableStepParser
 */
public class TestAppStepJob {
    public static void main(String[] args) {
        System.out.println("=== Testing AppStepJob ===");
        System.out.println("Connection: AppStepJob -> FileStepParserProcessor -> TableStepParser\n");

        // sample002.sql 파일로 테스트
        String sqlPath = "D:\\11. Project\\11. DB\\BigQuery\\sample002.sql";

        AppStepJob job = AppStepJob.createJob(sqlPath);
        job.execute();

        System.out.println("\n=== Test completed ===");
    }
}
