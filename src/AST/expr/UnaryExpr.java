package AST.expr;

import AST.ASTNode;

import java.util.List;

public class UnaryExpr extends Expression {

    private String operator;
    private Expression expression;

    public UnaryExpr(String operator, Expression expression, int lineNumber) {
        super("UnaryExpr", lineNumber);
        this.operator = operator;
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    public String getOperator() {
        return operator;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(expression);
    }

    @Override
    public String toString() {
        return "UnaryExpr(operator='" + operator + "') (line " + lineNumber + ")";
    }

}
