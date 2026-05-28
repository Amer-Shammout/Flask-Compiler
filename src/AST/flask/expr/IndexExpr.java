package AST.flask.expr;

import AST.ASTNode;
import AST.SourceRange;
import java.util.List;

public class IndexExpr extends Expression {
    private final Expression base;
    private final Expression index;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public IndexExpr(Expression base, Expression index, SourceRange sourceRange) {
        super("IndexExpr", sourceRange);
        this.base = base;
        this.index = index;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getBase() {
        return base;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getIndex() {
        return index;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(base, index);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "IndexExpr " + formatLocation();
    }

}


//obj[x]
