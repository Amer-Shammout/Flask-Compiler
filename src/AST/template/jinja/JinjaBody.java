package AST.template.jinja;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class JinjaBody extends JinjaNode {

    private final List<ASTNode> children;

    public JinjaBody(List<ASTNode> children, int lineNumber) {
        super("JinjaBody", lineNumber);
        this.children = children;
    }

    public List<ASTNode> getBodyChildren() {
        return children;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(children);
    }

    @Override
    public String toString() {
        return "JinjaBody (line " + lineNumber + ")";
    }
}
