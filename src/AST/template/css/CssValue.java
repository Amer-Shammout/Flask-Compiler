package AST.template.css;

import AST.ASTNode;
import AST.SourceRange;
import java.util.ArrayList;
import java.util.List;

public class CssValue extends CssNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final List<CssValuePart> parts;

    public CssValue(List<CssValuePart> parts, SourceRange sourceRange) {
        super("CssValue", sourceRange);
        this.parts = parts;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public List<CssValuePart> getParts() {
        return parts;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(parts);
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "CssValue " + formatLocation();
    }
}
