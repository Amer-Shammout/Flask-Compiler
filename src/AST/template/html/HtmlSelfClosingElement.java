package AST.template.html;

import java.util.List;

public class HtmlSelfClosingElement extends HtmlElement {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    public HtmlSelfClosingElement(String tagName,
                                  List<HtmlAttribute> attributes,
                                  int lineNumber) {
        super("HtmlSelfClosingElement", tagName, attributes, lineNumber);
    }
}
