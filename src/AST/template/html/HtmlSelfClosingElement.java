package AST.template.html;

import AST.SourceRange;
import java.util.List;

public class HtmlSelfClosingElement extends HtmlElement {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    public HtmlSelfClosingElement(String tagName,
                                  List<HtmlAttribute> attributes,
                                  SourceRange sourceRange) {
        super("HtmlSelfClosingElement", tagName, attributes, sourceRange);
    }
}
