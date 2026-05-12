package AST.template.css;

import AST.ASTNode;

import java.util.List;

public class CssFunctionCall extends CssValuePart {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

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

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public CssValue getArgs() {
        return args;
    }

    @Override
    public List<ASTNode> getChildren() {
        return (args != null) ? List.of(args) : List.of();
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "CssFunctionCall " + name + " (line " + lineNumber + ")";
    }
}
