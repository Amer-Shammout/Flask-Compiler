package SymbolTable;

import AST.ASTNode;
import AST.Program;

/**
 * Concrete builder for Template AST symbols. Laila should implement the
 * traversal and registration logic for template/jinja/css/html nodes.
 */
public class TemplateSymbolTableBuilder extends SymbolTableBuilder {

    public TemplateSymbolTableBuilder(SymbolTableRepository repository) {
        super(repository);
        this.activeTable = repository.getTemplateGlobal();
    }

    @Override
    public ISymbolTable build(Program program) {
        // TODO(Laila): Traverse Template AST and populate TemplateSymbolTable.
        throw new UnsupportedOperationException("TODO: implement TemplateSymbolTableBuilder.build");
    }

    @Override
    public void visit(ASTNode node) {
        // TODO(Laila): Dispatch based on Template AST node types.
        throw new UnsupportedOperationException("TODO: implement TemplateSymbolTableBuilder.visit");
    }

    @Override
    public void registerVariable(ISymbolTable table, String name) {
        // TODO(Laila): Register template-scoped variable symbol in the given table.
        throw new UnsupportedOperationException("TODO: implement TemplateSymbolTableBuilder.registerVariable");
    }

    @Override
    public void registerCallable(ISymbolTable table, String name) {
        // TODO(Laila): Register macro/include/block symbols as needed.
        throw new UnsupportedOperationException("TODO: implement TemplateSymbolTableBuilder.registerCallable");
    }
}
