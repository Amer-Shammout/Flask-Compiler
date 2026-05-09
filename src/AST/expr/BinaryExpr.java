package AST.expr;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class BinaryExpr extends Expression {

    private Expression left;
    private String operator;
    private Expression right;

    public BinaryExpr(Expression left, String operator, Expression right, int lineNumber) {
        super("BinaryExpr", lineNumber);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public Expression getLeft() {
        return left;
    }

    public String getOperator() {
        return operator;
    }

    public Expression getRight() {
        return right;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        if (left != null) children.add(left);
        if (right != null) children.add(right);
        return children;
    }

    @Override
    public String toString() {
        return "BinaryExpr{operator='" + operator + "'} (line " + lineNumber + ")";
    }

}
