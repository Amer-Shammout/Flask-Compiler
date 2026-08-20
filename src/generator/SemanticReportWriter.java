package generator;

import AST.Program;
import SymbolTable.FlaskSymbolTable;
import SymbolTable.FlaskSymbolTableBuilder;
import SymbolTable.SymbolTableRepository;
import SymbolTable.TemplateSymbolTable;
import semantic.analyzers.SemanticAnalysisPipeline;
import semantic.diagnostics.DiagnosticCollector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs Flask semantic analysis and writes {@code semantic_report.txt}.
 * (Generation itself does not depend on the Symbol Table; this report is an artifact.)
 */
public final class SemanticReportWriter {

    private SemanticReportWriter() {
    }

    public static Path writeFlaskReport(Program program, Path flaskSource, Path reportFile) throws IOException {
        DiagnosticCollector diagnostics = new DiagnosticCollector();
        StringBuilder report = new StringBuilder();
        report.append("Semantic Analysis Report\n");
        report.append("========================\n");
        report.append("Source: ").append(flaskSource.toAbsolutePath().normalize()).append('\n');
        report.append('\n');

        if (program == null) {
            report.append("No Flask program provided.\n");
        } else {
            FlaskSymbolTable flaskTable = new FlaskSymbolTable("flask-global", flaskSource.toString());
            SymbolTableRepository repository = new SymbolTableRepository(
                    flaskTable,
                    new TemplateSymbolTable("template-global", null)
            );
            FlaskSymbolTableBuilder flaskBuilder = new FlaskSymbolTableBuilder(repository);
            flaskBuilder.build(program);

            SemanticAnalysisPipeline pipeline = new SemanticAnalysisPipeline(repository, diagnostics, flaskBuilder);
            pipeline.analyzeFlaskOnly(program);

            report.append(diagnostics.toString());
            if (diagnostics.getErrorCount() == 0
                    && diagnostics.getWarningCount() == 0
                    && diagnostics.getInfoCount() == 0
                    && diagnostics.getHintCount() == 0) {
                report.append("\nNo diagnostics reported.\n");
            }
        }

        Files.createDirectories(reportFile.getParent());
        Files.writeString(reportFile, report.toString(), StandardCharsets.UTF_8);
        return reportFile;
    }
}
