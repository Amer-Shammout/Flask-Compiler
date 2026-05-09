package AST.template.html;

import java.util.List;

public class HtmlVoidElement extends HtmlElement {

    public HtmlVoidElement(String tagName,
                           List<HtmlAttribute> attributes,
                           int lineNumber) {
        super("HtmlVoidElement", tagName, attributes, lineNumber);
    }
}
