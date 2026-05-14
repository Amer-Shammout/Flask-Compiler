package AST.template.jinja;

import AST.template.TemplateNode;
import AST.SourceRange;

public abstract class JinjaNode extends TemplateNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    public JinjaNode(String nodeName, SourceRange sourceRange) {
        super(nodeName, sourceRange);
    }
}
