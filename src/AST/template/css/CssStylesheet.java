package AST.template.css;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class CssStylesheet extends CssNode {

    private final List<ASTNode> contents; // ✔ can be CssNode or JinjaStatement

    public CssStylesheet(List<ASTNode> contents, int line) {
        super("CssStylesheet", line);
        this.contents = contents;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(contents);
    }

    @Override
    public String toString() {
        return "CssStylesheet (line " + lineNumber + ")";
    }
}
