package AST.template.html;

import AST.template.TemplateNode;

public abstract class HtmlNode extends TemplateNode {

    // TODO(Ghalia): Add constructor overload with SourceRange and forward to TemplateNode.

    public HtmlNode(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
