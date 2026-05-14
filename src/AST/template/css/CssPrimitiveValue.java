package AST.template.css;

public class CssPrimitiveValue extends CssValuePart {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final String text;

    public CssPrimitiveValue(String text, AST.SourceRange sourceRange) {
        super("CssPrimitiveValue", sourceRange);
        this.text = text;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public String getText() {
        return text;
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "CssPrimitiveValue \"" + text + "\" " + formatLocation();
    }
}
