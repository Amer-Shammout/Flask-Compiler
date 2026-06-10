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
     * If not found in any scope, checks Python built-ins as a last resort *unless*
     * we're resolving from a Template context (TemplateSymbolTable).
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

        // If the use-site is within template context, DO NOT check runtime/builtins directly.
        if (!isTemplateContext(useSiteScope)) {
            // Runtime scope (IMPORTANT)
            Optional<Symbol> runtime = RuntimeScope.INSTANCE.lookupLocal(name);
            if (runtime.isPresent()) {
                return Optional.of(new ScopeBinding(runtime.get(), RuntimeScope.INSTANCE, false));
            }

            // Last resort: check Python built-ins
            Optional<Symbol> builtin = PythonBuiltins.lookup(name);
            if (builtin.isPresent()) {
                // Create a special marker ISymbolTable that represents the Python built-ins
                return Optional.of(new ScopeBinding(builtin.get(), BuiltinsScope.INSTANCE, false));
            }
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
        if (table instanceof RuntimeScope) {
            return "python-runtime";
        }
        if (table instanceof BuiltinsScope) {
            return "python-builtins";
        }
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

    /**
     * Walk up the parent chain from {@code scope} to see if we are inside a TemplateSymbolTable.
     * If true, we are in template context and should not auto-fall back to python runtime/builtins.
     */
    private static boolean isTemplateContext(ISymbolTable scope) {
        ISymbolTable cur = scope;
        while (cur != null) {
            if (cur instanceof TemplateSymbolTable) {
                return true;
            }
            cur = parentOf(cur);
        }
        return false;
    }
}
