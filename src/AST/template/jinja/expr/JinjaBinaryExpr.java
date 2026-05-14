package AST.template.jinja.expr;

import AST.ASTNode;

import java.util.List;
import java.util.Set;

public class JinjaBinaryExpr extends JinjaExpr {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.    

    private final JinjaExpr left;
    private final String op;
    private final JinjaExpr right;

    public JinjaBinaryExpr(JinjaExpr left, String op, JinjaExpr right, AST.SourceRange sourceRange) {
        super("JinjaBinaryExpr", sourceRange);
        this.left = left;
        this.op = op;
        this.right = right;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
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

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaBinary \"" + op + "\" " + formatLocation();
    }

    @Override
    public Set<String> getVariables() {
        Set<String> vars = new java.util.HashSet<>();
        vars.addAll(left.getVariables());
        vars.addAll(right.getVariables());
        return vars;
    }

}
