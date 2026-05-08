package AST.template.jinja.expr;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JinjaCallExpr extends JinjaExpr {

    private final JinjaExpr callee;
    private final List<JinjaExpr> args;

    public JinjaCallExpr(JinjaExpr callee, List<JinjaExpr> args, int lineNumber) {
        super("JinjaCallExpr", lineNumber);
        this.callee = callee;
        this.args = args;
    }

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

    @Override
    public String toString() {
        return "JinjaCall (line " + lineNumber + ")";
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
