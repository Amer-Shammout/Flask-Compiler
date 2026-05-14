package AST.template.html;

import AST.SourceRange;
import java.util.List;

public class HtmlVoidElement extends HtmlElement {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    public HtmlVoidElement(String tagName,
                           List<HtmlAttribute> attributes,
                           SourceRange sourceRange) {
        super("HtmlVoidElement", tagName, attributes, sourceRange);
    }
}
