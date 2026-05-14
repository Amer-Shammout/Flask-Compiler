package AST.template.css;

import AST.template.jinja.expr.JinjaExpr;

public class CssJinjaExpressionValue extends CssValuePart {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final JinjaExpr expr;

    public CssJinjaExpressionValue(JinjaExpr expr, AST.SourceRange sourceRange) {
        super("CssJinjaExpressionValue", sourceRange);
        this.expr = expr;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public JinjaExpr getExpr() {
        return expr;
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "CssJinjaExpressionValue(" + expr + ") " + formatLocation();
    }
}
