package AST.template.html;

import AST.template.TemplateNode;
import AST.SourceRange;

public abstract class HtmlNode extends TemplateNode {

    // TODO(Ghalia): Add constructor overload with SourceRange and forward to TemplateNode.

    public HtmlNode(String nodeName, SourceRange sourceRange) {
        super(nodeName, sourceRange);
    }
}
