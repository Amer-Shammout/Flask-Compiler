package SymbolTable;

import java.util.List;
import java.util.Optional;

/**
 * Symbol table specialized for Template-level symbols (Jinja templates).
 *
 * Fields:
 * - `templateName`: the logical name of the template this table represents.
 * - `allowImplicitGlobals`: templates sometimes resolve names from Flask context;
 *    this flag documents whether lookups may consult Flask symbols (via a manager).
 * - `symbols` and `children` are inherited from `AbstractSymbolTable`.
 */
public class TemplateSymbolTable extends AbstractSymbolTable {

    private final String templateName;

    // When true, TemplateSymbolTable consumers may consult FlaskSymbolTable for unresolved names.
    private final boolean allowImplicitGlobals;

    public TemplateSymbolTable(String name, String templateName) {
        this(name, templateName, true);
    }

    public TemplateSymbolTable(String name, String templateName, boolean allowImplicitGlobals) {
        super(name);
        this.templateName = templateName;
        this.allowImplicitGlobals = allowImplicitGlobals;
    }

    public String getTemplateName() { return templateName; }

    public boolean isAllowImplicitGlobals() { return allowImplicitGlobals; }

    @Override
    public boolean define(Symbol symbol) {
        // TODO(Laila): Implement define logic for template-scoped symbols.
        throw new UnsupportedOperationException("TODO: implement TemplateSymbolTable.define");
    }

    @Override
    public Optional<Symbol> lookupLocal(String name) {
        // TODO(Laila): Implement local lookup using `symbols` map.
        throw new UnsupportedOperationException("TODO: implement TemplateSymbolTable.lookupLocal");
    }

    @Override
    public Optional<Symbol> lookup(String name) {
        // TODO(Laila): Implement policy: local -> (optionally) consult Flask via manager.
        throw new UnsupportedOperationException("TODO: implement TemplateSymbolTable.lookup");
    }

    @Override
    public ISymbolTable enterScope(String scopeName) {
        // TODO(Laila): Create nested scope.
        throw new UnsupportedOperationException("TODO: implement TemplateSymbolTable.enterScope");
    }

    @Override
    public ISymbolTable exitScope() {
        // TODO(Laila): Return parent scope or policy-defined value.
        throw new UnsupportedOperationException("TODO: implement TemplateSymbolTable.exitScope");
    }

    @Override
    public List<Symbol> listLocalSymbols() {
        // TODO(Laila): Return list of local symbols.
        throw new UnsupportedOperationException("TODO: implement TemplateSymbolTable.listLocalSymbols");
    }
}
