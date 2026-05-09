package AST.template.css;

import AST.ASTNode;

import java.util.List;

public class CssDeclaration extends CssNode {

    private final String property;
    private final CssValue value;

    public CssDeclaration(String property, CssValue value, int lineNumber) {
        super("CssDeclaration", lineNumber);
        this.property = property;
        this.value = value;
    }

    public CssValue getValue() {
        return value;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(value);
    }

    @Override
    public String toString() {
        return "CssDeclaration " + property + " (line " + lineNumber + ")";
    }
}
