package semantic.diagnostics;

import java.util.*;

/**
 * Collects and manages diagnostic messages during semantic analysis.
 * <p>
 * DiagnosticCollector is a central repository for all diagnostics encountered
 * during
 * compilation (parsing, semantic analysis, type checking, etc.). It provides
 * methods to:
 * - Add diagnostics (errors, warnings, infos, hints)
 * - Filter diagnostics by severity
 * - Report all diagnostics in various formats
 * - Track whether errors have been encountered
 * <p>
 * Thread Safety: Not thread-safe; designed for single-threaded compilation
 * phases.
 */
public class DiagnosticCollector {

    // === Fields ===

    /**
     * Used for deduplication of diagnostics added. The key is a compact
     * deterministic representation of a diagnostic.
     */
    private final Set<String> seen = new HashSet<>();

    /**
     * List of all collected diagnostics in the order they were added.
     */
    private final List<Diagnostic> diagnostics;

    /**
     * Count of ERROR-level diagnostics.
     */
    private int errorCount;

    /**
     * Count of WARNING-level diagnostics.
     */
    private int warningCount;

    /**
     * Count of INFO-level diagnostics.
     */
    private int infoCount;

    /**
     * Count of HINT-level diagnostics.
     */
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

    private static int severityOrder(DiagnosticSeverity s) {
        return switch (s) {
            case ERROR -> 4;
            case WARNING -> 3;
            case INFO -> 2;
            case HINT -> 1;
        };
    }

    /**
     * Add a diagnostic to the collection and update severity counts.
     * <p>
     * Deduplication:
     * - Avoids adding identical diagnostics repeatedly (based on error code,
     * message, location)
     * - Keys tolerate null source ranges
     * - Hint is NOT part of deduplication key (multiple hints for same error are
     * treated as duplicates)
     *
     * @param diagnostic The Diagnostic to add.
     */
    public void addDiagnostic(Diagnostic diagnostic) {
        if (diagnostic == null)
            return;

        String code = diagnostic.getErrorCode() != null ? diagnostic.getErrorCode().getCode() : "UNKNOWN_CODE";
        String message = diagnostic.getMessage() != null ? diagnostic.getMessage() : "";
        String srcRangeStr = "";
        try {
            if (diagnostic.getSourceRange() != null) {
                srcRangeStr = diagnostic.getSourceRange().toString();
            }
        } catch (Exception ignored) {
        }

        // Deduplication key: code + location + message (NOT hint — so same error with
        // different hints is deduplicated)
        String key = code + "|" + srcRangeStr + "|" + message;
        if (seen.contains(key)) {
            return;
        }
        seen.add(key);
        diagnostics.add(diagnostic);

        switch (diagnostic.getSeverity()) {
            case ERROR -> errorCount++;
            case WARNING -> warningCount++;
            case INFO -> infoCount++;
            case HINT -> hintCount++;
        }
    }


    // === Query Methods ===

    /**
     * /**
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
     * Pretty-print all collected diagnostics to standard output in a human-friendly
     * format.
     * <p>
     * This delegates to ColoredLogger which centralizes color/formatting logic.
     */
    public void reportAll() {
        // Delegate to ColoredLogger to avoid duplicating formatting logic.
        ColoredLogger.printSummary(this);
    }

    /**
     * Get all diagnostics formatted as a single string report.
     * <p>
     * The result is sorted by severity and location to make output deterministic
     * (useful for tests or writing to log files).
     *
     * @return String representation of all diagnostics.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Diagnostic Summary ===\n");
        sb.append(String.format("Errors: %d, Warnings: %d, Infos: %d, Hints: %d\n", errorCount, warningCount, infoCount,
                hintCount));
        sb.append("\n");

        List<Diagnostic> sorted = getSortedDiagnostics();
        for (Diagnostic diag : sorted) {
            sb.append(diag.toString()).append("\n");
        }

        return sb.toString();
    }


    /**
     * Clear all collected diagnostics and reset counts.
     * <p>
     * Useful when reusing the same collector for multiple analyses or phases.
     */
    /**
     * Unused Now
     */
    public void clear() {
        diagnostics.clear();
        seen.clear();
        errorCount = 0;
        warningCount = 0;
        infoCount = 0;
        hintCount = 0;
    }

    // === Helper Methods for Common Diagnostics ===

