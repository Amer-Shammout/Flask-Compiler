package SymbolTable;

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
}