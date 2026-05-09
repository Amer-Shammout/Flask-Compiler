package AST.template.html;

import AST.ASTNode;

public class HtmlAttribute extends HtmlNode {

    private final String name;
    private final String value;

    public HtmlAttribute(String name, String value, int lineNumber) {
        super("HtmlAttribute", lineNumber);
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }

    public String getValue() { return value; }

    @Override
    public String toString() {
        return "HtmlAttribute " + name + "=\"" + value + "\" (line " + lineNumber + ")";
    }


}
