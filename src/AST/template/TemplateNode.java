package AST.template;

import AST.ASTNode;
import AST.SourceRange;

public abstract class TemplateNode extends ASTNode {

    // TODO(Ghalia): Add constructor overload with SourceRange and forward to ASTNode.

    public TemplateNode(String nodeName, SourceRange sourceRange) {
        super(nodeName, sourceRange);
    }
}
