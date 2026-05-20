package semantic.analyzers;

import AST.Program;
import AST.template.TemplateNode;
import SymbolTable.FlaskReferenceIndex;
import SymbolTable.SymbolTableRepository;
import SymbolTable.TemplateReferenceIndex;
import semantic.bridge.TemplateContextBridge;
import semantic.diagnostics.DiagnosticCollector;

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
        return analyze(flaskProgram, templateRoot, null, null);
    }

    public DiagnosticCollector analyze(
            Program flaskProgram,
            TemplateNode templateRoot,
            FlaskReferenceIndex flaskIndex,
            TemplateReferenceIndex templateIndex) {
        flaskAnalyzer.analyze(flaskProgram);
        templateAnalyzer.analyze(templateRoot);
        contextBridge.bridge(flaskProgram, templateRoot, flaskIndex, templateIndex);
        return diagnostics;
    }

    public TemplateContextBridge getContextBridge() {
        return contextBridge;
    }
}
