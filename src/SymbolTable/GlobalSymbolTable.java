package SymbolTable;

import java.util.List;
import java.util.Optional;

/**
 * Global symbol table (top-level). Laila should implement storage and lookup.
 * Keep this class minimal and prefer composition of AbstractSymbolTable.
 */
public class GlobalSymbolTable extends AbstractSymbolTable {

    public GlobalSymbolTable(String name) {
        super(name);
    }

    @Override
    public boolean define(Symbol symbol) {
        // TODO(Laila): Implement symbol definition at global scope.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Optional<Symbol> lookupLocal(String name) {
        // TODO(Laila): Lookup only in this global table.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Optional<Symbol> lookup(String name) {
        // TODO(Laila): Lookup with global semantics (may be same as local).
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ISymbolTable enterScope(String scopeName) {
        // TODO(Laila): Create and return a new child/local scope instance.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ISymbolTable exitScope() {
        // Global has no parent — decide policy (return self or null). Document choice.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<Symbol> listLocalSymbols() {
        // TODO(Laila): Return symbols defined at global scope.
        throw new UnsupportedOperationException("TODO");
    }
}
