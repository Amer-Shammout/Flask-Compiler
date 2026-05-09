package AST.template.jinja.expr;

import AST.ASTNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JinjaBinaryExpr extends JinjaExpr {

    private final JinjaExpr left;
    private final String op;
    private final JinjaExpr right;

    public JinjaBinaryExpr(JinjaExpr left, String op, JinjaExpr right, int lineNumber) {
        super("JinjaBinaryExpr", lineNumber);
        this.left = left;
        this.op = op;
        this.right = right;
    }

    public JinjaExpr getLeft() {
        return left;
    }

    public String getOp() {
        return op;
    }

    public JinjaExpr getRight() {
        return right;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(left, right);
    }

    @Override
    public String toString() {
        return "JinjaBinary \"" + op + "\" (line " + lineNumber + ")";
    }

    @Override
    public Set<String> getVariables() {
        Set<String> vars = new java.util.HashSet<>();
        vars.addAll(left.getVariables());
        vars.addAll(right.getVariables());
        return vars;
    }

}
