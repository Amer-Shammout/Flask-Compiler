package AST.stmt;

import AST.ASTNode;
import AST.expr.Expression;
import AST.suite.Suite;

import java.util.ArrayList;
import java.util.List;

public class WhileStmt extends Statement {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.

    private Expression condition;
    private Suite body;

    public WhileStmt(Expression condition, Suite body, int lineNumber) {
        super("WhileStmt", lineNumber);
        this.condition = condition;
        this.body = body;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getCondition() {
        return condition;
    }

    public Suite getBody() {
        return body;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(condition, body);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "WhileStmt (line " + lineNumber + ")";
    }


}