    /**
     * Add a diagnostic for an undefined variable.
     * <p>
     * Generates: E001_UNDEFINED_VARIABLE
     *
     * @param sourceRange  Location in source.
     * @param variableName Name of the undefined variable.
     * @param suggestion   Optional suggestion (e.g., "Did you mean 'x'?").
     */
    public void reportUndefinedVariable(AST.SourceRange sourceRange, String variableName, String suggestion) {
        Diagnostic diag = new Diagnostic(sourceRange, ErrorCode.E001_UNDEFINED_VARIABLE,
                String.format("Undefined variable '%s'", variableName), suggestion);
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for an undefined function (without suggestion).
     *
     * @param sourceRange Location in source.
     * @param fname       Name of the undefined function.
     */
    public void reportUndefinedFunction(AST.SourceRange sourceRange, String fname, String suggestion) {
        Diagnostic diag = new Diagnostic(sourceRange, ErrorCode.E002_UNDEFINED_FUNCTION,
                String.format("Undefined function '%s'", fname), suggestion);
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for an undefined variable (without suggestion).
     *
     * @param sourceRange  Location in source.
     * @param variableName Name of the undefined variable.
     */
    public void reportUndefinedVariable(AST.SourceRange sourceRange, String variableName) {
        reportUndefinedVariable(sourceRange, variableName, null);
    }

    /**
     * Add a diagnostic for a type mismatch.
     * <p>
     * Generates: E101_TYPE_MISMATCH
     *
     * @param sourceRange  Location in source.
     * @param symbolName   Name of the symbol.
     * @param expectedType Expected type.
     * @param actualType   Actual/used type.
     * @param suggestion   Optional suggestion for fixing.
     */
    public void reportTypeMismatch(AST.SourceRange sourceRange, String symbolName, TypeKind expectedType,
                                   TypeKind actualType, String suggestion) {
        Diagnostic diag = new Diagnostic(sourceRange, ErrorCode.E101_TYPE_MISMATCH,
                String.format("Type mismatch for '%s': expected %s but got %s", symbolName, expectedType, actualType),
                suggestion, actualType);
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for a type mismatch (without suggestion).
     *
     * @param sourceRange  Location in source.
     * @param symbolName   Name of the symbol.
     * @param expectedType Expected type.
     * @param actualType   Actual/used type.
     */
    public void reportTypeMismatch(AST.SourceRange sourceRange, String symbolName, TypeKind expectedType,
                                   TypeKind actualType) {
        reportTypeMismatch(sourceRange, symbolName, expectedType, actualType, null);
    }

    /**
     * Add a diagnostic for a type error (general operation type error).
     * <p>
     * Generates: E102_TYPE_ERROR
     *
     * @param sourceRange Location in source.
     * @param operation   Description of the operation (e.g., "addition of str and
     *                    int").
     * @param suggestion  Optional suggestion.
     */
    public void reportTypeError(AST.SourceRange sourceRange, String operation, String suggestion) {
        Diagnostic diag = new Diagnostic(sourceRange, ErrorCode.E102_TYPE_ERROR,
                String.format("Type error: %s", operation), suggestion);
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for a scope error (out of scope or duplicate definition).
     * <p>
     * Generates: E203_OUT_OF_SCOPE or E201_DUPLICATE_DEFINITION
     *
     * @param sourceRange Location in source.
     * @param symbolName  Name of the symbol.
     * @param errorCode   E201 or E203 (duplicate or out of scope).
     * @param message     Descriptive message.
     * @param suggestion  Optional suggestion.
     */
    public void reportScopeError(AST.SourceRange sourceRange, String symbolName, ErrorCode errorCode, String message,
                                 String suggestion) {
        Diagnostic diag = new Diagnostic(sourceRange, errorCode, message, suggestion);
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for a missing Flask variable.
     * <p>
     * Generates: E004_MISSING_FLASK_VARIABLE
     *
     * @param sourceRange  Location in template.
     * @param variableName Name of the missing Flask variable.
     * @param suggestion   Optional suggestion (e.g., list of available Flask
     *                     variables).
     */
    public void reportMissingFlaskVariable(AST.SourceRange sourceRange, String variableName, String suggestion) {
        Diagnostic diag = new Diagnostic(sourceRange, ErrorCode.E004_MISSING_FLASK_VARIABLE,
                String.format("Flask context variable '%s' not available", variableName), suggestion);
        addDiagnostic(diag);
    }

    /**
     * Add a diagnostic for variable shadowing (warning).
     * <p>
     * Generates: W101_SHADOWING
     *
     * @param sourceRange           Location in source where shadowing occurs.
     * @param shadowingVariableName Name of the shadowing variable.
     * @param shadowedVariableName  Name of the shadowed variable.
     * @param shadowedLocation      Description of where the shadowed variable is
     *                              from (e.g., "Flask context at app.py:30").
     */
    public void reportShadowing(AST.SourceRange sourceRange, String shadowingVariableName, String shadowedVariableName,
                                String shadowedLocation) {
        Diagnostic diag = new Diagnostic(
                sourceRange, ErrorCode.W101_SHADOWING, String.format("Variable '%s' shadows '%s' from %s",
                shadowingVariableName, shadowedVariableName, shadowedLocation),
                "This will hide the shadowed variable in this scope.");
        addDiagnostic(diag);
    }



    // === Utilities ===

    /**
     * Return diagnostics sorted by severity (ERROR first) then by source location
     * (line, column).
     * <p>
     * The comparator is stable and deterministic.
     */
    public List<Diagnostic> getSortedDiagnostics() {
        List<Diagnostic> list = new ArrayList<>(diagnostics);
        list.sort((a, b) -> {
            // Severity order: ERROR > WARNING > INFO > HINT
            int orderA = severityOrder(a.getSeverity());
            int orderB = severityOrder(b.getSeverity());
            if (orderA != orderB)
                return Integer.compare(orderB, orderA); // higher first

            // Compare by source location if available
            if (a.getSourceRange() != null && b.getSourceRange() != null) {
                try {
                    int lineA = a.getSourceRange().getStart().getLine();
                    int lineB = b.getSourceRange().getStart().getLine();
                    if (lineA != lineB)
                        return Integer.compare(lineA, lineB); // - => a < b
                    int colA = a.getSourceRange().getStart().getColumn();
                    int colB = b.getSourceRange().getStart().getColumn();
                    return Integer.compare(colA, colB);
                } catch (Exception ignored) {
                }
            }

            // Fallback: compare by error code then message
            int codeCmp = a.getErrorCode().getCode().compareTo(b.getErrorCode().getCode());
            if (codeCmp != 0)
                return codeCmp;
            return a.getMessage().compareTo(b.getMessage());
        });
        return list;
    }
}