package AST.flask.stmt;

import AST.ASTNode;
import AST.SourceRange;
import AST.flask.expr.Expression;

import java.util.List;

public class ExpressionStmt extends Statement {

    private Expression expression;
    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public ExpressionStmt(Expression expression, SourceRange sourceRange) {
        super("ExpressionStmt", sourceRange);
        this.expression = expression;
    }
     // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getExpression() {
        return expression;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(expression);
    }
    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "ExpressionStmt " + formatLocation();
    }

}
