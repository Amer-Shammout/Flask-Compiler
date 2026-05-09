package AST.stmt;

import AST.ASTNode;
import AST.expr.Expression;
import AST.suite.Suite;

import java.util.ArrayList;
import java.util.List;

public class WhileStmt extends Statement {

    private Expression condition;
    private Suite body;

    public WhileStmt(Expression condition, Suite body, int lineNumber) {
        super("WhileStmt", lineNumber);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(condition, body);
    }

    @Override
    public String toString() {
        return "WhileStmt (line " + lineNumber + ")";
    }


}
