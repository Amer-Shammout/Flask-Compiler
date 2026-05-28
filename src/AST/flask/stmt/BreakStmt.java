package AST.flask.stmt;

public class BreakStmt extends Statement {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public BreakStmt(AST.SourceRange sourceRange) {
        super("BreakStmt", sourceRange);
    }
}

