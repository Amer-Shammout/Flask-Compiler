package SymbolTable;

import java.util.List;
import java.util.Optional;

/**
 * Special marker scope representing Python built-in symbols.
 * Used only as the defining scope for built-in symbols in ScopeBinding.
 */
public final class BuiltinsScope implements ISymbolTable {

    public static final BuiltinsScope INSTANCE = new BuiltinsScope();

    private BuiltinsScope() {
        // Singleton
    }

    @Override
    public boolean define(Symbol symbol) {
        return false; // Built-ins are immutable
    }

    @Override
    public Optional<Symbol> lookupLocal(String name) {
        return PythonBuiltins.lookup(name);
    }

    @Override
    public Optional<Symbol> lookup(String name) {
        return lookupLocal(name);
    }

    @Override
    public ISymbolTable enterScope(String scopeName) {
        return this; // Can't enter new scopes from built-ins
    }

    @Override
    public ISymbolTable exitScope() {
        return this;
    }

    @Override
    public String toString() {
        return "BuiltinsScope";
    }
}