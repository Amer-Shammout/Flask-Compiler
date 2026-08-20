package generator;

import AST.ASTNode;
import AST.Program;
import AST.flask.visitor.ProgramVisitor;
import AST.template.TemplateVisitor;
import antlr.FlaskLexer;
import antlr.FlaskParser;
import antlr.TemplateLexer;
import antlr.TemplateParser;
import generator.context.ContextData;
import generator.context.PythonContextEvaluator;
import generator.template.TemplateRenderer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * End-to-end code generation:
 * Flask AST → ContextData → Jinja AST → HTML under {@code output/},
 * plus analysis artifacts under {@code compiler_output/}.
 */
public final class GenerationPipeline {

    public record Result(Path outputDir, Path compilerOutputDir, Map<String, Path> generatedHtml) {
    }

    private final Path flaskSource;
    private final Path templateDirectory;
    private final Path outputDir;
    private final Path compilerOutputDir;

    public GenerationPipeline(Path flaskSource, Path templateDirectory, Path outputDir, Path compilerOutputDir) {
        this.flaskSource = flaskSource;
        this.templateDirectory = templateDirectory;
        this.outputDir = outputDir;
        this.compilerOutputDir = compilerOutputDir;
    }

    public Result run() throws Exception {
        OutputWriter writer = new OutputWriter(outputDir, compilerOutputDir);
        writer.ensureDirectories();
        writer.log("Flask source: " + flaskSource.toAbsolutePath().normalize());
        writer.log("Template dir: " + templateDirectory.toAbsolutePath().normalize());

        Program flaskProgram = parseFlaskHeadless(flaskSource);
        writer.log("Parsed Flask AST successfully");

        // --- compiler_output: Python AST ---
        Path astPython = AstJsonExporter.write(flaskProgram, compilerOutputDir.resolve("ast_python.json"));
        writer.log("Wrote " + astPython.getFileName());

        // --- compiler_output: semantic report (artifact; generation does not use Symbol Table) ---
        Path semanticReport = SemanticReportWriter.writeFlaskReport(
                flaskProgram, flaskSource, compilerOutputDir.resolve("semantic_report.txt"));
        writer.log("Wrote " + semanticReport.getFileName());

        Map<String, Path> generated = new LinkedHashMap<>();
        Map<String, ASTNode> templateAsts = new LinkedHashMap<>();

        generateOne(writer, "products.html",
                PythonContextEvaluator.forProductsList(flaskProgram), generated, templateAsts);
        generateOne(writer, "product_details.html",
                PythonContextEvaluator.forProductDetails(flaskProgram, Optional.empty()), generated, templateAsts);
        generateOne(writer, "add_product.html",
                PythonContextEvaluator.forAddProduct(flaskProgram), generated, templateAsts);
        // Static snapshot only — the interactive Mode 5 server (GenerationServer) renders this
        // per-request with the actually-requested product's data.
        generateOne(writer, "edit_product.html",
                PythonContextEvaluator.forProductDetails(flaskProgram, Optional.empty()), generated, templateAsts);

        // --- compiler_output: Jinja/Template ASTs ---
        Path astJinja = AstJsonExporter.writeNamed(templateAsts, compilerOutputDir.resolve("ast_jinja.json"));
        writer.log("Wrote " + astJinja.getFileName() + " (" + templateAsts.size() + " templates)");

        // Supporting files are copied as-is (not analyzed/generated)
        if (Files.isRegularFile(flaskSource)) {
            writer.copySupportFile(flaskSource);
        }
        Path styleCss = templateDirectory.resolve("style.css");
        if (Files.isRegularFile(styleCss)) {
            writer.copySupportFile(styleCss);
        }
        Path scriptJs = templateDirectory.resolve("script.js");
        if (Files.isRegularFile(scriptJs)) {
            writer.copySupportFile(scriptJs);
        }

        Path logPath = writer.writeGenerationLog();
        System.out.println("generation_log → " + logPath.toAbsolutePath().normalize());

        return new Result(outputDir, compilerOutputDir, generated);
    }

    private void generateOne(
            OutputWriter writer,
            String templateFileName,
            ContextData context,
            Map<String, Path> generated,
            Map<String, ASTNode> templateAsts
    ) throws Exception {
        Path templatePath = templateDirectory.resolve(templateFileName);
        if (!Files.isRegularFile(templatePath)) {
            writer.log("SKIP missing template: " + templatePath);
            return;
        }

        String source = Files.readString(templatePath);
        ASTNode templateAst = parseTemplateHeadless(source);
        templateAsts.put(templateFileName, templateAst);

        String html = new TemplateRenderer(context, templateDirectory).render(templateAst);
        // Generated files in output/ are opened as local files → use relative CSS path
        html = html.replace("href=\"/style.css\"", "href=\"style.css\"");
        Path out = writer.writeHtml(templateFileName, html);
        writer.log("Context keys for " + templateFileName + ": " + context.keys());
        generated.put(templateFileName, out);
    }

    public static Program parseFlaskHeadless(Path path) throws Exception {
        FlaskLexer lexer = new FlaskLexer(CharStreams.fromPath(path));
        FlaskParser parser = new FlaskParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.prog();
        return new ProgramVisitor().visitProg((FlaskParser.ProgContext) tree);
    }

    public static ASTNode parseTemplateHeadless(String source) {
        TemplateLexer lexer = new TemplateLexer(CharStreams.fromString(source));
        TemplateParser parser = new TemplateParser(new CommonTokenStream(lexer));
        return new TemplateVisitor().visitTemplateRoot((TemplateParser.TemplateRootContext) parser.template());
    }
}
