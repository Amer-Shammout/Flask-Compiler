package AST.template.css;

public abstract class CssValuePart extends CssNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    public CssValuePart(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
