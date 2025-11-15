package service.scan;

import java.nio.file.Path;

/**
 * 由ы룷???앹꽦 ?대떦 ?대옒??
 */
public class ReportGenerator {

    /**
     * 由ы룷??臾몄옄???앹꽦 (肄섏넄/?뚯씪 怨듭슜)
     */
    public String buildReport(Path sqlFile, TablesInfo tables) {
        StringBuilder sb = new StringBuilder();

        sb.append("FILE: ").append(sqlFile.toAbsolutePath()).append("\n\n");

        if (tables.isEmpty()) {
            sb.append("(異붿텧???뚯씠釉??놁쓬)\n");
            return sb.toString();
        }

        if (!tables.getTargets().isEmpty()) {
            sb.append("[Target Tables]\n");
            int i = 1;
            for (String t : tables.getTargets()) {
                sb.append("  ").append(i++).append(". ").append(t).append("\n");
            }
            sb.append("\n");
        }

        if (!tables.getSources().isEmpty()) {
            sb.append("[Source Tables]\n");
            int i = 1;
            for (String s : tables.getSources()) {
                sb.append("  ").append(i++).append(". ").append(s).append("\n");
            }
        }

        return sb.toString();
    }
}

