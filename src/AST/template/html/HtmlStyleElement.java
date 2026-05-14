package AST.template.html;

import AST.ASTNode;
import AST.SourceRange;
import AST.template.css.CssStylesheet;

import java.util.ArrayList;
import java.util.List;

public class HtmlStyleElement extends HtmlElement {

    private final CssStylesheet stylesheet;

    public HtmlStyleElement(List<HtmlAttribute> attributes,
                            CssStylesheet stylesheet,
                            SourceRange sourceRange) {
        super("HtmlStyleElement", "style", attributes, sourceRange);
        this.stylesheet = stylesheet;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> nodes = new ArrayList<>(getAttributes());
        nodes.add(stylesheet);
        return nodes;
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "HtmlStyleElement <style> " + formatLocation();
    }
}
