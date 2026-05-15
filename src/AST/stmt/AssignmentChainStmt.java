package AST.stmt;

import AST.ASTNode;
import AST.expr.Expression;

import java.util.List;

public class AssignmentChainStmt extends Statement {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.

    private final List<Expression> targets;  // left-to-right
    private final Expression value;          // final RHS

    public AssignmentChainStmt(List<Expression> targets, Expression value, int lineNumber) {
        super("AssignmentChainStmt", lineNumber);
        this.targets = targets;
        this.value = value;
    }

    public Expression getValue() {
        return value;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public List<Expression> getTargets() {
        return targets;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new java.util.ArrayList<>(targets);
        children.add(value);
        return children;
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "AssignmentChainStmt (line " + lineNumber + ")";
    }

}
