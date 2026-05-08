package AST.template.jinja.stmt;

import AST.ASTNode;
import AST.template.jinja.JinjaBody;
import AST.template.jinja.JinjaNode;
import AST.template.jinja.expr.JinjaExpr;

import java.util.List;

public class JinjaElifClause extends JinjaStmt {

    private final JinjaExpr condition;
    private final JinjaBody body;

    public JinjaElifClause(JinjaExpr condition, JinjaBody body, int lineNumber) {
        super("JinjaElifClause", lineNumber);
        this.condition = condition;
        this.body = body;
    }

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

    @Override
    public String toString() {
        return "JinjaElifClause (line " + lineNumber + ")";
    }
}
