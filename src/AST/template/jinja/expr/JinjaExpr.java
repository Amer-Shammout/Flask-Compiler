package AST.template.jinja.expr;

import AST.ASTNode;
import AST.template.jinja.JinjaNode;

import java.util.HashSet;
import java.util.Set;

public abstract class JinjaExpr extends JinjaNode {

    public JinjaExpr(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
    public Set<String> getVariables() {
        Set<String> vars = new HashSet<>();
        for (ASTNode child : getChildren()) {
            if (child instanceof JinjaExpr expr) {
                vars.addAll(expr.getVariables());
            }
        }
        return vars;
    }
}
