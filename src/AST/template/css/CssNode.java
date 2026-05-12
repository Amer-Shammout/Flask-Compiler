package AST.template.css;

import AST.template.TemplateNode;

public abstract class CssNode extends TemplateNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    public CssNode(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
