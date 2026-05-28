package AST.flask.stmt;

import AST.SourceRange;

public class PassStmt extends Statement {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public PassStmt(SourceRange sourceRange) {
        super("PassStmt", sourceRange);
    }
}
