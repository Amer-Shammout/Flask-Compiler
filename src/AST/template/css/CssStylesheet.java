package AST.template.css;

import AST.ASTNode;
import AST.SourceRange;

import java.util.ArrayList;
import java.util.List;

public class CssStylesheet extends CssNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final List<ASTNode> contents; // ✔ can be CssNode or JinjaStatement

    public CssStylesheet(List<ASTNode> contents, SourceRange sourceRange) {
        super("CssStylesheet", sourceRange);
        this.contents = contents;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public List<ASTNode> getContents() {
        return contents;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(contents);
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "CssStylesheet " + formatLocation();
    }
}
