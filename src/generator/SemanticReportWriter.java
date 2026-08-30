package generator;

import AST.ASTNode;
import AST.flask.Program;
import AST.template.TemplateNode;
import SymbolTable.FlaskSymbolTable;
import SymbolTable.FlaskSymbolTableBuilder;
import SymbolTable.SymbolTableRepository;
import SymbolTable.TemplateSymbolTable;
import SymbolTable.TemplateSymbolTableBuilder;
import semantic.analyzers.SemanticAnalysisPipeline;
import semantic.diagnostics.DiagnosticCollector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SemanticReportWriter {

    private SemanticReportWriter() {
    }

    public static Path writeFlaskReport(Program program, Path flaskSource, Path reportFile) throws IOException {
        return writeFlaskReport(program, flaskSource, null, reportFile);
    }

    /**
     * Writes a semantic report for:
     * - the main Flask file
     * - all template HTML files under the given template directory
     */
    public static Path writeFlaskReport(Program program, Path flaskSource, Path templateDirectory, Path reportFile)
            throws IOException {

        StringBuilder report = new StringBuilder();
        report.append("Semantic Analysis Report\n");
        report.append("========================\n");
        report.append("Flask Source: ")
                .append(flaskSource == null ? "<null>" : flaskSource.toAbsolutePath().normalize())
                .append('\n');
        if (templateDirectory != null) {
            report.append("Template Directory: ")
                    .append(templateDirectory.toAbsolutePath().normalize())
                    .append('\n');
        }
        report.append('\n');

        FlaskSymbolTable flaskTable = null;
        if (program != null && flaskSource != null) {
            flaskTable = new FlaskSymbolTable("flask-global", flaskSource.toString());
            SymbolTableRepository repository = new SymbolTableRepository(flaskTable, new TemplateSymbolTable("template-global", null));
            FlaskSymbolTableBuilder flaskBuilder = new FlaskSymbolTableBuilder(repository);
            flaskBuilder.build(program);

            DiagnosticCollector flaskDiagnostics = new DiagnosticCollector();
            SemanticAnalysisPipeline flaskPipeline = new SemanticAnalysisPipeline(repository, flaskDiagnostics, flaskBuilder);
            flaskPipeline.analyzeFlaskOnly(program);

            report.append("FILE: ").append(flaskSource.getFileName()).append('\n');
            report.append(flaskDiagnostics.toString());

            if (isEmpty(flaskDiagnostics)) {
                report.append("\nNo diagnostics reported for Flask source.\n");
            }
            report.append('\n');
        } else {
            report.append("No Flask program provided.\n\n");
        }

        if (templateDirectory != null) {
            report.append("Template Files\n");
            report.append("--------------\n");

            if (!Files.isDirectory(templateDirectory)) {
                report.append("Template directory does not exist: ")
                        .append(templateDirectory.toAbsolutePath().normalize())
                        .append('\n');
            } else {
                List<Path> templateFiles = new ArrayList<>();
                try (var walk = Files.walk(templateDirectory)) {
                    templateFiles = walk
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".html"))
                            .sorted(Comparator.comparing(path -> path.toString()))
                            .toList();
                }

                if (templateFiles.isEmpty()) {
                    report.append("No .html template files were found in the input directory.\n");
                } else {
                    for (Path templateFile : templateFiles) {
                        report.append("\nFILE: ").append(templateFile.toAbsolutePath().normalize()).append('\n');
                        String templateSection = analyzeTemplateFile(program, flaskTable, templateFile);
                        report.append(templateSection);
                    }
                }
            }
        }

        Files.createDirectories(reportFile.getParent());
        Files.writeString(reportFile, report.toString(), StandardCharsets.UTF_8);
        return reportFile;
    }

    private static String analyzeTemplateFile(Program flaskProgram, FlaskSymbolTable flaskTable, Path templateFile) {
        StringBuilder section = new StringBuilder();

        if (flaskProgram == null || flaskTable == null) {
            section.append("Template analysis skipped: Flask program or Flask symbol table is unavailable.\n");
            return section.toString();
        }

        try {
            String source = Files.readString(templateFile, StandardCharsets.UTF_8);
            ASTNode root = GenerationPipeline.parseTemplateHeadless(source);

            if (!(root instanceof TemplateNode templateRoot)) {
                section.append("Template parse result is not a TemplateNode.\n");
                return section.toString();
            }

            SymbolTableRepository templateRepository = new SymbolTableRepository(
                    flaskTable,
                    new TemplateSymbolTable("template-global", templateFile.getFileName().toString())
            );

            TemplateSymbolTableBuilder templateBuilder = new TemplateSymbolTableBuilder(templateRepository);
            templateBuilder.buildTemplate(root);

            DiagnosticCollector templateDiagnostics = new DiagnosticCollector();
            SemanticAnalysisPipeline templatePipeline = new SemanticAnalysisPipeline(templateRepository, templateDiagnostics);

            templatePipeline.analyzeTemplateOnly(templateRoot, templateBuilder.getReferenceIndex());
            templatePipeline.bridgeTemplateWithFlask(flaskProgram, templateRoot, templateBuilder.getReferenceIndex());

            section.append(templateDiagnostics.toString());

            if (isEmpty(templateDiagnostics)) {
                section.append("\nNo diagnostics reported for this template.\n");
            }
        } catch (Exception ex) {
            section.append("Template parse/analysis failed: ").append(ex.getMessage()).append('\n');
        }

        return section.toString();
    }

    private static boolean isEmpty(DiagnosticCollector diagnostics) {
        return diagnostics.getErrorCount() == 0
                && diagnostics.getWarningCount() == 0
                && diagnostics.getInfoCount() == 0
                && diagnostics.getHintCount() == 0;
    }
}