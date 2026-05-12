package AST.stmt;

public class PassStmt extends Statement {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.

    public PassStmt(int lineNumber) {
        super("PassStmt", lineNumber);
    }
}