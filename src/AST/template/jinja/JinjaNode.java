package AST.template.jinja;

import AST.template.TemplateNode;

public abstract class JinjaNode extends TemplateNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    public JinjaNode(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
