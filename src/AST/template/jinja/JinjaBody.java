package AST.template.jinja;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class JinjaBody extends JinjaNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final List<ASTNode> children;

    public JinjaBody(List<ASTNode> children, int lineNumber) {
        super("JinjaBody", lineNumber);
        this.children = children;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public List<ASTNode> getBodyChildren() {
        return children;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(children);
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaBody (line " + lineNumber + ")";
    }
}
