package AST.template.css;

import AST.ASTNode;
import AST.SourceRange;
import java.util.ArrayList;
import java.util.List;

public class CssValue extends CssNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    /**
     * Comma-separated groups, each holding the space-separated parts between commas.
     * {@code font-family: Arial, sans-serif} gives {@code [[Arial], [sans-serif]]};
     * {@code margin: 10px 20px} gives {@code [[10px, 20px]]}.
     */
    private final List<List<CssValuePart>> groups;

    /** Every part with the comma structure flattened away. */
    private final List<CssValuePart> parts;

    public CssValue(List<List<CssValuePart>> groups, SourceRange sourceRange) {
        super("CssValue", sourceRange);
        this.groups = groups != null ? groups : new ArrayList<>();
        List<CssValuePart> flattened = new ArrayList<>();
        for (List<CssValuePart> group : this.groups) {
            flattened.addAll(group);
        }
        this.parts = flattened;
    }

    /** Return the comma-separated groups; render a ", " between consecutive groups. */
    public List<List<CssValuePart>> getGroups() {
        return groups;
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
