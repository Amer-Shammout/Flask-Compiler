package AST.template.html;

import java.util.List;

public class HtmlVoidElement extends HtmlElement {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    public HtmlVoidElement(String tagName,
                           List<HtmlAttribute> attributes,
                           int lineNumber) {
        super("HtmlVoidElement", tagName, attributes, lineNumber);
    }
}
