package SymbolTable;

import java.util.List;
import java.util.Optional;

/**
 * Local / nested scope symbol table. Laila should implement parent chaining
 * and shadowing semantics here.
 */
public class LocalSymbolTable extends AbstractSymbolTable {

    private final ISymbolTable parent;

    public LocalSymbolTable(String name, ISymbolTable parent) {
        super(name);
        this.parent = parent;
    }

    @Override
    public boolean define(Symbol symbol) {
        // TODO(Laila): Implement definition with local conflict detection.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Optional<Symbol> lookupLocal(String name) {
        // TODO(Laila): Lookup only in this local table.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Optional<Symbol> lookup(String name) {
        // TODO(Laila): Lookup in local then parent(s) according to policy.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ISymbolTable enterScope(String scopeName) {
        // TODO(Laila): Create nested child scope (return new LocalSymbolTable)
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ISymbolTable exitScope() {
        // TODO(Laila): Return parent scope.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<Symbol> listLocalSymbols() {
        // TODO(Laila): Return list of local symbols.
        throw new UnsupportedOperationException("TODO");
    }
}
