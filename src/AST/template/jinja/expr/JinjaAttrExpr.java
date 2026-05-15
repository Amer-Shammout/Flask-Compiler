package AST.template.jinja.expr;

import AST.ASTNode;

import java.util.List;
import java.util.Set;

public class JinjaAttrExpr extends JinjaExpr {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final JinjaExpr target;


    private final String attribute;

    public JinjaAttrExpr(JinjaExpr target, String attribute, int lineNumber) {
        super("JinjaAttrExpr", lineNumber);
        this.target = target;
        this.attribute = attribute;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public JinjaExpr getTarget() {
        return target;
    }

    public String getAttribute() {
        return attribute;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(target);
    }
    @Override
    public Set<String> getVariables() {
        return target.getVariables();
    }


    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaAttr ." + attribute + " (line " + lineNumber + ")";
    }
}
