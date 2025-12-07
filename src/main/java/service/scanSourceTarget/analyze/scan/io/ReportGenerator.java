package service.scanSourceTarget.analyze.scan.io;

import service.scanSourceTarget.analyze.scan.model.TablesInfo;

import java.nio.file.Path;

/**
 * 리포트 문자열 생성기
 */
public class ReportGenerator {
    public String buildReport(Path sqlFile, TablesInfo tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("FILE: ").append(sqlFile.toAbsolutePath()).append("\n\n");

        if (tables.isEmpty()) {
            sb.append("(추출된 테이블 없음)\n");
            return sb.toString();
        }
        if (!tables.getTargets().isEmpty()) {
            sb.append("[Target Tables]\n");
            int i = 1;
            for (String t : tables.getTargets()) sb.append("  ").append(i++).append(". ").append(t).append("\n");
            sb.append("\n");
        }
        if (!tables.getSources().isEmpty()) {
            sb.append("[Source Tables]\n");
            int i = 1;
            for (String s : tables.getSources()) sb.append("  ").append(i++).append(". ").append(s).append("\n");
        }
        return sb.toString();
    }
}
