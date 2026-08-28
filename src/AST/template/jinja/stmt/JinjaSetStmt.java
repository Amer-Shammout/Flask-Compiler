package AST.template.jinja.stmt;

import AST.ASTNode;
import AST.SourceRange;
import AST.template.jinja.expr.JinjaExpr;

import java.util.List;

public class JinjaSetStmt extends JinjaStmt {
    private final String name;
    private final JinjaExpr value;

    public JinjaSetStmt(String name, JinjaExpr value, SourceRange sourceRange) {
        super("JinjaSetStmt", sourceRange);
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public JinjaExpr getValue() { return value; }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(value);
    }
}