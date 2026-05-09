package AST.template.css;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class CssRule extends CssNode {

    private final CssSelector selector;
    private final List<CssNode> blockContents;

    public CssRule(CssSelector selector, List<CssNode> blockContents, int lineNumber) {
        super("CssRule", lineNumber);
        this.selector = selector;
        this.blockContents = blockContents;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> list = new ArrayList<>();
        list.add(selector);
        list.addAll(blockContents);
        return list;
    }

    @Override
    public String toString() {
        return "CssRule (line " + lineNumber + ")";
    }
}
