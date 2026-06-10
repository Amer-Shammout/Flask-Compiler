package semantic.analyzers;

import AST.template.TemplateNode;
import SymbolTable.SymbolTableRepository;
import SymbolTable.TemplateReferenceIndex;
import SymbolTable.SymbolReference;
import AST.SourceRange;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.ErrorCode;
import semantic.type.TemplateTypeErrorChecker;

/**
 * Phase 2 analyzer for Jinja/Template-local semantics.
 *
 * Responsibilities:
 * - Template-local semantic diagnostics (locals, blocks, macro usage, shadowing)
 * - Template-internal name/type checks before cross-context bridging
 * - Emitting diagnostics through the shared collector
 *
 * IMPORTANT: Template symbol table MUST be built BEFORE calling analyze().
 * This class only checks scopes and types, it does NOT build the symbol table.
 */
public class TemplateSemanticAnalyzer {

    private final SymbolTableRepository repository;
    private final DiagnosticCollector diagnostics;
    private final TemplateTypeErrorChecker typeChecker;

    public TemplateSemanticAnalyzer(SymbolTableRepository repository, DiagnosticCollector diagnostics) {
        this.repository = repository;
        this.diagnostics = diagnostics;
        this.typeChecker = new TemplateTypeErrorChecker(repository, diagnostics);
    }

    /**
     * Analyze Template semantics.
     *
     * @param templateRoot Template AST root.
     */
    public void analyze(TemplateNode templateRoot,TemplateReferenceIndex referenceIndex) {
        if (templateRoot == null) {
            diagnostics.addDiagnostic(new Diagnostic(
                    null,
                    ErrorCode.H001_SUGGESTION,
                    "Template semantic analysis skipped: Template AST root is null.",
                    "Ensure template parsing succeeds before semantic analysis."
            ));
            return;
        }

        checkTemplateScopes(templateRoot, referenceIndex);
        checkTemplateTypes(templateRoot);
    }

    /**
     * Step 1: Check template scopes (reserved for future implementation).
     */
    private void checkTemplateScopes(TemplateNode templateRoot, TemplateReferenceIndex referenceIndex) {
        // TODO(Member 4): Implement template-local scope checks.
        if (referenceIndex == null) {
            return;
        }
        for (SymbolReference ref : referenceIndex.getUnresolvedReferences()) {
            String name = ref.getName();
            boolean definedInTemplate = referenceIndex.getDefinitions().stream().anyMatch(def -> name.equals(def.getName()));
            if (definedInTemplate) {
                SourceRange src = ref.getLocation();
                String message = String.format("Variable '%s' referenced outside its defining scope", name);
                String suggestion = "Move usage inside the block where it is defined (e.g., inside the for-loop) or define it in an outer scope.";
                diagnostics.reportScopeError(src, name, ErrorCode.E203_OUT_OF_SCOPE, message, suggestion);
            }
        }
    }

    /**
     * Step 2: Check template types (template-local, not cross-context).
     */
    private void checkTemplateTypes(TemplateNode templateRoot) {
        // Run template type checks (will use Flask symbol info if available)
        try {
            typeChecker.checkTemplate(templateRoot);
        } catch (Exception ex) {
            diagnostics.addDiagnostic(new Diagnostic(
                    null,
                    ErrorCode.E102_TYPE_ERROR,
                    "Error during template type checking: " + ex.getMessage(),
                    "Review the template code for type inconsistencies."
            ));
        }
    }
}
