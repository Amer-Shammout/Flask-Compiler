package AST.stmt;

import AST.ASTNode;
import AST.SourceRange;
import AST.expr.Expression;
import java.util.List;

public class Decorator extends ASTNode {

    private Expression expr;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public Decorator(Expression expr, SourceRange sourceRange) {
        super("Decorator", sourceRange);
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
        return "Decorator " + formatLocation();
    }

}
