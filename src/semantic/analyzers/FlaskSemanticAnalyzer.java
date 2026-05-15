package semantic.analyzers;

import AST.Program;
import SymbolTable.FlaskSymbolTableBuilder;
import SymbolTable.SymbolTableRepository;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.ErrorCode;

/**
 * Phase analyzer for Flask/Python semantic checks.
 *
 * Responsibilities:
 * - Flask-local semantic diagnostics (scope/definition/type checks)
 * - Building/using Flask symbol table artifacts
 * - Emitting diagnostics through the shared collector
 *
 * This class is intentionally scaffolded to keep the pipeline runnable while
 * SymbolTable builders are still under implementation.
 */
public class FlaskSemanticAnalyzer {

    private final SymbolTableRepository repository;
    private final DiagnosticCollector diagnostics;

    public FlaskSemanticAnalyzer(SymbolTableRepository repository, DiagnosticCollector diagnostics) {
        this.repository = repository;
        this.diagnostics = diagnostics;
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

        buildFlaskSymbols(program);
        checkFlaskScopes(program);
        checkFlaskTypes(program);
    }

    private void buildFlaskSymbols(Program program) {
        try {
            FlaskSymbolTableBuilder builder = new FlaskSymbolTableBuilder(repository);
            builder.build(program);
        } catch (UnsupportedOperationException ex) {
            diagnostics.addDiagnostic(new Diagnostic(
                null,
                ErrorCode.H001_SUGGESTION,
                "Flask symbol table builder is not implemented yet.",
                "Implement FlaskSymbolTableBuilder.build(...) to enable full Flask semantic checks."
            ));
        }
    }

    private void checkFlaskScopes(Program program) {
        // TODO(Member 4): Implement scope checks:
        // - duplicate definitions
        // - use-before-definition
        // - out-of-scope access
    }

    private void checkFlaskTypes(Program program) {
        // TODO(Member 4): Implement Flask type checks using TypeKind.
    }
}
