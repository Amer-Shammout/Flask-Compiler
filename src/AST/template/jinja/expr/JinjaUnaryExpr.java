package AST.template.jinja.expr;

import AST.ASTNode;

import java.util.List;
import java.util.Set;

public class JinjaUnaryExpr extends JinjaExpr {

    private final String op;      // e.g. "not"
    private final JinjaExpr expr;

    public JinjaUnaryExpr(String op, JinjaExpr expr, int lineNumber) {
        super("JinjaUnaryExpr", lineNumber);
        this.op = op;
        this.expr = expr;
    }

    public String getOp() {
        return op;
    }

    public JinjaExpr getExpr() {
        return expr;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(expr);
    }

    @Override
    public String toString() {
        return "JinjaUnary \"" + op + "\" (line " + lineNumber + ")";
    }
    @Override
    public Set<String> getVariables() {
        return expr.getVariables();
    }


}
