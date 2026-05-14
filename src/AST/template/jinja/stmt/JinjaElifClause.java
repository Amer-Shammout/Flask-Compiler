package AST.template.jinja.stmt;

import AST.ASTNode;
import AST.template.jinja.JinjaBody;
import AST.template.jinja.expr.JinjaExpr;

import java.util.List;

public class JinjaElifClause extends JinjaStmt {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final JinjaExpr condition;
    private final JinjaBody body;

    public JinjaElifClause(JinjaExpr condition, JinjaBody body, AST.SourceRange sourceRange) {
        super("JinjaElifClause", sourceRange);
        this.condition = condition;
        this.body = body;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public JinjaExpr getCondition() {
        return condition;
    }

    public JinjaBody getBody() {
        return body;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(condition, body);
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaElifClause " + formatLocation();
    }
}
