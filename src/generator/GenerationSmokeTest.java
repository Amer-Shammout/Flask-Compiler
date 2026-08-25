package generator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Smoke test for Step 8 — full {@code compiler_output/} artifacts.
 * <p>
 * In IntelliJ: right-click → Run 'GenerationSmokeTest.main()'
 * (Working directory = project root.)
 */
public final class GenerationSmokeTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Step 8: compiler_output artifacts ===\n");

        Path flask = Paths.get("src/Tests/FinalTests/app.py").toAbsolutePath().normalize();
        Path templates = Paths.get("src/Tests/FinalTests").toAbsolutePath().normalize();
        Path output = Paths.get("output").toAbsolutePath().normalize();
        Path compilerOutput = Paths.get("compiler_output").toAbsolutePath().normalize();

        new GenerationPipeline(flask, templates, output, compilerOutput).run();

        requireFile(compilerOutput.resolve("ast_python.json"));
        requireFile(compilerOutput.resolve("ast_jinja.json"));
        requireFile(compilerOutput.resolve("semantic_report.txt"));
        requireFile(compilerOutput.resolve("generation_log.txt"));
        requireFile(output.resolve("products.html"));

        String productsHtml = Files.readString(output.resolve("products.html"));
        if (!productsHtml.contains("nav")) {
            throw new AssertionError("products.html missing nav");
        }
        if (!productsHtml.contains("<style>")) {
            throw new AssertionError("products.html missing compiled <style> block");
        }
        if (!productsHtml.contains(".nav a {")) {
            throw new AssertionError("products.html: descendant selector lost in compiled CSS");
        }
        if (productsHtml.contains("rel=\"stylesheet\"")) {
            throw new AssertionError("products.html still references an external stylesheet");
        }

        String astPython = Files.readString(compilerOutput.resolve("ast_python.json"));
        if (!astPython.contains("\"type\": \"Program\"")) {
            throw new AssertionError("ast_python.json missing Program root");
        }

        String astJinja = Files.readString(compilerOutput.resolve("ast_jinja.json"));
        if (!astJinja.contains("products.html") || !astJinja.contains("HtmlDocument")) {
            throw new AssertionError("ast_jinja.json missing expected templates/nodes");
        }

        String semantic = Files.readString(compilerOutput.resolve("semantic_report.txt"));
        if (!semantic.contains("Semantic Analysis Report")) {
            throw new AssertionError("semantic_report.txt looks wrong");
        }

        System.out.println("compiler_output/");
        System.out.println("  ast_python.json       (" + Files.size(compilerOutput.resolve("ast_python.json")) + " bytes)");
        System.out.println("  ast_jinja.json        (" + Files.size(compilerOutput.resolve("ast_jinja.json")) + " bytes)");
        System.out.println("  semantic_report.txt   (" + Files.size(compilerOutput.resolve("semantic_report.txt")) + " bytes)");
        System.out.println("  generation_log.txt    (" + Files.size(compilerOutput.resolve("generation_log.txt")) + " bytes)");
        System.out.println();
        System.out.println("Step 8 OK — all four compiler_output files are present.");
    }

    private static void requireFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new AssertionError("Missing expected file: " + path);
        }
    }
}
