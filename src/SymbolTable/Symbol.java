package SymbolTable;

/**
 * Simple symbol descriptor used by symbol tables.
 * Laila: fill fields as needed. Keep this as a plain data holder.
 */
public final class Symbol {

    // we can add later inferredType, range, mutable
    private final String name;
    private final SymbolKind kind; // semantic classification (enum)
    private final String origin; // optional: where this symbol was declared (file/template)

    public Symbol(String name, SymbolKind kind) {
        this(name, kind, null);
    }

    public Symbol(String name, SymbolKind kind, String origin) {
        this.name = name;
        this.kind = kind;
        this.origin = origin;
    }

    /** Return the symbol name. */
    public String getName() { return name; }

    /** Return the symbol kind (semantic classification). */
    public SymbolKind getKind() { return kind; }

    /**
     * Optional origin information (source file, template name, etc.).
     */
    public String getOrigin() { return origin; }
}
