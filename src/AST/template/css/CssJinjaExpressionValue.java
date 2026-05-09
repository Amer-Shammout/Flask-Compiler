package AST.template.css;

import AST.template.jinja.expr.JinjaExpr;

public class CssJinjaExpressionValue extends CssValuePart {

    private final JinjaExpr expr;

    public CssJinjaExpressionValue(JinjaExpr expr, int lineNumber) {
        super("CssJinjaExpressionValue", lineNumber);
        this.expr = expr;
    }

    public JinjaExpr getExpr() {
        return expr;
    }

    @Override
    public String toString() {
        return "CssJinjaExpressionValue(" + expr + ") (line " + getLineNumber() + ")";
    }
}
