package AST.template.jinja.stmt;

import AST.ASTNode;
import AST.template.jinja.JinjaBody;
import AST.template.jinja.expr.JinjaExpr;

import java.util.ArrayList;
import java.util.List;

public class JinjaIfStmt extends JinjaStmt {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final JinjaExpr condition;
    private final JinjaBody thenBody;
    private final List<JinjaElifClause> elifClauses;
    private final JinjaBody elseBody; // may be null

    public JinjaIfStmt(JinjaExpr condition,
                       JinjaBody thenBody,
                       List<JinjaElifClause> elifClauses,
                       JinjaBody elseBody,
                       AST.SourceRange sourceRange) {
        super("JinjaIfStmt", sourceRange);
        this.condition = condition;
        this.thenBody = thenBody;
        this.elifClauses = elifClauses;
        this.elseBody = elseBody;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public JinjaExpr getCondition() {
        return condition;
    }

    public JinjaBody getThenBody() {
        return thenBody;
    }

    public List<JinjaElifClause> getElifClauses() {
        return elifClauses;
    }

    public JinjaBody getElseBody() {
        return elseBody;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> list = new ArrayList<>();
        list.add(condition);
        list.add(thenBody);
        list.addAll(elifClauses);
        if (elseBody != null) list.add(elseBody);
        return list;
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaIfStmt " + formatLocation();
    }
}
