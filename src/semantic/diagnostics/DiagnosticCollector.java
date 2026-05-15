package semantic.diagnostics;

import java.util.*;

/**
 * Collects and manages diagnostic messages during semantic analysis.
 *
 * DiagnosticCollector is a central repository for all diagnostics encountered during
 * compilation (parsing, semantic analysis, type checking, etc.). It provides methods to:
 * - Add diagnostics (errors, warnings, infos, hints)
 * - Filter diagnostics by severity
 * - Report all diagnostics in various formats
 * - Track whether errors have been encountered
 *
 * Thread Safety: Not thread-safe; designed for single-threaded compilation phases.
 *
 * Usage Example:
 *   DiagnosticCollector collector = new DiagnosticCollector();
 *   // During semantic analysis:
 *   if (variableNotFound) {
 *       Diagnostic diag = new Diagnostic(
 *           sourceRange,
 *           DiagnosticSeverity.ERROR,
 *           "Undefined variable 'x'"
 *       );
 *       collector.addDiagnostic(diag);
 *   }
 *   // After analysis:
 *   collector.reportAll(); // Print to console or collect for IDE
 */
public class DiagnosticCollector {

    // === Fields ===

    /** List of all collected diagnostics in the order they were added. */
    private final List<Diagnostic> diagnostics;

    /** Count of ERROR-level diagnostics. */
    private int errorCount;

    /** Count of WARNING-level diagnostics. */
    private int warningCount;

    /** Count of INFO-level diagnostics. */
    private int infoCount;

    /** Count of HINT-level diagnostics. */
    private int hintCount;


    // === Constructor ===

    /**
     * Construct a new empty DiagnosticCollector.
     */
    public DiagnosticCollector() {
        this.diagnostics = new ArrayList<>();
        this.errorCount = 0;
        this.warningCount = 0;
        this.infoCount = 0;
        this.hintCount = 0;
    }


    // === Core Methods ===

    /**
     * Add a diagnostic to the collection and update severity counts.
     *
     * TODO(Sedra): Implement deduplication logic if the same diagnostic is added multiple times.
     *
     * @param diagnostic The Diagnostic to add.
     */
    public void addDiagnostic(Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
        switch (diagnostic.getSeverity()) {
            case ERROR:
                errorCount++;
                break;
            case WARNING:
                warningCount++;
                break;
            case INFO:
                infoCount++;
                break;
            case HINT:
                hintCount++;
                break;
        }
    }

    /**
     * Add multiple diagnostics at once.
     *
     * @param diags Collection of diagnostics to add.
     */
    public void addDiagnostics(Collection<Diagnostic> diags) {
        for (Diagnostic diag : diags) {
            addDiagnostic(diag);
        }
    }


    // === Query Methods ===

