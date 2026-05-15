package AST.template;

import AST.ASTNode;

public abstract class TemplateNode extends ASTNode {

    // TODO(Ghalia): Add constructor overload with SourceRange and forward to ASTNode.

    public TemplateNode(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
