package AST.template.html;

import AST.ASTNode;
import AST.SourceRange;
import java.util.ArrayList;
import java.util.List;

public class HtmlDocument extends HtmlNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final List<ASTNode> children;

    public HtmlDocument(List<ASTNode> children, SourceRange sourceRange) {
        super("HtmlDocument", sourceRange);
        this.children = children;
    }

    public List<ASTNode> getChildrenNodes() {
        return children;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(children);
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "HtmlDocument " + formatLocation();
    }
}
