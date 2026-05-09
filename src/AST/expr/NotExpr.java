package AST.expr;

import AST.ASTNode;

import java.util.List;

public class NotExpr extends Expression {

    private Expression expr;

    public NotExpr(Expression expr, int line) {
        super("NotExpr", line);
        this.expr = expr;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(expr);
    }
    @Override
    public String toString() {
        return "NotExpr (line " + lineNumber + ")";
    }

}
