package AST.stmt;

import AST.ASTNode;
import AST.expr.Expression;
import java.util.List;

public class AssignmentStmt extends Statement {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.

    private final Expression target;
    private final Expression value;


    public AssignmentStmt(Expression target, Expression value, int lineNumber) {
        super("AssignmentStmt", lineNumber);
        this.target = target;
        this.value = value;
    }

    public Expression getValue() { return value; }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getTarget() {
        return target;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(target, value);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "AssignmentStmt (line " + lineNumber + ")";
    }

}
