package AST.template.html;

public class HtmlText extends HtmlNode {

    private final String text;

    public HtmlText(String text, int lineNumber) {
        super("HtmlText", lineNumber);
        this.text = text;
    }

    public String getText() {
        return text;
    }
    @Override
    public String toString() {
        return "HtmlText \"" + text + "\" (line " + lineNumber + ")";
    }


}
