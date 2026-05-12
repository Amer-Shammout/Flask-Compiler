package AST.stmt;

public class BreakStmt extends Statement {

    public BreakStmt(int lineNumber) {
        // TODO(George): Add SourceRange to constructor and store it via ASTNode.
        super("BreakStmt", lineNumber);
    }
}
