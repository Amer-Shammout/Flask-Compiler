package AST.template.html;

import java.util.List;

public class HtmlSelfClosingElement extends HtmlElement {

    public HtmlSelfClosingElement(String tagName,
                                  List<HtmlAttribute> attributes,
                                  int lineNumber) {
        super("HtmlSelfClosingElement", tagName, attributes, lineNumber);
    }
}
