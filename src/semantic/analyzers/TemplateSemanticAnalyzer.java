package semantic.analyzers;

import AST.template.TemplateNode;
import SymbolTable.SymbolTableRepository;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.ErrorCode;

/**
 * Phase analyzer for Jinja/Template-local semantics.
 *
 * Responsibilities:
 * - Template-local semantic diagnostics (locals, blocks, macro usage, shadowing)
 * - Template-internal name/type checks before cross-context bridging
 * - Emitting diagnostics through the shared collector
 */
public class TemplateSemanticAnalyzer {

    private final SymbolTableRepository repository;
    private final DiagnosticCollector diagnostics;

    public TemplateSemanticAnalyzer(SymbolTableRepository repository, DiagnosticCollector diagnostics) {
        this.repository = repository;
        this.diagnostics = diagnostics;
    }

    /**
     * Analyze Template semantics.
     *
     * @param templateRoot Template AST root.
     */
    public void analyze(TemplateNode templateRoot) {
        if (templateRoot == null) {
            diagnostics.addDiagnostic(new Diagnostic(
                null,
                ErrorCode.H001_SUGGESTION,
                "Template semantic analysis skipped: Template AST root is null.",
                "Ensure template parsing succeeds before semantic analysis."
            ));
            return;
        }

        buildTemplateSymbols(templateRoot);
        checkTemplateScopes(templateRoot);
        checkTemplateTypes(templateRoot);
    }

    private void buildTemplateSymbols(TemplateNode templateRoot) {
        // TODO(Member 4): Connect to TemplateSymbolTableBuilder when its API is finalized.
        // Current builder base signature accepts Program; template builder integration
        // should be aligned to accept TemplateNode root.
        if (repository.getTemplateGlobal() == null) {
            diagnostics.addDiagnostic(new Diagnostic(
                null,
                ErrorCode.H001_SUGGESTION,
                "Template global symbol table is missing.",
                "Initialize SymbolTableRepository with a TemplateSymbolTable instance."
            ));
        }
    }

    private void checkTemplateScopes(TemplateNode templateRoot) {
        // TODO(Member 4): Implement template-local scope checks.
    }

    private void checkTemplateTypes(TemplateNode templateRoot) {
        // TODO(Member 4): Implement template-local type checks using TypeKind.
    }
}
