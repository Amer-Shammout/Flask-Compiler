package AST.template.jinja.expr;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JinjaCallExpr extends JinjaExpr {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final JinjaExpr callee;
    private final List<JinjaExpr> args;

    public JinjaCallExpr(JinjaExpr callee, List<JinjaExpr> args, AST.SourceRange sourceRange) {
        super("JinjaCallExpr", sourceRange);
        this.callee = callee;
        this.args = args;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public JinjaExpr getCallee() {
        return callee;
    }

    public List<JinjaExpr> getArgs() {
        return args;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> list = new ArrayList<>();
        list.add(callee);
        list.addAll(args);
        return list;
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaCall " + formatLocation();
    }
    @Override
    public Set<String> getVariables() {
        Set<String> vars = new java.util.HashSet<>();
        vars.addAll(callee.getVariables());
        for (JinjaExpr arg : args) {
            vars.addAll(arg.getVariables());
        }
        return vars;
    }


}
