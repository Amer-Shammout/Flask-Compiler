package AST.template.css;

import AST.ASTNode;
import AST.template.jinja.expr.JinjaExpr;

import java.util.ArrayList;
import java.util.List;

public class CssSelector extends CssNode {

    private final String selectorText;
    private final List<JinjaExpr> jinjaExpressions;

    public CssSelector(String selectorText, List<JinjaExpr> jinjaExpressions, int lineNumber) {
        super("CssSelector", lineNumber);
        this.selectorText = selectorText;
        this.jinjaExpressions = jinjaExpressions;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(jinjaExpressions);
    }

    @Override
    public String toString() {
        return "CssSelector \"" + selectorText + "\" (line " + lineNumber + ")";
    }
}


