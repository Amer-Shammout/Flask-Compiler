package AST.template.css;

import AST.ASTNode;
import java.util.ArrayList;
import java.util.List;

public class CssValue extends CssNode {

    private final List<CssValuePart> parts;

    public CssValue(List<CssValuePart> parts, int line) {
        super("CssValue",line);
        this.parts = parts;
    }

    public List<CssValuePart> getParts() {
        return parts;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(parts);
    }

    @Override
    public String toString() {
        return "CssValue (line " + lineNumber + ")";
    }
}
