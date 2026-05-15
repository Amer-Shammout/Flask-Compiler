package AST.stmt;

public class ContinueStmt extends Statement {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public ContinueStmt(AST.SourceRange sourceRange) {
        super("ContinueStmt", sourceRange);
    }
}
