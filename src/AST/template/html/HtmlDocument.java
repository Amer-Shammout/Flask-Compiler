package AST.template.html;

import AST.ASTNode;
import java.util.ArrayList;
import java.util.List;

public class HtmlDocument extends HtmlNode {

    private final List<ASTNode> children;

    public HtmlDocument(List<ASTNode> children, int lineNumber) {
        super("HtmlDocument", lineNumber);
        this.children = children;
    }

    public List<ASTNode> getChildrenNodes() {
        return children;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(children);
    }

    @Override
    public String toString() {
        return "HtmlDocument (line " + lineNumber + ")";
    }
}
