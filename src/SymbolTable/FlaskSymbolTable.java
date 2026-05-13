package SymbolTable;

import java.util.List;
import java.util.Optional;

/**
 * Symbol table specialized for Flask (application-level) symbols.
 *
 * Fields:
 * - `sourceFile`: optional path to the source module that this table represents.
 * - `caseSensitive`: whether symbol names are case-sensitive for this language.
 * - `symbols` and `children` are inherited from `AbstractSymbolTable`.
 *
 * All methods contain TODO markers for Laila to implement actual behavior.
 */
public class FlaskSymbolTable extends AbstractSymbolTable {

    // Optional: origin/source file for diagnostics and origin tracking.
    private final String sourceFile;

    // Whether lookup is case-sensitive (Python/Flask is case-sensitive by default).
    private final boolean caseSensitive;

    public FlaskSymbolTable(String name, String sourceFile) {
        this(name, sourceFile, true);
    }

    public FlaskSymbolTable(String name, String sourceFile, boolean caseSensitive) {
        super(name);
        this.sourceFile = sourceFile;
        this.caseSensitive = caseSensitive;
    }

    /**
     * Return the optional source file associated with this table.
     */
    public String getSourceFile() { return sourceFile; }

    /**
     * Whether this table treats names as case-sensitive.
     */
    public boolean isCaseSensitive() { return caseSensitive; }

    @Override
    public boolean define(Symbol symbol) {
        // TODO(Laila): Implement define logic (conflict detection, insertion into `symbols`).
        throw new UnsupportedOperationException("TODO: implement FlaskSymbolTable.define");
    }

    @Override
    public Optional<Symbol> lookupLocal(String name) {
        // TODO(Laila): Implement local lookup using `symbols` map.
        throw new UnsupportedOperationException("TODO: implement FlaskSymbolTable.lookupLocal");
    }

    @Override
    public Optional<Symbol> lookup(String name) {
        // TODO(Laila): Implement lookup policy; for global table this may be same as local.
        throw new UnsupportedOperationException("TODO: implement FlaskSymbolTable.lookup");
    }

    @Override
    public ISymbolTable enterScope(String scopeName) {
        // TODO(Laila): Create a child LocalSymbolTable and register it in `children`.
        throw new UnsupportedOperationException("TODO: implement FlaskSymbolTable.enterScope");
    }

    @Override
    public ISymbolTable exitScope() {
        // TODO(Laila): Global table may return self or null; document chosen behavior.
        throw new UnsupportedOperationException("TODO: implement FlaskSymbolTable.exitScope");
    }

    @Override
    public List<Symbol> listLocalSymbols() {
        // TODO(Laila): Return a shallow copy of local symbols from `symbols`.
        throw new UnsupportedOperationException("TODO: implement FlaskSymbolTable.listLocalSymbols");
    }
}
