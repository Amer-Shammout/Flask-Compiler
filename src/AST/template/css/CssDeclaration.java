package AST.template.css;

import AST.ASTNode;

import java.util.List;

public class CssDeclaration extends CssNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

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

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public String getProperty() {
        return property;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(value);
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "CssDeclaration " + property + " (line " + lineNumber + ")";
    }
}
