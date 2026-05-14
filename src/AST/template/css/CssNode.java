package AST.template.css;

import AST.template.TemplateNode;
import AST.SourceRange;

public abstract class CssNode extends TemplateNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    public CssNode(String nodeName, SourceRange sourceRange) {
        super(nodeName, sourceRange);
    }
}
