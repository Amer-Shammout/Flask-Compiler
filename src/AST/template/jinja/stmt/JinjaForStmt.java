package AST.template.jinja.stmt;

import AST.ASTNode;
import AST.template.jinja.JinjaBody;
import AST.template.jinja.expr.JinjaExpr;

import java.util.ArrayList;
import java.util.List;

public class JinjaForStmt extends JinjaStmt {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final List<String> variables;
    private final JinjaExpr iterable;
    private final JinjaBody body;

    public JinjaForStmt(List<String> variables,
                        JinjaExpr iterable,
                        JinjaBody body,
                        AST.SourceRange sourceRange) {
        super("JinjaForStmt", sourceRange);
        this.variables = variables;
        this.iterable = iterable;
        this.body = body;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public List<String> getVariables() {
        return variables;
    }

    public JinjaExpr getIterable() {
        return iterable;
    }

    public JinjaBody getBody() {
        return body;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> list = new ArrayList<>();
        list.add(iterable);
        list.add(body);
        return list;
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaForStmt " + formatLocation();
    }
}
