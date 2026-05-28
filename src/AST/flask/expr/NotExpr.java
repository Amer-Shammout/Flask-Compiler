package AST.flask.expr;

import AST.ASTNode;
import AST.SourceRange;

import java.util.List;

public class NotExpr extends Expression {
    private Expression expr;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public NotExpr(Expression expr, SourceRange sourceRange) {
        super("NotExpr", sourceRange);
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
        return "NotExpr " + formatLocation();
    }

}
