package semantic.diagnostics;

/**
 * Enumeration of error codes for diagnostic messages.
 *
 * Each error code uniquely identifies a class of semantic errors, enabling:
 * - Consistent error reporting and categorization
 * - Easy filtering and grouping of related errors
 * - IDE integration (quick fixes, error suppression by code)
 * - Statistics and error tracking
 *
 * Error codes follow the pattern: [E|W|I|H]NNN
 * - E: ERROR
 * - W: WARNING
 * - I: INFO
 * - H: HINT
 *
 * Usage:
 *   Diagnostic diag = new Diagnostic(
 *       sourceRange,
 *       DiagnosticSeverity.ERROR,
 *       "Undefined variable 'x'",
 *       ErrorCode.E001_UNDEFINED_VARIABLE,
 *       "Did you mean 'X'?"
 *   );
 *
 * TODO(Sedra): Add more error codes as semantic analysis features expand.
 * TODO(Sedra): Consider organizing codes by category (E0xx for undefined, E1xx for type, etc.).
 */
public enum ErrorCode {

    // === Undefined / Not Found Errors (E0xx) ===

    /**
     * E001: Variable used but not defined in any accessible scope.
     * Severity: ERROR
     * Example: "Undefined variable 'user_name' in template"
     */
    E001_UNDEFINED_VARIABLE("E001", "Undefined variable"),

    /**
     * E002: Function or callable used but not defined.
     * Severity: ERROR
     * Example: "Undefined function 'render_card'"
     */
    E002_UNDEFINED_FUNCTION("E002", "Undefined function"),

    /**
     * E003: Class or type not found.
     * Severity: ERROR
     * Example: "Undefined class 'User'"
     */
    E003_UNDEFINED_CLASS("E003", "Undefined class"),

    /**
     * E004: Flask context variable not found.
     * Severity: ERROR
     * Example: "Flask context variable 'request' not available"
     */
    E004_MISSING_FLASK_VARIABLE("E004", "Missing Flask variable"),

    /**
     * E005: Attribute or member not found on object.
     * Severity: ERROR
     * Example: "Attribute 'email' not found on class 'User'"
     */
    E005_UNDEFINED_ATTRIBUTE("E005", "Undefined attribute"),


    // === Type Errors (E1xx) ===

    /**
     * E101: Type mismatch between expected and actual types.
     * Severity: ERROR
     * Example: "Type mismatch: expected List[int] but got Dict[str, str]"
     */
    E101_TYPE_MISMATCH("E101", "Type mismatch"),

    /**
     * E102: Type error in operation (e.g., adding string to int).
     * Severity: ERROR
     * Example: "TypeError: cannot add str and int"
     */
    E102_TYPE_ERROR("E102", "Type error"),

    /**
     * E103: Incompatible types in assignment or comparison.
     * Severity: ERROR
     * Example: "Incompatible types: str and int in comparison"
     */
    E103_INCOMPATIBLE_TYPES("E103", "Incompatible types"),


    // === Scope Errors (E2xx) ===

    /**
     * E201: Symbol already defined in the same scope (duplicate definition).
     * Severity: ERROR
     * Example: "Symbol 'count' already defined in this scope"
     */
    E201_DUPLICATE_DEFINITION("E201", "Duplicate definition"),

    /**
     * E202: Variable used before definition in the same scope.
     * Severity: ERROR
     * Example: "Variable 'x' used before definition"
     */
    E202_USE_BEFORE_DEFINITION("E202", "Use before definition"),

    /**
     * E203: Symbol referenced outside its valid scope.
     * Severity: ERROR
     * Example: "Variable 'loop_index' referenced outside loop scope"
     */
    E203_OUT_OF_SCOPE("E203", "Out of scope"),


    // === Warnings (W0xx) ===

    /**
     * W101: Variable shadows an outer scope variable.
     * Severity: WARNING
     * Example: "Warning: variable 'name' shadows Flask variable at app.py:30"
     */
    W101_SHADOWING("W101", "Shadowing"),

    /**
     * W102: Unused variable or symbol.
     * Severity: WARNING
     * Example: "Warning: variable 'temp' is never used"
     */
    W102_UNUSED_SYMBOL("W102", "Unused symbol"),

    /**
     * W103: Type mismatch but operation may still succeed (e.g., implicit conversion).
     * Severity: WARNING
     * Example: "Warning: implicit conversion from int to str"
     */
    W103_IMPLICIT_CONVERSION("W103", "Implicit conversion"),

    /**
     * W104: Potentially unsafe or deprecated syntax.
     * Severity: WARNING
     * Example: "Warning: deprecated syntax; use new form instead"
     */
    W104_DEPRECATED_SYNTAX("W104", "Deprecated syntax"),


    // === Info Messages (I0xx) ===

    /**
     * I001: Informational message about symbol resolution.
     * Severity: INFO
     * Example: "Symbol 'user' resolved from Flask context"
     */
    I001_SYMBOL_RESOLVED("I001", "Symbol resolved"),

    /**
     * I002: Type inference information.
     * Severity: INFO
     * Example: "Inferred type of 'items' as List[dict]"
     */
    I002_TYPE_INFERRED("I002", "Type inferred"),


    // === Hints (H0xx) ===

    /**
     * H001: Suggest correction or alternative.
     * Severity: HINT
     * Example: "Did you mean 'user_name'?"
     */
    H001_SUGGESTION("H001", "Suggestion"),

    /**
     * H002: Hint about available symbols in scope.
     * Severity: HINT
     * Example: "Available symbols: name, email, phone"
     */
    H002_AVAILABLE_SYMBOLS("H002", "Available symbols");


    // === Fields ===

    /** Error code string (e.g., "E001", "W101"). */
    private final String code;

    /** Short human-readable description. */
    private final String description;


    // === Constructor ===

    /**
     * Construct an ErrorCode.
     *
     * @param code Error code string.
     * @param description Short description.
     */
    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }


    // === Getters ===

    /**
     * Get the error code string.
     *
     * @return Code like "E001", "W101", etc.
     */
    public String getCode() {
        return code;
    }

    /**
     * Get the description.
     *
     * @return Short description of the error.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the severity of this error code based on prefix.
     *
     * TODO(Sedra): Implement severity mapping for quick filtering.
     *
     * @return DiagnosticSeverity (ERROR if E, WARNING if W, etc.)
     */
    public DiagnosticSeverity getSeverity() {
        if (code.startsWith("E")) {
            return DiagnosticSeverity.ERROR;
        } else if (code.startsWith("W")) {
            return DiagnosticSeverity.WARNING;
        } else if (code.startsWith("I")) {
            return DiagnosticSeverity.INFO;
        } else {
            return DiagnosticSeverity.HINT;
        }
    }

    /**
     * Get formatted string for use in diagnostics.
     *
     * @return "[CODE] Description"
     */
    @Override
    public String toString() {
        return String.format("[%s] %s", code, description);
    }
}
