package SymbolTable;

import semantic.diagnostics.ResolutionStatus;

import java.util.Optional;

/**
 * Resolves identifiers through a lexical scope chain ({@link ISymbolTable} parent links).
 */
public final class NameResolver {

    private NameResolver() {
    }

    /**
     * Resolve {@code name} starting from {@code useSiteScope} (innermost scope first).
     */
    public static Optional<ScopeBinding> resolve(ISymbolTable useSiteScope, String name) {
        ISymbolTable current = useSiteScope;
        while (current != null) {
            Optional<Symbol> local = current.lookupLocal(name);
            if (local.isPresent()) {
                boolean shadowsOuter = hasOuterBinding(current, name);
                return Optional.of(new ScopeBinding(local.get(), current, shadowsOuter));
            }
            current = parentOf(current);
        }
        return Optional.empty();
    }

    public static ResolutionStatus toStatus(Optional<ScopeBinding> binding) {
        if (binding.isEmpty()) {
            return ResolutionStatus.UNDEFINED;
        }
        if (binding.get().shadowsOuter()) {
            return ResolutionStatus.SHADOWED;
        }
        return ResolutionStatus.RESOLVED;
    }

    public static String scopeName(ISymbolTable table) {
        if (table instanceof AbstractSymbolTable abstractTable) {
            return abstractTable.getName();
        }
        return "unknown";
    }

    public static ISymbolTable parentOf(ISymbolTable table) {
        if (table instanceof LocalSymbolTable local) {
            return local.getParent();
        }
        return null;
    }

    private static boolean hasOuterBinding(ISymbolTable definingScope, String name) {
        ISymbolTable outer = parentOf(definingScope);
        return outer != null && outer.lookup(name).isPresent();
    }
}
