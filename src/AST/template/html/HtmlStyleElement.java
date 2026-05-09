package AST.template.html;

import AST.ASTNode;
import AST.template.css.CssStylesheet;

import java.util.ArrayList;
import java.util.List;

public class HtmlStyleElement extends HtmlElement {

    private final CssStylesheet stylesheet;

    public HtmlStyleElement(List<HtmlAttribute> attributes,
                            CssStylesheet stylesheet,
                            int lineNumber) {
        super("HtmlStyleElement", "style", attributes, lineNumber);
        this.stylesheet = stylesheet;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> nodes = new ArrayList<>(getAttributes());
        nodes.add(stylesheet);
        return nodes;
    }

    @Override
    public String toString() {
        return "HtmlStyleElement <style> (line " + lineNumber + ")";
    }
}
