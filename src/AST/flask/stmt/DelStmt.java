package AST.flask.stmt;

import AST.ASTNode;
import AST.SourceRange;
import AST.flask.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class DelStmt extends Statement {

    private List<Expression> targets;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public DelStmt(List<Expression> targets, SourceRange sourceRange) {
        super("DelStmt", sourceRange);
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
