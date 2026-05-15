package AST.stmt;

import AST.ASTNode;
import AST.SourceRange;
import AST.expr.Expression;
import java.util.List;

public class AssignmentStmt extends Statement {

    private final Expression target;
    private final Expression value;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public AssignmentStmt(Expression target, Expression value, SourceRange sourceRange) {
        super("AssignmentStmt", sourceRange);
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
        return "AssignmentStmt " + formatLocation();
    }

}
