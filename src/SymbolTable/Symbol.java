package SymbolTable;

import semantic.diagnostics.TypeKind;

/**
 * Simple symbol descriptor used by symbol tables.
 */
public final class Symbol {

    private final String name;
    private final SymbolKind kind; // semantic classification (enum)
    private final String origin; // optional: where this symbol was declared (file/template)

    // optional: inferred type recorded by TypeErrorChecker during Flask analysis
    private TypeKind inferredType;

    public Symbol(String name, SymbolKind kind) {
        this(name, kind, null, null);
    }

    public Symbol(String name, SymbolKind kind, String origin) {
        this(name, kind, origin, null);
    }

    public Symbol(String name, SymbolKind kind, String origin, TypeKind inferredType) {
        this.name = name;
        this.kind = kind;
        this.origin = origin;
        this.inferredType = inferredType != null ? inferredType : TypeKind.UNKNOWN;
    }

    /** Return the symbol name. */
    public String getName() { return name; }

    /** Return the symbol kind (semantic classification). */
    public SymbolKind getKind() { return kind; }

    /**
     * Optional origin information (source file, template name, etc.).
     */
    public String getOrigin() { return origin; }

    /**
     * Get the inferred type for this symbol (if recorded); returns TypeKind.UNKNOWN if not set.
     */
    public TypeKind getInferredType() {
        return inferredType != null ? inferredType : TypeKind.UNKNOWN;
    }

    /**
     * Set/update the inferred type for this symbol.
     */
    public void setInferredType(TypeKind type) {
        this.inferredType = type != null ? type : TypeKind.UNKNOWN;
    }
}
