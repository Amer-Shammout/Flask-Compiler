package AST.template.css;

import AST.ASTNode;
import AST.template.jinja.expr.JinjaExpr;

import java.util.ArrayList;
import java.util.List;

public class CssJinjaValueIf extends CssValuePart {

    private final JinjaExpr condition;
    private final CssValue thenValue;
    private final CssValue elseValue; // can be null

    public CssJinjaValueIf(JinjaExpr condition,
                           CssValue thenValue,
                           CssValue elseValue,
                           int lineNumber) {
        super("CssJinjaValueIf", lineNumber);
        this.condition = condition;
        this.thenValue = thenValue;
        this.elseValue = elseValue;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        children.add(condition);
        children.add(thenValue);
        if (elseValue != null) {
            children.add(elseValue);
        }
        return children;
    }

    @Override
    public String toString() {
        return "CssJinjaValueIf (line " + lineNumber + ")";
    }
}
