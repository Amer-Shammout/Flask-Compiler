package AST.template.html;

import AST.ASTNode;
import java.util.ArrayList;
import java.util.List;

public class HtmlNormalElement extends HtmlElement {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final List<ASTNode> children;

    public HtmlNormalElement(String tagName,
                             List<HtmlAttribute> attributes,
                             List<ASTNode> children,
                             int lineNumber) {

        super("HtmlNormalElement", tagName, attributes, lineNumber);
        this.children = children;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> list = new ArrayList<>();
        list.addAll(attributes);
        list.addAll(children);
        return list;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public List<ASTNode> getChildNodes() {
        return children;
    }
}
