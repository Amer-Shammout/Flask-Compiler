package SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Symbol table for Jinja template-level names (variables, blocks, loop iterators).
 */
public class TemplateSymbolTable extends AbstractSymbolTable {

    private final String templateName;
    private final boolean allowImplicitGlobals;
    private final boolean caseSensitive;

    public TemplateSymbolTable(String name, String templateName) {
        this(name, templateName, true, true);
    }

    public TemplateSymbolTable(String name, String templateName, boolean allowImplicitGlobals) {
        this(name, templateName, allowImplicitGlobals, true);
    }

    public TemplateSymbolTable(
            String name,
            String templateName,
            boolean allowImplicitGlobals,
            boolean caseSensitive) {
        super(name);
        this.templateName = templateName;
        this.allowImplicitGlobals = allowImplicitGlobals;
        this.caseSensitive = caseSensitive;
    }

    public String getTemplateName() {
        return templateName;
    }

    public boolean isAllowImplicitGlobals() {
        return allowImplicitGlobals;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
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
        return lookupLocal(name);
    }

    @Override
    public ISymbolTable enterScope(String scopeName) {
        LocalSymbolTable child = new LocalSymbolTable(scopeName, this, caseSensitive);
        children.add(child);
        return child;
    }

    @Override
    public ISymbolTable exitScope() {
        return this;
    }

    @Override
    public List<Symbol> listLocalSymbols() {
        return new ArrayList<>(symbols.values());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TemplateSymbolTable[").append(name).append("]");
        if (templateName != null) {
            sb.append(" (").append(templateName).append(")");
        }
        sb.append('\n');
        appendScope(sb, this, "  ");
        return sb.toString();
    }

    private static void appendScope(StringBuilder sb, ISymbolTable table, String indent) {
        if (table instanceof AbstractSymbolTable abstractTable) {
            for (Symbol symbol : abstractTable.symbols.values()) {
                sb.append(indent)
                        .append(symbol.getKind())
                        .append(' ')
                        .append(symbol.getName());
                if (symbol.getOrigin() != null) {
                    sb.append(" @ ").append(symbol.getOrigin());
                }
                sb.append('\n');
            }
            for (ISymbolTable child : abstractTable.children) {
                String scopeName = child instanceof AbstractSymbolTable scope
                        ? scope.getName()
                        : "nested";
                sb.append(indent).append("scope ").append(scopeName).append(":\n");
                appendScope(sb, child, indent + "  ");
            }
        }
    }
}
