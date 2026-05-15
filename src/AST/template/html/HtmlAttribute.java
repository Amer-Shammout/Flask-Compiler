package AST.template.html;

import AST.ASTNode;

public class HtmlAttribute extends HtmlNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final String name;
    private final String value;

    public HtmlAttribute(String name, String value, int lineNumber) {
        super("HtmlAttribute", lineNumber);
        this.name = name;
        this.value = value;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public String getName() { return name; }

    public String getValue() { return value; }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "HtmlAttribute " + name + "=\"" + value + "\" (line " + lineNumber + ")";
    }


}
