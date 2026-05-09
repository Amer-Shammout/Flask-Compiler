package AST.template.jinja.stmt;

import AST.ASTNode;
import AST.template.jinja.JinjaBody;
import AST.template.jinja.expr.JinjaExpr;

import java.util.ArrayList;
import java.util.List;

public class JinjaForStmt extends JinjaStmt {

    private final List<String> variables;
    private final JinjaExpr iterable;
    private final JinjaBody body;

    public JinjaForStmt(List<String> variables,
                        JinjaExpr iterable,
                        JinjaBody body,
                        int lineNumber) {
        super("JinjaForStmt", lineNumber);
        this.variables = variables;
        this.iterable = iterable;
        this.body = body;
    }

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

    @Override
    public String toString() {
        return "JinjaForStmt (line " + lineNumber + ")";
    }
}
