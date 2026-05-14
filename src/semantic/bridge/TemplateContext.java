package semantic.bridge;

import SymbolTable.Symbol;
import semantic.diagnostics.TypeKind;
import java.util.Optional;

/**
 * Represents contextual information about a symbol in a Template that may be linked
 * to a Flask context or scope.
 *
 * TemplateContext bridges the gap between:
 * - Template-level symbols (variables, macros, blocks)
 * - Flask-level symbols (context variables, functions, classes)
 *
 * When a template variable is rendered or used, we may want to:
 * - Track its origin (Flask context, parent template, local definition)
 * - Resolve its type or value from Flask
 * - Generate diagnostics linking template usage to Flask definitions
 * - Check for shadowing, type mismatches, and scope violations
 *
 * Fields:
 * - templateSymbol: The symbol as seen in the template.
 * - flaskSymbol: Optional symbol linked from Flask scope.
 * - origin: Where the symbol comes from (FLASK_CONTEXT, PARENT_TEMPLATE, LOCAL, etc.).
 * - resolvedType: Type information (using TypeKind enum, not String).
 * - sourceHint: Optional source location hint (e.g., line in parent template).
 *
 * TODO(Member 5): Extend with more context fields (e.g., original name in Flask, line in parent template).
 */
public class TemplateContext {

    // === Nested Enum ===

    /**
     * Origin of a template symbol.
     */
    public enum SymbolOrigin {
        /**
         * Symbol comes from Flask context/scope (app context, view function, etc.).
         */
        FLASK_CONTEXT,

        /**
         * Symbol comes from a parent template (via extends/import).
         */
        PARENT_TEMPLATE,

        /**
         * Symbol is defined locally in this template (macro, set, block, etc.).
         */
        LOCAL,

        /**
         * Symbol is from Jinja2 built-ins (range, dict, lipsum, etc.).
         */
        JINJA_BUILTIN,

        /**
         * Symbol origin is unknown or ambiguous.
         */
        UNKNOWN
    }


    // === Fields ===

    /** The symbol as it appears in the template. */
    private final Symbol templateSymbol;

    /** Optional corresponding symbol from Flask scope. */
    private final Optional<Symbol> flaskSymbol;

    /** Origin of this symbol. */
    private final SymbolOrigin origin;

    /** Type of the symbol (using TypeKind enum, not String). */
    private final TypeKind type;

    /** Optional source location context (e.g., name in Flask, template file, parent). */
    private final Optional<String> sourceHint;


    // === Constructors ===

    /**
     * Construct a TemplateContext with all fields.
     *
     * @param templateSymbol The symbol from template.
     * @param flaskSymbol Optional symbol from Flask.
     * @param origin Symbol origin.
     * @param type Type of the symbol (TypeKind enum, defaults to UNKNOWN).
     * @param sourceHint Optional source context hint.
     */
    public TemplateContext(Symbol templateSymbol, Symbol flaskSymbol,
                           SymbolOrigin origin, TypeKind type, String sourceHint) {
        this.templateSymbol = templateSymbol;
        this.flaskSymbol = Optional.ofNullable(flaskSymbol);
        this.origin = origin;
        this.type = type != null ? type : TypeKind.UNKNOWN;
        this.sourceHint = Optional.ofNullable(sourceHint);
    }

    /**
     * Construct a TemplateContext with minimal fields (type defaults to UNKNOWN).
     *
     * @param templateSymbol The symbol from template.
     * @param origin Symbol origin.
     */
    public TemplateContext(Symbol templateSymbol, SymbolOrigin origin) {
        this(templateSymbol, null, origin, TypeKind.UNKNOWN, null);
    }

    /**
     * Construct a TemplateContext with explicit type.
     *
     * @param templateSymbol The symbol from template.
     * @param origin Symbol origin.
     * @param type Type of the symbol (TypeKind).
     */
    public TemplateContext(Symbol templateSymbol, SymbolOrigin origin, TypeKind type) {
        this(templateSymbol, null, origin, type, null);
    }


    // === Getters ===

    /**
     * Get the template symbol.
     *
     * @return The Symbol as seen in template.
     */
    public Symbol getTemplateSymbol() {
        return templateSymbol;
    }

    /**
     * Get the optional Flask symbol linked to this template symbol.
     *
     * @return Optional Symbol from Flask scope.
     */
    public Optional<Symbol> getFlaskSymbol() {
        return flaskSymbol;
    }

    /**
     * Get the origin of this symbol.
     *
     * @return SymbolOrigin.
     */
    public SymbolOrigin getOrigin() {
        return origin;
    }

    /**
     * Get the type of this symbol (TypeKind enum).
     *
     * @return TypeKind (may be UNKNOWN).
     */
    public TypeKind getType() {
        return type;
    }

    /**
     * Get the optional source hint for this symbol.
     *
     * @return Optional source context (e.g., name in Flask, location in parent).
     */
    public Optional<String> getSourceHint() {
        return sourceHint;
    }


    // === Utility Methods ===

    /**
     * Check if this template context has a linked Flask symbol.
     *
     * @return true if flaskSymbol is present.
     */
    public boolean isLinkedToFlask() {
        return flaskSymbol.isPresent();
    }

    /**
     * Check if this symbol is from a Flask context (either directly or via linking).
     *
     * @return true if origin is FLASK_CONTEXT or if a Flask symbol is linked.
     */
    public boolean isFromFlask() {
        return origin == SymbolOrigin.FLASK_CONTEXT || flaskSymbol.isPresent();
    }

    /**
     * Get a human-readable description of this context for diagnostics.
     *
     * Format: "Symbol 'name' from ORIGIN (type: TYPE, linked to Flask: yes/no)"
     *
     * TODO(Member 5): Implement detailed context string for error messages.
     *
     * @return Descriptive string.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TemplateContext { symbol: ").append(templateSymbol.getName())
          .append(", origin: ").append(origin.name())
          .append(", type: ").append(type);

        if (flaskSymbol.isPresent()) {
            sb.append(", linkedToFlask: ").append(flaskSymbol.get().getName());
        }

        if (sourceHint.isPresent()) {
            sb.append(", source: ").append(sourceHint.get());
        }

        sb.append(" }");
        return sb.toString();
    }
}
