package semantic.diagnostics;

import AST.SourceRange;
import java.util.Optional;

/**
 * Represents a single diagnostic message (error, warning, info, or hint).
 *
 * A Diagnostic captures:
 * - Location in source code (via SourceRange)
 * - Severity level (ERROR, WARNING, INFO, HINT)
 * - Human-readable message
 * - Error code (ErrorCode enum, not just string)
 * - Optional context hint or suggestion
 * - Optional type information for type errors
 *
 * Diagnostics are immutable and designed to be collected and reported by DiagnosticCollector.
 *
 * Example:
 *   Diagnostic diag = new Diagnostic(
 *       sourceRange,
 *       ErrorCode.E001_UNDEFINED_VARIABLE,
 *       "Undefined variable 'x'",
 *       "Did you mean 'X' (defined on line 5)?"
 *   );
 */
public class Diagnostic {

    // === Fields ===

    /** Source location where the diagnostic occurred. */
    private final SourceRange sourceRange;

    /** Diagnostic error code (enum, not string). */
    private final ErrorCode errorCode;

    /** Human-readable diagnostic message. */
    private final String message;

    /** Optional context hint or suggestion for fixing the issue. */
    private final Optional<String> hint;

    /** Optional type information (for type errors). */
    private final Optional<TypeKind> relatedType;


    // === Constructors ===

    /**
     * Construct a Diagnostic with all fields.
     *
     * @param sourceRange Location in source code.
     * @param errorCode ErrorCode (enum).
     * @param message Human-readable message.
     * @param hint Optional hint or suggestion.
     * @param relatedType Optional type for type errors.
     */
    public Diagnostic(SourceRange sourceRange, ErrorCode errorCode,
                      String message, String hint, TypeKind relatedType) {
        this.sourceRange = sourceRange;
        this.errorCode = errorCode;
        this.message = message;
        this.hint = Optional.ofNullable(hint);
        this.relatedType = Optional.ofNullable(relatedType);
    }

    /**
     * Construct a Diagnostic with basic fields (no hint or type).
     *
     * @param sourceRange Location in source code.
     * @param errorCode ErrorCode (enum).
     * @param message Human-readable message.
     */
    public Diagnostic(SourceRange sourceRange, ErrorCode errorCode, String message) {
        this(sourceRange, errorCode, message, null, null);
    }

    /**
     * Construct a Diagnostic with hint.
     *
     * @param sourceRange Location in source code.
     * @param errorCode ErrorCode (enum).
     * @param message Human-readable message.
     * @param hint Optional hint or suggestion.
     */
    public Diagnostic(SourceRange sourceRange, ErrorCode errorCode, String message, String hint) {
        this(sourceRange, errorCode, message, hint, null);
    }


    // === Getters ===

    /**
     * Get the error code (enum).
     *
     * @return ErrorCode.
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Get the severity level of this diagnostic (derived from error code).
     *
     * @return DiagnosticSeverity (ERROR, WARNING, INFO, HINT).
     */
    public DiagnosticSeverity getSeverity() {
        return errorCode.getSeverity();
    }

    /**
     * Get the human-readable message.
     *
     * @return Message string.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the optional hint or suggestion.
     *
     * @return Optional hint string.
     */
    public Optional<String> getHint() {
        return hint;
    }

    /**
     * Get the optional related type for type errors.
     *
     * @return Optional TypeKind.
     */
    public Optional<TypeKind> getRelatedType() {
        return relatedType;
    }

    /**
     * Get the source range where the diagnostic occurred.
     *
     * @return SourceRange.
     */
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    // === toString & Comparison ===

    /**
     * Format diagnostic as: "[SEVERITY] [line:col] [CODE] message\n  Hint: hint (if available)"
     *
     * Example output:
     *   [ERROR] [5:10] [E001] Undefined variable 'x'
     *     Hint: Did you mean 'X' defined on line 3?
     *
     * TODO(Member 5): Implement formatting logic with color codes (if needed for terminal output).
     *
     * @return Formatted string representation.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Severity
        DiagnosticSeverity severity = getSeverity();
        sb.append("[").append(severity.name()).append("]");

        // Location (line:col)
        if (sourceRange != null) {
            sb.append(" [").append(sourceRange.getStart().getLine())
              .append(":").append(sourceRange.getStart().getColumn()).append("]");
        }

        // Error code
        sb.append(" ").append(errorCode.toString());

        // Message
        sb.append(" ").append(message);

        // Related type (if present)
        if (relatedType.isPresent()) {
            sb.append(" (type: ").append(relatedType.get()).append(")");
        }

        // Hint
        if (hint.isPresent()) {
            sb.append("\n  Hint: ").append(hint.get());
        }

        return sb.toString();
    }

    /**
     * Compare two diagnostics by severity (ERROR > WARNING > INFO > HINT) and then by location.
     *
     * Ordering: ERROR > WARNING > INFO > HINT, then by line/column.
     *
     * @param other Another Diagnostic.
     * @return Negative if this diagnostic is more severe, positive if less, 0 if equal severity.
     */
    public int compareBySeverity(Diagnostic other) {
        // Define severity ordering
        int severityOrder = getSeverityOrder(this.getSeverity())
                            - getSeverityOrder(other.getSeverity());
        if (severityOrder != 0) {
            return severityOrder;
        }

        // If same severity, compare by location
        if (sourceRange != null && other.sourceRange != null) {
            int lineCompare = Integer.compare(
                sourceRange.getStart().getLine(),
                other.sourceRange.getStart().getLine()
            );
            if (lineCompare != 0) {
                return lineCompare;
            }

            return Integer.compare(
                sourceRange.getStart().getColumn(),
                other.sourceRange.getStart().getColumn()
            );
        }

        return 0;
    }

    /**
     * Helper: get numeric order for severity (higher = more severe).
     *
     * @param severity DiagnosticSeverity.
     * @return Numeric order (ERROR=4, WARNING=3, INFO=2, HINT=1).
     */
    private static int getSeverityOrder(DiagnosticSeverity severity) {
        switch (severity) {
            case ERROR:
                return 4;
            case WARNING:
                return 3;
            case INFO:
                return 2;
            case HINT:
                return 1;
            default:
                return 0;
        }
    }
}
