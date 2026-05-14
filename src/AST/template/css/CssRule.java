package AST.template.css;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class CssRule extends CssNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final CssSelector selector;
    private final List<CssNode> blockContents;

    public CssRule(CssSelector selector, List<CssNode> blockContents, AST.SourceRange sourceRange) {
        super("CssRule", sourceRange);
        this.selector = selector;
        this.blockContents = blockContents;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public CssSelector getSelector() {
        return selector;
    }

    public List<CssNode> getBlockContents() {
        return blockContents;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> list = new ArrayList<>();
        list.add(selector);
        list.addAll(blockContents);
        return list;
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "CssRule " + formatLocation();
    }
}
