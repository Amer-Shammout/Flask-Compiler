package SymbolTable;

import AST.flask.Program;
import AST.ASTNode;

/**
 * SymbolTableBuilder: responsible for walking the AST and building the
 * two symbol tables required by the compiler (Laila).
 *
 * This class contains method signatures and documentation only. Laila
 * should implement the algorithms (no implementations here).
 *
 * Recommended usage:
 * - Keep Flask and Template tables separate.
 * - Use SymbolTableRepository when a template may resolve values from Flask.
 * - Make this class the main orchestrator for semantic events.
 */
public abstract class SymbolTableBuilder {

    // Repository holding the two top-level tables and optional cross-resolution policy.
    protected final SymbolTableRepository repository;

    // Active table used while traversing the current AST context.
    protected ISymbolTable activeTable;

    public SymbolTableBuilder(SymbolTableRepository repository) {
        this.repository = repository;
        this.activeTable = repository.getFlaskGlobal();
    }

    /**
     * Build symbol tables for the given program AST.
     * @param program top-level AST Program node
    * @return the populated top-level table or repository-owned result
     */
    public abstract ISymbolTable build(Program program);

    /**
     * Visit a generic AST node and update symbol tables accordingly.
     * Implemented as a dispatch in Laila (visitor pattern recommended).
     * @param node AST node to process
     */
    public abstract void visit(ASTNode node);

}
