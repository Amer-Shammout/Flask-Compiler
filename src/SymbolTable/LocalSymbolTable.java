package SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Local / nested scope symbol table with parent chaining.
 */
public class LocalSymbolTable extends AbstractSymbolTable {

    private final ISymbolTable parent;
    private final boolean caseSensitive;

    public LocalSymbolTable(String name, ISymbolTable parent) {
        this(name, parent, true);
    }

    public LocalSymbolTable(String name, ISymbolTable parent, boolean caseSensitive) {
        super(name);
        this.parent = parent;
        this.caseSensitive = caseSensitive;
    }

    public ISymbolTable getParent() {
        return parent;
    }

    @Override
    public boolean define(Symbol symbol) {
        return insertSymbol(symbol, caseSensitive);
    }

    @Override
    public Optional<Symbol> lookupLocal(String name) {
        return lookupInMap(name, caseSensitive);
    }

    @Override
    public Optional<Symbol> lookup(String name) {
        Optional<Symbol> local = lookupLocal(name);
        if (local.isPresent()) {
            return local;
        }
        return parent != null ? parent.lookup(name) : Optional.empty();
    }

    @Override
    public ISymbolTable enterScope(String scopeName) {
        LocalSymbolTable child = new LocalSymbolTable(scopeName, this, caseSensitive);
        children.add(child);
        return child;
    }

    @Override
    public ISymbolTable exitScope() {
        return parent;
    }

    @Override
    public List<Symbol> listLocalSymbols() {
        return new ArrayList<>(symbols.values());
    }
}
