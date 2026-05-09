package AST.template.css;

import AST.template.TemplateNode;

public abstract class CssNode extends TemplateNode {

    public CssNode(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
