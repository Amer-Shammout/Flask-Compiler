package AST.template.css;

import AST.ASTNode;
import AST.template.jinja.expr.JinjaExpr;

import java.util.ArrayList;
import java.util.List;

public class CssSelector extends CssNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final String selectorText;
    private final List<JinjaExpr> jinjaExpressions;

    public CssSelector(String selectorText, List<JinjaExpr> jinjaExpressions, int lineNumber) {
        super("CssSelector", lineNumber);
        this.selectorText = selectorText;
        this.jinjaExpressions = jinjaExpressions;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public String getSelectorText() {
        return selectorText;
    }

    public List<JinjaExpr> getJinjaExpressions() {
        return jinjaExpressions;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(jinjaExpressions);
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "CssSelector \"" + selectorText + "\" (line " + lineNumber + ")";
    }
}


