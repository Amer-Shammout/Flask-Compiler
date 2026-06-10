package SymbolTable;

import java.util.Optional;

/**
 * Repository holding references to the two primary symbol tables: Flask and Template.
 */
public final class SymbolTableRepository {

    private final FlaskSymbolTable flaskGlobal;
    private final TemplateSymbolTable templateGlobal;

    public SymbolTableRepository(FlaskSymbolTable flaskGlobal, TemplateSymbolTable templateGlobal) {
        this.flaskGlobal = flaskGlobal;
        this.templateGlobal = templateGlobal;
    }

    public FlaskSymbolTable getFlaskGlobal() {
        return flaskGlobal;
    }

    public TemplateSymbolTable getTemplateGlobal() {
        return templateGlobal;
    }

    /**
     * Resolve a name across tables according to project policy.
     * Template-first when hint is {@link SymbolKind#TEMPLATE}, then optional Flask fallback.
     */
    public Optional<Symbol> resolveAcross(String name, SymbolKind namespaceHint) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        if (namespaceHint == SymbolKind.TEMPLATE || namespaceHint == SymbolKind.BLOCK) {
            Optional<Symbol> inTemplate = NameResolver.resolve(templateGlobal, name)
                    .map(ScopeBinding::getSymbol);
            if (inTemplate.isPresent()) {
                return inTemplate;
            }
            if (templateGlobal.isAllowImplicitGlobals()) {
                return NameResolver.resolve(flaskGlobal, name).map(ScopeBinding::getSymbol);
            }
            return Optional.empty();
        }

        return NameResolver.resolve(flaskGlobal, name).map(ScopeBinding::getSymbol);
    }
}