package semantic.analyzers;

import AST.Program;
import AST.template.TemplateNode;
import SymbolTable.SymbolTableRepository;
import semantic.bridge.TemplateContextBridge;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.ErrorCode;

/**
 * Orchestrates semantic phases in order:
 * 1) Flask semantic analysis
 * 2) Template semantic analysis
 * 3) Cross-context bridge analysis
 */
public class SemanticAnalysisPipeline {

    private final DiagnosticCollector diagnostics;
    private final FlaskSemanticAnalyzer flaskAnalyzer;
    private final TemplateSemanticAnalyzer templateAnalyzer;
    private final TemplateContextBridge contextBridge;

    public SemanticAnalysisPipeline(SymbolTableRepository repository, DiagnosticCollector diagnostics) {
        this.diagnostics = diagnostics;
        this.flaskAnalyzer = new FlaskSemanticAnalyzer(repository, diagnostics);
        this.templateAnalyzer = new TemplateSemanticAnalyzer(repository, diagnostics);
        this.contextBridge = new TemplateContextBridge(repository, diagnostics);
    }

    /**
     * Run all semantic phases.
     *
     * @param flaskProgram Flask AST root.
     * @param templateRoot Template AST root.
     * @return Shared diagnostics collector containing all emitted diagnostics.
     */
    public DiagnosticCollector analyze(Program flaskProgram, TemplateNode templateRoot) {
        flaskAnalyzer.analyze(flaskProgram);
        templateAnalyzer.analyze(templateRoot);

        runBridge(flaskProgram, templateRoot);
        return diagnostics;
    }

    private void runBridge(Program flaskProgram, TemplateNode templateRoot) {
        try {
            contextBridge.bridge(flaskProgram, templateRoot);
        } catch (UnsupportedOperationException ex) {
            diagnostics.addDiagnostic(new Diagnostic(
                null,
                ErrorCode.H001_SUGGESTION,
                "TemplateContextBridge.bridge(...) is not implemented yet.",
                "Implement bridge resolution to produce cross-context diagnostics."
            ));
        }
    }

    public TemplateContextBridge getContextBridge() {
        return contextBridge;
    }
}
