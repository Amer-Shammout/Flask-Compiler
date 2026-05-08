package SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Scope {

    private final String name;
    private final Scope parentScope;
    private final Map<String, Symbol> symbols;
    private final List<Scope> childScopes;


    public Scope(String name, Scope parentScope) {
        this.name = name;
        this.parentScope = parentScope;
        this.symbols = new HashMap<>();
        this.childScopes = new ArrayList<>();
    }


    public boolean addSymbol(Symbol symbol) {
        if (symbols.containsKey(symbol.getName())) {

            System.err.println("Error: Symbol '" + symbol.getName() + "' already defined in scope '" + name + "'");
            return false;
        }
        symbols.put(symbol.getName(), symbol);
        return true;
    }


    public Symbol lookup(String name) {
        Symbol s = symbols.get(name);
        if (s != null) return s;
        if (parentScope != null) return parentScope.lookup(name);
        return null;
    }


    public Scope createChildScope(String childName) {
        Scope child = new Scope(childName, this);
        childScopes.add(child);
        return child;
    }


    public String getName() { return name; }
    public Scope getParentScope() { return parentScope; }
    public List<Scope> getChildScopes() { return childScopes; }
    public Map<String, Symbol> getSymbols() { return symbols; }


    public void printScope(String indent) {
        System.out.println(indent + "Scope: " + name);
        for (Symbol s : symbols.values()) {
            System.out.println(indent + "  " + s);
        }
        for (Scope child : childScopes) {
            child.printScope(indent + "  ");
        }
    }
}
