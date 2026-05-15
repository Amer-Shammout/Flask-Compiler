package SymbolTable;

import AST.ASTNode;
import AST.Program;

/**
 * Concrete builder for Flask AST symbols. Laila should implement the
 * traversal and registration logic for Flask-specific nodes.
 */
public class FlaskSymbolTableBuilder extends SymbolTableBuilder {

    public FlaskSymbolTableBuilder(SymbolTableRepository repository) {
        super(repository);
        this.activeTable = repository.getFlaskGlobal();
    }

    @Override
    public ISymbolTable build(Program program) {
        // TODO(Laila): Traverse Flask AST and populate FlaskSymbolTable.
        throw new UnsupportedOperationException("TODO: implement FlaskSymbolTableBuilder.build");
    }

    @Override
    public void visit(ASTNode node) {
        // TODO(Laila): Dispatch based on Flask AST node types.
        throw new UnsupportedOperationException("TODO: implement FlaskSymbolTableBuilder.visit");
    }

    @Override
    public void registerVariable(ISymbolTable table, String name) {
        // TODO(Laila): Register variable symbol in the given table.
        throw new UnsupportedOperationException("TODO: implement FlaskSymbolTableBuilder.registerVariable");
    }

    @Override
    public void registerCallable(ISymbolTable table, String name) {
        // TODO(Laila): Register function/class symbol in the given table.
        throw new UnsupportedOperationException("TODO: implement FlaskSymbolTableBuilder.registerCallable");
    }
}
