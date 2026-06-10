package SymbolTable;

import java.util.Optional;

/**
 * Helper that tries to resolve attributes/members on classes declared in Flask symbol table.
 *
 * Heuristic:
 * - If baseSymbol.kind == SymbolKind.CLASS => look for child scope named "class:ClassName" and
 *   check its local symbols for attributeName.
 *
 * Returns Optional<Boolean>:
 * - Optional.of(true): attribute exists
 * - Optional.of(false): attribute does not exist (class exists but member not found)
 * - Optional.empty(): cannot determine (no class scope or insufficient info)
 */
public final class AttributeResolver {

    private AttributeResolver() {}

    public static Optional<Boolean> resolveAttribute(SymbolTableRepository repository, Symbol baseSymbol, String attributeName) {
        if (baseSymbol == null || attributeName == null || attributeName.isBlank()) {
            return Optional.empty();
        }

        if (baseSymbol.getKind() == SymbolKind.CLASS) {
            // Find child scope for the class inside flask global
            String classScopeName = "class:" + baseSymbol.getName();
            ISymbolTable flaskGlobal = repository.getFlaskGlobal();
            if (flaskGlobal instanceof AbstractSymbolTable parent) {
                for (ISymbolTable child : parent.children) {
                    if (classScopeName.equals(NameResolver.scopeName(child))) {
                        Optional<Symbol> member = child.lookupLocal(attributeName);
                        return Optional.of(member.isPresent()); // true if found, false otherwise
                    }
                }
            }
            // class exists but we couldn't find its member scope -> we can treat as unknown
            return Optional.of(false);
        }

        // Not a class symbol: cannot reliably resolve attribute here without type info
        return Optional.empty();
    }
}