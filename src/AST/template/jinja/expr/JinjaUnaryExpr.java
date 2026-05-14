package AST.template.jinja.expr;

import AST.ASTNode;

import java.util.List;
import java.util.Set;

public class JinjaUnaryExpr extends JinjaExpr {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final String op;      // e.g. "not"
    private final JinjaExpr expr;

    public JinjaUnaryExpr(String op, JinjaExpr expr, AST.SourceRange sourceRange) {
        super("JinjaUnaryExpr", sourceRange);
        this.op = op;
        this.expr = expr;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
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

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaUnary \"" + op + "\" " + formatLocation();
    }
    @Override
    public Set<String> getVariables() {
        return expr.getVariables();
    }


}
