package AST.stmt;

import AST.ASTNode;
import AST.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class DelStmt extends Statement {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.

    private List<Expression> targets;

    public DelStmt(List<Expression> targets, int line) {
        super("DelStmt", line);
        this.targets = targets;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public List<Expression> getTargets() {
        return targets;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(targets);
    }
}
