package SymbolTable;

import java.util.Map;
import java.util.Optional;

public final class RuntimeScope implements ISymbolTable {

    public static final RuntimeScope INSTANCE = new RuntimeScope();

    private static final Map<String, Symbol> RUNTIME_SYMBOLS = Map.of(
            "__name__", new Symbol("__name__", SymbolKind.VARIABLE, "runtime"),
            "__file__", new Symbol("__file__", SymbolKind.VARIABLE, "runtime"),
            "__doc__", new Symbol("__doc__", SymbolKind.VARIABLE, "runtime"),
            "__package__", new Symbol("__package__", SymbolKind.VARIABLE, "runtime")
    );

    private RuntimeScope() {}

    @Override
    public boolean define(Symbol symbol) {
        // runtime scope is immutable
        return false;
    }

    @Override
    public Optional<Symbol> lookupLocal(String name) {
        return Optional.ofNullable(RUNTIME_SYMBOLS.get(name));
    }

    @Override
    public Optional<Symbol> lookup(String name) {
        return lookupLocal(name);
    }

    @Override
    public ISymbolTable enterScope(String scopeName) {
        throw new UnsupportedOperationException("RuntimeScope has no child scopes");
    }

    @Override
    public ISymbolTable exitScope() {
        return this;
    }
}