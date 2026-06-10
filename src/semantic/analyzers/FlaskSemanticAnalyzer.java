package semantic.analyzers;

import AST.Program;
import SymbolTable.FlaskSymbolTableBuilder;
import SymbolTable.SymbolTableRepository;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.ErrorCode;
import semantic.type.TypeErrorChecker;
import semantic.scope.UndefinedNameChecker;
import semantic.scope.ScopeCheckAnalyzer;

/**
 * Phase 1 analyzer for Flask/Python semantic checks.
 *
 * IMPORTANT: Symbol table MUST be built BEFORE calling analyze().
 */
public class FlaskSemanticAnalyzer {

    private final SymbolTableRepository repository;
    private final DiagnosticCollector diagnostics;
    private final TypeErrorChecker typeChecker;
    private final UndefinedNameChecker undefinedChecker;
    private final ScopeCheckAnalyzer scopeChecker; // may be null if builder not provided

    // Existing constructor (backward compatible) - no builder provided
    public FlaskSemanticAnalyzer(SymbolTableRepository repository, DiagnosticCollector diagnostics) {
        this(repository, diagnostics, null);
    }

    // New constructor: accept FlaskSymbolTableBuilder so we can run scope checks that rely on the reference index
    public FlaskSemanticAnalyzer(SymbolTableRepository repository, DiagnosticCollector diagnostics, FlaskSymbolTableBuilder builder) {
        this.repository = repository;
        this.diagnostics = diagnostics;
        this.typeChecker = new TypeErrorChecker(diagnostics, repository);
        this.undefinedChecker = new UndefinedNameChecker(repository, diagnostics);
        this.scopeChecker = builder != null ? new ScopeCheckAnalyzer(repository, diagnostics, builder) : null;
    }

    /**
     * Analyze Flask semantics.
     *
     * @param program Flask AST root.
     */
    public void analyze(Program program) {
        if (program == null) {
            diagnostics.addDiagnostic(new Diagnostic(
                    null,
                    ErrorCode.H001_SUGGESTION,
                    "Flask semantic analysis skipped: Program AST is null.",
                    "Ensure Flask parsing succeeds before semantic analysis."
            ));
            return;
        }

        // Step 1: Check undefined names/functions (E001 / E002)
        checkUndefinedError(program);

        // Step 2: Check scope violations (E202 / E203) using ScopeCheckAnalyzer if available
        checkScopeViolations();

        // Step 3: Type checking
        checkFlaskTypes(program);
    }

    private void checkUndefinedError(Program program) {
        undefinedChecker.checkFlaskScopes(program);
    }

    private void checkScopeViolations() {
        if (scopeChecker != null) {
            scopeChecker.analyze();
        }
    }

    private void checkFlaskTypes(Program program) {
        try {
            typeChecker.checkProgram(program);
        } catch (Exception ex) {
            diagnostics.addDiagnostic(new Diagnostic(
                    null,
                    ErrorCode.E102_TYPE_ERROR,
                    "Error during type checking: " + ex.getMessage(),
                    "Review the Flask code for type inconsistencies."
            ));
        }
    }
}