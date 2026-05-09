package AST.template.html;

import AST.template.TemplateNode;

public abstract class HtmlNode extends TemplateNode {

    public HtmlNode(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
