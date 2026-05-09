package AST.stmt;

import AST.ASTNode;
import AST.expr.Expression;

import java.util.List;

public class AssignmentChainStmt extends Statement {

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

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new java.util.ArrayList<>(targets);
        children.add(value);
        return children;
    }
    @Override
    public String toString() {
        return "AssignmentChainStmt (line " + lineNumber + ")";
    }

}
