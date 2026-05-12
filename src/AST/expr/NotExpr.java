package AST.expr;

import AST.ASTNode;

import java.util.List;

public class NotExpr extends Expression {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.

    private Expression expr;

    public NotExpr(Expression expr, int line) {
        super("NotExpr", line);
        this.expr = expr;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getExpr() {
        return expr;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(expr);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "NotExpr (line " + lineNumber + ")";
    }

}
