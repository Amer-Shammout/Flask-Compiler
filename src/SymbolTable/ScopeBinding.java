package SymbolTable;

/**
 * Result of resolving a name to the innermost scope that owns the binding.
 */
public final class ScopeBinding {

    private final Symbol symbol;
    private final ISymbolTable definingScope;
    private final boolean shadowsOuter;

    public ScopeBinding(Symbol symbol, ISymbolTable definingScope, boolean shadowsOuter) {
        this.symbol = symbol;
        this.definingScope = definingScope;
        this.shadowsOuter = shadowsOuter;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public ISymbolTable getDefiningScope() {
        return definingScope;
    }

    public boolean shadowsOuter() {
        return shadowsOuter;
    }
}
