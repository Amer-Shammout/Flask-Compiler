package AST.stmt;

import AST.ASTNode;
import AST.expr.Expression;

import java.util.List;

public class ReturnStmt extends Statement {

    private Expression value;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public ReturnStmt(Expression value, int lineNumber) {
        super("ReturnStmt", lineNumber);
        this.value = value;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getValue() {
        return value;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(value);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "ReturnStmt (line " + lineNumber + ")";
    }

}
