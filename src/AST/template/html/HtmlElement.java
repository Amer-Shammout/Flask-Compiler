package AST.template.html;

import AST.ASTNode;
import java.util.ArrayList;
import java.util.List;

public abstract class HtmlElement extends HtmlNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    protected final String tagName;
    protected final List<HtmlAttribute> attributes;

    public HtmlElement(String nodeName,
                       String tagName,
                       List<HtmlAttribute> attributes,
                       int lineNumber) {
        super(nodeName, lineNumber);
        this.tagName = tagName;
        this.attributes = attributes;
    }

    public List<HtmlAttribute> getAttributes() {
        return attributes;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public String getTagName() {
        return tagName;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(attributes);
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return nodeName + " <" + tagName + "> (line " + lineNumber + ")";
    }

}
