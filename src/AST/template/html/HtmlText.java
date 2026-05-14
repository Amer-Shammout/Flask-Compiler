package AST.template.html;

import AST.SourceRange;

public class HtmlText extends HtmlNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final String text;

    public HtmlText(String text, SourceRange sourceRange) {
        super("HtmlText", sourceRange);
        this.text = text;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public String getText() {
        return text;
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "HtmlText \"" + text + "\" " + formatLocation();
    }


}
