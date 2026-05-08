package AST.template.jinja.expr;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JinjaFilterExpr extends JinjaExpr {

    private final JinjaExpr base;
    private final String filterName;
    private final List<JinjaExpr> args;

    public JinjaFilterExpr(JinjaExpr base, String filterName, List<JinjaExpr> args, int lineNumber) {
        super("JinjaFilterExpr", lineNumber);
        this.base = base;
        this.filterName = filterName;
        this.args = args;
    }

    public JinjaExpr getBase() {
        return base;
    }

    public String getFilterName() {
        return filterName;
    }

    public List<JinjaExpr> getArgs() {
        return args;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> list = new ArrayList<>();
        list.add(base);
        list.addAll(args);
        return list;
    }

    @Override
    public String toString() {
        return "JinjaFilter " + filterName + " (line " + lineNumber + ")";
    }
    @Override
    public Set<String> getVariables() {
        Set<String> vars = new java.util.HashSet<>();
        vars.addAll(base.getVariables());
        for (JinjaExpr arg : args) {
            vars.addAll(arg.getVariables());
        }
        return vars;
    }


}