    /**
     * Get all collected diagnostics in order.
     *
     * @return Unmodifiable list of all diagnostics.
     */
    public List<Diagnostic> getAllDiagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }

    /**
     * Get diagnostics filtered by severity level.
     *
     * @param severity The DiagnosticSeverity to filter by.
     * @return List of diagnostics matching the given severity.
     */
    public List<Diagnostic> getDiagnosticsBySeverity(DiagnosticSeverity severity) {
        List<Diagnostic> result = new ArrayList<>();
        for (Diagnostic diag : diagnostics) {
            if (diag.getSeverity() == severity) {
                result.add(diag);
            }
        }
        return result;
    }

    /**
     * Get only error-level diagnostics.
     *
     * @return List of ERROR diagnostics.
     */
    public List<Diagnostic> getErrors() {
        return getDiagnosticsBySeverity(DiagnosticSeverity.ERROR);
    }

    /**
     * Get only warning-level diagnostics.
     *
     * @return List of WARNING diagnostics.
     */
    public List<Diagnostic> getWarnings() {
        return getDiagnosticsBySeverity(DiagnosticSeverity.WARNING);
    }

    /**
     * Get only info-level diagnostics.
     *
     * @return List of INFO diagnostics.
     */
    public List<Diagnostic> getInfos() {
        return getDiagnosticsBySeverity(DiagnosticSeverity.INFO);
    }

    /**
     * Get only hint-level diagnostics.
     *
     * @return List of HINT diagnostics.
     */
    public List<Diagnostic> getHints() {
        return getDiagnosticsBySeverity(DiagnosticSeverity.HINT);
    }


    // === Status Methods ===

    /**
     * Check if any ERROR-level diagnostics have been collected.
     *
     * @return true if errorCount > 0, false otherwise.
     */
    public boolean hasErrors() {
        return errorCount > 0;
    }

    /**
     * Check if any WARNING-level diagnostics have been collected.
     *
     * @return true if warningCount > 0, false otherwise.
     */
    public boolean hasWarnings() {
        return warningCount > 0;
    }

    /**
     * Get the total number of diagnostics collected.
     *
     * @return Total count of all diagnostics.
     */
    public int getTotalCount() {
        return diagnostics.size();
    }

    /**
     * Get the count of ERROR-level diagnostics.
     *
     * @return Error count.
     */
    public int getErrorCount() {
        return errorCount;
    }

    /**
     * Get the count of WARNING-level diagnostics.
     *
     * @return Warning count.
     */
    public int getWarningCount() {
        return warningCount;
    }

    /**
     * Get the count of INFO-level diagnostics.
     *
     * @return Info count.
     */
    public int getInfoCount() {
        return infoCount;
    }

    /**
     * Get the count of HINT-level diagnostics.
     *
     * @return Hint count.
     */
    public int getHintCount() {
        return hintCount;
    }

    /**
     * Check if the collector is empty (no diagnostics collected).
     *
     * @return true if total count is 0, false otherwise.
     */
    public boolean isEmpty() {
        return diagnostics.isEmpty();
    }


    // === Reporting Methods ===

    /**
     * Print all collected diagnostics to standard output in a human-readable format.
     *
     * Format example:
     *   === Diagnostic Summary ===
     *   Errors: 2, Warnings: 3, Infos: 1, Hints: 0
     *   [ERROR] [E001] Undefined variable 'x' at line 5:10
     *   [WARNING] [W102] Unused variable 'y' at line 8:3
     *   ...
     *
     * TODO(Sedra): Implement pretty printing with sorting by severity/location.
     */
    public void reportAll() {
        System.out.println("=== Diagnostic Summary ===");
        System.out.println(String.format("Errors: %d, Warnings: %d, Infos: %d, Hints: %d",
                errorCount, warningCount, infoCount, hintCount));
        System.out.println();

        for (Diagnostic diag : diagnostics) {
            System.out.println(diag);
        }
    }

    /**
     * Get all diagnostics formatted as a single string report.
     *
     * TODO(Sedra): Implement detailed report generation with sorting/filtering options.
     *
     * @return String representation of all diagnostics.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Diagnostic Summary ===\n");
        sb.append(String.format("Errors: %d, Warnings: %d, Infos: %d, Hints: %d\n",
                errorCount, warningCount, infoCount, hintCount));
        sb.append("\n");

        for (Diagnostic diag : diagnostics) {
            sb.append(diag).append("\n");
        }

        return sb.toString();
    }

    /**
     * Clear all collected diagnostics and reset counts.
     *
     * Useful when reusing the same collector for multiple analyses or phases.
     */
    public void clear() {
        diagnostics.clear();
        errorCount = 0;
        warningCount = 0;
        infoCount = 0;
        hintCount = 0;
    }


    // === Helper Methods for Common Diagnostics ===

    /**
     * Add a diagnostic for an undefined variable.
     *
     * Generates: E001_UNDEFINED_VARIABLE
     *
     * @param sourceRange Location in source.
     * @param variableName Name of the undefined variable.
     * @param suggestion Optional suggestion (e.g., "Did you mean 'x'?").
     */
    public void reportUndefinedVariable(AST.SourceRange sourceRange, String variableName, String suggestion) {
        Diagnostic diag = new Diagnostic(
            sourceRange,
            ErrorCode.E001_UNDEFINED_VARIABLE,
            String.format("Undefined variable '%s'", variableName),
            suggestion
        );
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for an undefined variable (without suggestion).
     *
     * @param sourceRange Location in source.
     * @param variableName Name of the undefined variable.
     */
    public void reportUndefinedVariable(AST.SourceRange sourceRange, String variableName) {
        reportUndefinedVariable(sourceRange, variableName, null);
    }

    /**
     * Add a diagnostic for a type mismatch.
     *
     * Generates: E101_TYPE_MISMATCH
     *
     * @param sourceRange Location in source.
     * @param symbolName Name of the symbol.
     * @param expectedType Expected type.
     * @param actualType Actual/used type.
     * @param suggestion Optional suggestion for fixing.
     */
    public void reportTypeMismatch(AST.SourceRange sourceRange, String symbolName,
                                   TypeKind expectedType, TypeKind actualType, String suggestion) {
        Diagnostic diag = new Diagnostic(
            sourceRange,
            ErrorCode.E101_TYPE_MISMATCH,
            String.format("Type mismatch for '%s': expected %s but got %s",
                symbolName, expectedType, actualType),
            suggestion,
            actualType
        );
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for a type mismatch (without suggestion).
     *
     * @param sourceRange Location in source.
     * @param symbolName Name of the symbol.
     * @param expectedType Expected type.
     * @param actualType Actual/used type.
     */
    public void reportTypeMismatch(AST.SourceRange sourceRange, String symbolName,
                                   TypeKind expectedType, TypeKind actualType) {
        reportTypeMismatch(sourceRange, symbolName, expectedType, actualType, null);
    }

    /**
     * Add a diagnostic for a type error (general operation type error).
     *
     * Generates: E102_TYPE_ERROR
     *
     * @param sourceRange Location in source.
     * @param operation Description of the operation (e.g., "addition of str and int").
     * @param suggestion Optional suggestion.
     */
    public void reportTypeError(AST.SourceRange sourceRange, String operation, String suggestion) {
        Diagnostic diag = new Diagnostic(
            sourceRange,
            ErrorCode.E102_TYPE_ERROR,
            String.format("Type error: %s", operation),
            suggestion
        );
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for a scope error (out of scope or duplicate definition).
     *
     * Generates: E203_OUT_OF_SCOPE or E201_DUPLICATE_DEFINITION
     *
     * @param sourceRange Location in source.
     * @param symbolName Name of the symbol.
     * @param errorCode E201 or E203 (duplicate or out of scope).
     * @param message Descriptive message.
     * @param suggestion Optional suggestion.
     */
    public void reportScopeError(AST.SourceRange sourceRange, String symbolName,
                                 ErrorCode errorCode, String message, String suggestion) {
        Diagnostic diag = new Diagnostic(
            sourceRange,
            errorCode,
            message,
            suggestion
        );
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for a missing Flask variable.
     *
     * Generates: E004_MISSING_FLASK_VARIABLE
     *
     * @param sourceRange Location in template.
     * @param variableName Name of the missing Flask variable.
     * @param suggestion Optional suggestion (e.g., list of available Flask variables).
     */
    public void reportMissingFlaskVariable(AST.SourceRange sourceRange, String variableName, String suggestion) {
        Diagnostic diag = new Diagnostic(
            sourceRange,
            ErrorCode.E004_MISSING_FLASK_VARIABLE,
            String.format("Flask context variable '%s' not available", variableName),
            suggestion
        );
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for variable shadowing (warning).
     *
     * Generates: W101_SHADOWING
     *
     * @param sourceRange Location in source where shadowing occurs.
     * @param shadowingVariableName Name of the shadowing variable.
     * @param shadowedVariableName Name of the shadowed variable.
     * @param shadowedLocation Description of where the shadowed variable is from (e.g., "Flask context at app.py:30").
     */
    public void reportShadowing(AST.SourceRange sourceRange, String shadowingVariableName,
                                String shadowedVariableName, String shadowedLocation) {
        Diagnostic diag = new Diagnostic(
            sourceRange,
            ErrorCode.W101_SHADOWING,
            String.format("Variable '%s' shadows '%s' from %s", shadowingVariableName, shadowedVariableName, shadowedLocation),
            "This will hide the shadowed variable in this scope."
        );
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for unused variable (warning).
     *
     * Generates: W102_UNUSED_SYMBOL
     *
     * @param sourceRange Location where symbol is defined.
     * @param symbolName Name of the unused symbol.
     */
    public void reportUnusedSymbol(AST.SourceRange sourceRange, String symbolName) {
        Diagnostic diag = new Diagnostic(
            sourceRange,
            ErrorCode.W102_UNUSED_SYMBOL,
            String.format("Variable '%s' is never used", symbolName),
            null
        );
        addDiagnostic(diag);
    }

    /**
     * Add an info diagnostic that a symbol was successfully resolved.
     *
     * Generates: I001_SYMBOL_RESOLVED
     *
     * @param sourceRange Location in source.
     * @param symbolName Name of the symbol.
     * @param resolvedFrom Where the symbol was resolved from (e.g., "Flask context", "Template local").
     */
    public void reportSymbolResolved(AST.SourceRange sourceRange, String symbolName, String resolvedFrom) {
        Diagnostic diag = new Diagnostic(
            sourceRange,
            ErrorCode.I001_SYMBOL_RESOLVED,
            String.format("Symbol '%s' resolved from %s", symbolName, resolvedFrom),
            null
        );
        addDiagnostic(diag);
    }

    /**
     * Add an info diagnostic about type inference.
     *
     * Generates: I002_TYPE_INFERRED
     *
     * @param sourceRange Location in source.
     * @param symbolName Name of the symbol.
     * @param inferredType The inferred type.
     */
    public void reportTypeInferred(AST.SourceRange sourceRange, String symbolName, TypeKind inferredType) {
        Diagnostic diag = new Diagnostic(
            sourceRange,
            ErrorCode.I002_TYPE_INFERRED,
            String.format("Inferred type of '%s' as %s", symbolName, inferredType),
            null,
            inferredType
        );
        addDiagnostic(diag);
    }

    /**
     * Add a hint diagnostic with a suggestion.
     *
     * Generates: H001_SUGGESTION
     *
     * @param sourceRange Location in source.
     * @param suggestion The suggestion text.
     */
    public void reportSuggestion(AST.SourceRange sourceRange, String suggestion) {
        Diagnostic diag = new Diagnostic(
            sourceRange,
            ErrorCode.H001_SUGGESTION,
            suggestion,
            null
        );
        addDiagnostic(diag);
    }

    /**
     * Add a hint diagnostic with available symbols.
     *
     * Generates: H002_AVAILABLE_SYMBOLS
     *
     * @param sourceRange Location in source.
     * @param availableSymbols List or description of available symbols.
     */
    public void reportAvailableSymbols(AST.SourceRange sourceRange, String availableSymbols) {
        Diagnostic diag = new Diagnostic(
            sourceRange,
            ErrorCode.H002_AVAILABLE_SYMBOLS,
            String.format("Available symbols: %s", availableSymbols),
            null
        );
        addDiagnostic(diag);
    }
}