package SymbolTable;

/**
 * Repository holding references to the two primary symbol tables: Flask and Template.
 *
 * Purpose:
 * - Centralize access when resolution must cross table boundaries.
 * - Provide helper APIs that implement the chosen lookup policy (e.g. template->flask fallback).
 *
 * Laila should implement resolution behavior in `resolveAcross`.
 */
public final class SymbolTableRepository {

    private final FlaskSymbolTable flaskGlobal;
    private final TemplateSymbolTable templateGlobal;

    public SymbolTableRepository(FlaskSymbolTable flaskGlobal, TemplateSymbolTable templateGlobal) {
        this.flaskGlobal = flaskGlobal;
        this.templateGlobal = templateGlobal;
    }

    public FlaskSymbolTable getFlaskGlobal() { return flaskGlobal; }

    public TemplateSymbolTable getTemplateGlobal() { return templateGlobal; }

    /**
     * Resolve a name across tables according to project policy.
     * Example policy: if `namespaceHint` is TEMPLATE, check template table, then
     * optionally consult `flaskGlobal` if not found and fallback allowed.
     *
     * @param name symbol name
     * @param namespaceHint optional hint about where to start (may be null)
     */
    public Symbol resolveAcross(String name, SymbolKind namespaceHint) {
        // TODO(Laila): Implement resolution logic.
        throw new UnsupportedOperationException("TODO: implement cross-table resolution");
    }
}
