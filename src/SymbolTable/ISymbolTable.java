package SymbolTable;

import java.util.Optional;

/**
 * Public symbol table interface. Laila should implement these operations.
 * Methods are signatures only — implementations belong to Laila.
 */
public interface ISymbolTable {

    /**
     * Define a symbol in the current table/scope.
     * @param symbol symbol descriptor to define.
     * @return true if definition succeeded (no conflict), false otherwise.
     */
    boolean define(Symbol symbol);

    /**
     * Lookup a symbol by name in this table (does not search parent by default).
     * @param name symbol name
     * @return Optional with symbol if found, empty otherwise
     */
    Optional<Symbol> lookupLocal(String name);

    /**
     * Lookup a symbol by name, searching parents according to table policy.
     * @param name symbol name
     * @return Optional with symbol if found
     */
    Optional<Symbol> lookup(String name);

    /**
     * Enter a child scope (returns new child table). Implementations may
     * return a new ISymbolTable representing the nested scope.
     */
    ISymbolTable enterScope(String scopeName);

    /**
     * Exit current scope and return the parent table. If no parent, return self or null
     * depending on implementation. Laila should document chosen behavior.
     */
    ISymbolTable exitScope();

}
