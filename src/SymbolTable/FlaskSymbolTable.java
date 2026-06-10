package SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Symbol table specialized for Flask (application-level) symbols.
 */
public class FlaskSymbolTable extends AbstractSymbolTable {

    private final String sourceFile;
    private final boolean caseSensitive;

    public FlaskSymbolTable(String name, String sourceFile) {
        this(name, sourceFile, true);
    }

    public FlaskSymbolTable(String name, String sourceFile, boolean caseSensitive) {
        super(name);
        this.sourceFile = sourceFile;
        this.caseSensitive = caseSensitive;
    }

    public String getSourceFile() {
        return sourceFile;
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
        sb.append("FlaskSymbolTable[").append(name).append("]");
        if (sourceFile != null) {
            sb.append(" (").append(sourceFile).append(")");
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

    /**
     * Find the deepest (most-nested) symbol with the given name inside this FlaskSymbolTable.
     * Returns Optional.empty() if not found.
     *
     * This method traverses the children scopes and prefers symbols defined in deeper nested
     * scopes (to honor shadowing).
     */
    public Optional<Symbol> findDeepest(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        Result r = findDeepestHelper(this, name, 0);
        return Optional.ofNullable(r.best);
    }

    // Helper result carrying best symbol and its depth
    private static final class Result {
        private Symbol best = null;
        private int depth = -1;
    }

    // Recursive helper: search current table and children; prefer larger depth (more nested)
    private static Result findDeepestHelper(ISymbolTable table, String name, int depth) {
        Result res = new Result();

        // Check local first via public API (respects per-table case sensitivity implementation)
        Optional<Symbol> local = table.lookupLocal(name);
        if (local.isPresent()) {
            res.best = local.get();
            res.depth = depth;
        }

        // If we can access children (we are in same package and table might be AbstractSymbolTable)
        if (table instanceof AbstractSymbolTable abstractTable) {
            for (ISymbolTable child : abstractTable.children) {
                Result childRes = findDeepestHelper(child, name, depth + 1);
                if (childRes.best != null && childRes.depth > res.depth) {
                    res = childRes;
                }
            }
        }

        return res;
    }
}