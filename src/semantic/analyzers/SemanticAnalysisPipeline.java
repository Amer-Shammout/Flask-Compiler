package semantic.analyzers;

import AST.flask.Program;
import AST.template.TemplateNode;
import SymbolTable.FlaskSymbolTableBuilder;
import SymbolTable.SymbolTableRepository;
import SymbolTable.TemplateReferenceIndex;
import semantic.bridge.TemplateContextBridge;
import semantic.diagnostics.DiagnosticCollector;

/**
 * Orchestrates semantic phases in order.
 */
public class SemanticAnalysisPipeline {

    private final SymbolTableRepository repository;
    private final DiagnosticCollector diagnostics;
    private final FlaskSemanticAnalyzer flaskAnalyzer;
    private final TemplateSemanticAnalyzer templateAnalyzer;
    private final TemplateContextBridge contextBridge;

    // Backward-compatible constructor (no builder)
    public SemanticAnalysisPipeline(SymbolTableRepository repository, DiagnosticCollector diagnostics) {
        this(repository, diagnostics, null);
    }

    // New constructor that accepts FlaskSymbolTableBuilder so we can run scope checks that need the reference index
    public SemanticAnalysisPipeline(SymbolTableRepository repository, DiagnosticCollector diagnostics, FlaskSymbolTableBuilder flaskBuilder) {
        this.repository = repository;
        this.diagnostics = diagnostics;
        this.flaskAnalyzer = new FlaskSemanticAnalyzer(repository, diagnostics, flaskBuilder);
        this.templateAnalyzer = new TemplateSemanticAnalyzer(repository, diagnostics);
        this.contextBridge = new TemplateContextBridge(repository, diagnostics);
    }

    public void analyzeFlaskOnly(Program program) {
        if (program == null) return;
        flaskAnalyzer.analyze(program);
    }

    public void analyzeTemplateOnly(TemplateNode templateRoot, TemplateReferenceIndex templateReferenceIndex) {
        if (templateRoot == null) return;
        templateAnalyzer.analyze(templateRoot, templateReferenceIndex);
    }

    public void bridgeTemplateWithFlask(Program flaskProgram, TemplateNode templateRoot, TemplateReferenceIndex templateReferenceIndex) {
        if (flaskProgram == null || templateRoot == null) return;
        if (templateReferenceIndex == null) {
            return;
        }
        contextBridge.bridge(flaskProgram, templateRoot, null, templateReferenceIndex);
    }

    public TemplateContextBridge getContextBridge() {
        return contextBridge;
    }
}