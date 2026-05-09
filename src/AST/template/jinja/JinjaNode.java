package AST.template.jinja;

import AST.template.TemplateNode;

public abstract class JinjaNode extends TemplateNode {

    public JinjaNode(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
