package semantic.diagnostics;

import AST.SourceRange;

import java.util.Optional;

/**
 * Represents a single diagnostic message (error, warning, info, or hint).
 * <p>
 * A Diagnostic captures:
 * - Location in source code (via SourceRange)
 * - Severity level (derived from ErrorCode)
 * - Human-readable message
 * - Error code (ErrorCode enum)
 * - Optional context hint or suggestion
 * - Optional type information for type errors
 * <p>
 * Diagnostics are immutable and designed to be collected and reported by DiagnosticCollector.
 */
public class Diagnostic {

    // === Fields ===

    /**
     * Source location where the diagnostic occurred.
     */
    private final SourceRange sourceRange;

    /**
     * Diagnostic error code (enum).
     */
    private final ErrorCode errorCode;

    /**
     * Human-readable diagnostic message.
     */
    private final String message;

    /**
     * Optional context hint or suggestion for fixing the issue.
     */
    private final Optional<String> hint;

    /**
     * Optional type information (for type errors).
     */
    private final Optional<TypeKind> relatedType;


    // === Constructors ===

    /**
     * Construct a Diagnostic with all fields.
     *
     * @param sourceRange Location in source code.
     * @param errorCode   ErrorCode (enum).
     * @param message     Human-readable message.
     * @param hint        Optional hint or suggestion.
     * @param relatedType Optional type for type errors.
     */
    public Diagnostic(SourceRange sourceRange, ErrorCode errorCode, String message, String hint, TypeKind relatedType) {
        this.sourceRange = sourceRange;
        this.errorCode = errorCode;
        this.message = message != null ? message : "";
        this.hint = Optional.ofNullable(hint);
        this.relatedType = Optional.ofNullable(relatedType);
    }

    /**
     * Construct a Diagnostic with basic fields (no hint or type).
     *
     * @param sourceRange Location in source code.
     * @param errorCode   ErrorCode (enum).
     * @param message     Human-readable message.
     */
    public Diagnostic(SourceRange sourceRange, ErrorCode errorCode, String message) {
        this(sourceRange, errorCode, message, null, null);
    }

    /**
     * Construct a Diagnostic with hint.
     *
     * @param sourceRange Location in source code.
     * @param errorCode   ErrorCode (enum).
     * @param message     Human-readable message.
     * @param hint        Optional hint or suggestion.
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
     * <p>
     * Example output:
     * [ERROR] [5:10] [E001] Undefined variable 'x'
     * Hint: Did you mean 'X' defined on line 3?
     * <p>
     * TODO(Sedra): Implement formatting logic with color codes (if needed for terminal output).
     * Colored output is handled by ColoredLogger; this toString produces a deterministic plain-text representation.
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
        if (sourceRange != null && sourceRange.getStart() != null) {
            sb.append(" [").append(sourceRange.getStart().getLine()).append(":").append(sourceRange.getStart().getColumn()).append("]");
        } else {
            sb.append(" [unknown]");
        }

        // Error code
        if (errorCode != null) {
            sb.append(" [").append(errorCode.getCode()).append("]");
        } else {
            sb.append(" [UNKNOWN]");
        }

        // Message
        sb.append(" ").append(message);

        // Related type (if present)
        relatedType.ifPresent(t -> sb.append(" (type: ").append(t.getDisplayName()).append(")"));

        // Hint
        hint.ifPresent(h -> sb.append("\n  Hint: ").append(h));

        return sb.toString();
    }

    /**
     * Compare two diagnostics by severity (ERROR > WARNING > INFO > HINT) and then by location.
     * <p>
     * Ordering: ERROR > WARNING > INFO > HINT, then by line/column.
     *
     * @param other Another Diagnostic.
     * @return Negative if this diagnostic is more severe or earlier, positive if less, 0 if equal.
     */
    public int compareBySeverity(Diagnostic other) {
        if (other == null) return -1;
        int myOrder = getSeverityOrder(this.getSeverity());
        int otherOrder = getSeverityOrder(other.getSeverity());
        if (myOrder != otherOrder) {
            // higher numeric order => more severe, so return negative when this is more severe
            return Integer.compare(otherOrder, myOrder);
        }

        if (this.sourceRange != null && other.sourceRange != null && this.sourceRange.getStart() != null && other.sourceRange.getStart() != null) {
            int lineCompare = Integer.compare(this.sourceRange.getStart().getLine(), other.sourceRange.getStart().getLine());
            if (lineCompare != 0) return lineCompare;

            int colCompare = Integer.compare(this.sourceRange.getStart().getColumn(), other.sourceRange.getStart().getColumn());
            return colCompare;
        }

        // Fallback to comparing codes/messages
        String myCode = this.errorCode != null ? this.errorCode.getCode() : "";
        String otherCode = other.errorCode != null ? other.errorCode.getCode() : "";
        int codeCmp = myCode.compareTo(otherCode);
        if (codeCmp != 0) return codeCmp;
        return this.message.compareTo(other.message);
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