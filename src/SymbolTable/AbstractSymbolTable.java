package SymbolTable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;


/**
 * Abstract base for symbol table implementations. Provides method signatures
 * and high-level documentation for Laila to implement.
 */
public abstract class AbstractSymbolTable implements ISymbolTable {


    // Map that stores symbols declared directly in this scope.
    // Use LinkedHashMap to preserve insertion order (helpful for deterministic
    // iteration in tests and diagnostics).
    protected final Map<String, Symbol> symbols = new LinkedHashMap<>();

    // Child scopes nested within this table. Useful for diagnostics and
    // for implementations that keep an ownership tree of scopes.
    protected final List<ISymbolTable> children = new ArrayList<>();
    protected final String name;

    public AbstractSymbolTable(String name) {
        this.name = name;
    }

    /**
     * Return human readable name for this table (scope name).
     */
    public String getName() { return name; }

    protected String normalizeKey(String symbolName, boolean caseSensitive) {
        return caseSensitive ? symbolName : symbolName.toLowerCase(Locale.ROOT);
    }

    protected boolean insertSymbol(Symbol symbol, boolean caseSensitive) {
        String key = normalizeKey(symbol.getName(), caseSensitive);
        if (symbols.containsKey(key)) {
            return false;
        }
        symbols.put(key, symbol);
        return true;
    }

    protected Optional<Symbol> lookupInMap(String symbolName, boolean caseSensitive) {
        return Optional.ofNullable(symbols.get(normalizeKey(symbolName, caseSensitive)));
    }

    @Override
    public abstract boolean define(Symbol symbol);

    @Override
    public abstract Optional<Symbol> lookupLocal(String name);

    @Override
    public abstract Optional<Symbol> lookup(String name);

    @Override
    public abstract ISymbolTable enterScope(String scopeName);

    @Override
    public abstract ISymbolTable exitScope();

    /**
     * Laila helper: list all symbols defined directly in this table.
     * @return list of symbols (shallow copy)
     */
    public abstract List<Symbol> listLocalSymbols();
}
