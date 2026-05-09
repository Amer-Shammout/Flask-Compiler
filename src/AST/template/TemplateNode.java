package AST.template;

import AST.ASTNode;

public abstract class TemplateNode extends ASTNode {

    public TemplateNode(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
