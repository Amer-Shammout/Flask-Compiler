package AST.template.css;

public class CssPrimitiveValue extends CssValuePart {

    private final String text;

    public CssPrimitiveValue(String text, int lineNumber) {
        super("CssPrimitiveValue", lineNumber);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "CssPrimitiveValue \"" + text + "\" (line " + lineNumber + ")";
    }
}
