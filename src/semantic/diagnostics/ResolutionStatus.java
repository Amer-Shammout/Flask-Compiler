package semantic.diagnostics;

/**
 * Enumeration representing the resolution status of a symbol during semantic analysis.
 *
 * ResolutionStatus indicates whether a symbol (variable, function, type) was successfully
 * resolved (found in a symbol table) or not, and if not, the reason why.
 *
 * This enum supports cleaner semantic analysis flow:
 * - Avoids using Optional<Symbol> or null checks
 * - Provides explicit failure reasons for better diagnostics
 * - Enables recovery strategies based on resolution failure type
 *
 * Usage:
 *   ResolutionStatus status = resolver.resolveSymbol(name);
 *   if (status == ResolutionStatus.RESOLVED) {
 *       Symbol sym = resolver.getResolvedSymbol(name);
 *   } else if (status == ResolutionStatus.UNDEFINED) {
 *       // Generate E001 diagnostic and suggest similar names
 *   } else if (status == ResolutionStatus.OUT_OF_SCOPE) {
 *       // Generate E203 diagnostic mentioning the scope where it exists
 *   }
 *
 * TODO(Member 5): Implement resolution tracking in SymbolTable/Bridge methods.
 */
public enum ResolutionStatus {

    /**
     * RESOLVED: Symbol was found and resolved successfully.
     * 
     * Next step: Use the resolved Symbol for type checking and semantic analysis.
     */
    RESOLVED("resolved", "Symbol successfully resolved"),

    /**
     * UNDEFINED: Symbol not found in any accessible scope.
     * 
     * Generates: E001_UNDEFINED_VARIABLE (or E002_UNDEFINED_FUNCTION, E003_UNDEFINED_CLASS, etc.)
     * Suggestion: Show similar names from current scope or Flask context.
     */
    UNDEFINED("undefined", "Symbol not found in any scope"),

    /**
     * OUT_OF_SCOPE: Symbol exists but is outside the current scope (e.g., loop variable outside loop).
     * 
     * Generates: E203_OUT_OF_SCOPE
     * Suggestion: Show where the symbol is defined and why it's out of scope.
     */
    OUT_OF_SCOPE("out_of_scope", "Symbol exists but is out of scope"),

    /**
     * AMBIGUOUS: Multiple symbols with the same name exist (name resolution conflict).
     * 
     * Generates: E206 (future: ambiguous symbol)
     * Suggestion: Specify which scope the user meant.
     */
    AMBIGUOUS("ambiguous", "Multiple symbols with this name"),

    /**
     * PARTIAL: Symbol found but with incomplete type information (type unknown/any).
     * 
     * Used when: Flask variable lacks type annotation, template variable has inferred type only.
     * May generate: I002_TYPE_INFERRED (info) or W103_IMPLICIT_CONVERSION (warning).
     * Suggestion: Declare type explicitly or use type hints.
     */
    PARTIAL("partial", "Symbol found with incomplete type information"),

    /**
     * SHADOWED: Symbol is valid but shadows another symbol from an outer scope.
     * 
     * Generates: W101_SHADOWING (warning)
     * Used for: Local variable shadows Flask variable, template variable shadows parent template variable.
     * Note: Symbol is still resolved; this is just a warning.
     */
    SHADOWED("shadowed", "Symbol shadows an outer scope symbol"),

    /**
     * UNAVAILABLE: Symbol would be valid but is temporarily unavailable (e.g., in Flask context but app not initialized).
     * 
     * Used for: Flask context variables that aren't yet available during parsing.
     * May generate: W105 (future: delayed resolution warning).
     */
    UNAVAILABLE("unavailable", "Symbol is unavailable in this context");


    // === Fields ===

    /** Short status name. */
    private final String statusName;

    /** Human-readable description. */
    private final String description;


    // === Constructor ===

    /**
     * Construct a ResolutionStatus.
     *
     * @param statusName Short name (e.g., "resolved", "undefined").
     * @param description Human-readable description.
     */
    ResolutionStatus(String statusName, String description) {
        this.statusName = statusName;
        this.description = description;
    }


    // === Getters ===

    /**
     * Get the status name.
     *
     * @return Short status name.
     */
    public String getStatusName() {
        return statusName;
    }

    /**
     * Get the description.
     *
     * @return Human-readable description.
     */
    public String getDescription() {
        return description;
    }


    // === Utility Methods ===

    /**
     * Check if this status represents a successful resolution.
     *
     * @return true if status is RESOLVED or PARTIAL or SHADOWED.
     */
    public boolean isResolved() {
        return this == RESOLVED || this == PARTIAL || this == SHADOWED;
    }

    /**
     * Check if this status represents an error (symbol not found or conflict).
     *
     * @return true if status is UNDEFINED, OUT_OF_SCOPE, AMBIGUOUS, or UNAVAILABLE.
     */
    public boolean isError() {
        return this == UNDEFINED || this == OUT_OF_SCOPE || this == AMBIGUOUS || this == UNAVAILABLE;
    }

    /**
     * Check if this status is a warning (not fatal but noteworthy).
     *
     * @return true if status is SHADOWED.
     */
    public boolean isWarning() {
        return this == SHADOWED;
    }

    /**
     * Get a string representation for diagnostics/logging.
     *
     * @return Status name.
     */
    @Override
    public String toString() {
        return statusName;
    }
}
