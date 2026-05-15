package semantic.diagnostics;

/**
 * Enumeration representing the severity level of a diagnostic message.
 *
 * Diagnostic severity determines how critical a problem is and how it should be displayed
 * to the user in IDEs, build tools, or compiler output.
 *
 * Usage: When creating a Diagnostic, specify its severity level to indicate the impact.
 *
 * TODO(Sedra): Add severity-based filtering or prioritization logic if needed.
 */
public enum DiagnosticSeverity {
    /**
     * ERROR: Critical issue that prevents compilation or execution.
     * Examples: Undefined variable, type mismatch, syntax error.
     */
    ERROR,

    /**
     * WARNING: Potential problem that does not prevent compilation but may cause runtime issues.
     * Examples: Unused variable, shadowed symbol, deprecated syntax.
     */
    WARNING,

    /**
     * INFO: Informational message that suggests best practices or minor improvements.
     * Examples: Code style suggestions, optimization hints.
     */
    INFO,

    /**
     * HINT: Subtle suggestion that may improve code clarity or performance.
     * Examples: Unused import, simplifiable expression.
     */
    HINT
}
