package AST.flask.expr;

import AST.ASTNode;
import AST.SourceRange;

import java.util.List;

public class UnaryExpr extends Expression {
    private String operator;
    private Expression expression;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public UnaryExpr(String operator, Expression expression, SourceRange sourceRange) {
        super("UnaryExpr", sourceRange);
        this.operator = operator;
        this.expression = expression;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getExpression() {
        return expression;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public String getOperator() {
        return operator;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(expression);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "UnaryExpr(operator='" + operator + "') " + formatLocation();
    }

}
