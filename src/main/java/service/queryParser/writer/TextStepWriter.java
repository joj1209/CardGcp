package service.queryParser.writer;

import service.queryParser.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TextStepWriter {

    private final Path outputDir;
    private final Charset charset;

    public TextStepWriter(Path outputDir, Charset charset) {
        this.outputDir = outputDir;
        this.charset = charset;
    }

    public Path writeStepTables(Path inputDir, Path file, Map<String, TablesInfo> stepTables) throws IOException {
        System.out.println("[StepWriter] Processing step table info: " + file.getFileName());
        String relativeName = buildOutputName(inputDir, file);
        System.out.println("[StepWriter] Generated output filename: " + relativeName);
        System.out.println("[StepWriter] Total steps: " + stepTables.size());
        return writeStepTables(relativeName, stepTables);
    }

    public Path writeStepTables(String relativeFile, Map<String, TablesInfo> stepTables) throws IOException {
        String content = formatStepTables(stepTables);
        return write(relativeFile, content);
    }

    private String formatStepTables(Map<String, TablesInfo> stepTables) {
        StringBuilder sb = new StringBuilder();

        stepTables.forEach((stepName, info) -> {
            sb.append("=".repeat(60)).append("\n");
            sb.append(stepName).append("\n");
            sb.append("=".repeat(60)).append("\n\n");

            sb.append("[Source Tables]\n");
            if (info.getSortedSources().isEmpty()) {
                sb.append("(No source tables)\n");
            } else {
                info.getSortedSources().forEach(t -> sb.append(t).append("\n"));
            }

            sb.append("\n[Target Tables]\n");
            if (info.getSortedTargets().isEmpty()) {
                sb.append("(No target tables)\n");
            } else {
                info.getSortedTargets().forEach(t -> sb.append(t).append("\n"));
            }

            sb.append("\n");
        });

        return sb.toString();
    }

    private Path write(String relativeFile, String content) throws IOException {
        Path target = outputDir.resolve(relativeFile);
        System.out.println("[StepWriter] Creating output directory: " + target.getParent());
        Files.createDirectories(target.getParent());
        System.out.println("[StepWriter] Writing file: " + target.getFileName());
        Path result = Files.write(target, content.getBytes(charset));
        System.out.println("[StepWriter] File write completed: " + target);
        return result;
    }

    private String buildOutputName(Path inputDir, Path file) {
        Path relative = inputDir.relativize(file);
        String name = relative.toString().replace("\\", "/");
        return name.replaceAll("\\.sql$", "_step_tables.txt");
    }
}

