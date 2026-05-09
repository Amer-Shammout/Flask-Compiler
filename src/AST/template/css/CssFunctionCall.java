package AST.template.css;

import AST.ASTNode;

import java.util.List;

public class CssFunctionCall extends CssValuePart {

    private final String name;
    private final CssValue args; // may be null

    public CssFunctionCall(String name, CssValue args, int lineNumber) {
        super("CssFunctionCall", lineNumber);
        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    @Override
    public List<ASTNode> getChildren() {
        return (args != null) ? List.of(args) : List.of();
    }

    @Override
    public String toString() {
        return "CssFunctionCall " + name + " (line " + lineNumber + ")";
    }
}
