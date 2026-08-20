package generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes generated HTML and companion files to {@code output/},
 * and writes a generation log under {@code compiler_output/}.
 */
public final class OutputWriter {

    private final Path outputDir;
    private final Path compilerOutputDir;
    private final List<String> logLines = new ArrayList<>();

    public OutputWriter(Path outputDir, Path compilerOutputDir) {
        this.outputDir = outputDir;
        this.compilerOutputDir = compilerOutputDir;
    }

    public void ensureDirectories() throws IOException {
        Files.createDirectories(outputDir);
        Files.createDirectories(compilerOutputDir);
    }

    public Path writeHtml(String fileName, String html) throws IOException {
        Path target = outputDir.resolve(fileName);
        Files.writeString(target, html, StandardCharsets.UTF_8);
        log("Wrote HTML: " + target.toAbsolutePath().normalize());
        return target;
    }

    /** Copy supporting files (app.py, style.css, …) without modifying them. */
    public Path copySupportFile(Path source) throws IOException {
        Path target = outputDir.resolve(source.getFileName().toString());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        log("Copied support file: " + source.getFileName() + " → " + target.toAbsolutePath().normalize());
        return target;
    }

    public void log(String message) {
        logLines.add(message);
    }

    public Path writeGenerationLog() throws IOException {
        Path logFile = compilerOutputDir.resolve("generation_log.txt");
        StringBuilder sb = new StringBuilder();
        sb.append("Generation log\n");
        sb.append("==============\n");
        for (String line : logLines) {
            sb.append(line).append('\n');
        }
        Files.writeString(logFile, sb.toString(), StandardCharsets.UTF_8);
        return logFile;
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public Path getCompilerOutputDir() {
        return compilerOutputDir;
    }
}
