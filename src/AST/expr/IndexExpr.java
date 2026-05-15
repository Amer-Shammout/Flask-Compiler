package AST.expr;

import AST.ASTNode;
import java.util.List;

public class IndexExpr extends Expression {
    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    private final Expression base;
    private final Expression index;

    public IndexExpr(Expression base, Expression index, int line) {
        super("IndexExpr", line);
        this.base = base;
        this.index = index;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getBase() {
        return base;
    }

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
        return "IndexExpr (line " + lineNumber + ")";
    }

}


//obj[x]