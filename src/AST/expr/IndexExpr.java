package AST.expr;

import AST.ASTNode;
import java.util.List;

public class IndexExpr extends Expression {
    private final Expression base;
    private final Expression index;

    public IndexExpr(Expression base, Expression index, int line) {
        super("IndexExpr", line);
        this.base = base;
        this.index = index;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(base, index);
    }
    @Override
    public String toString() {
        return "IndexExpr (line " + lineNumber + ")";
    }

}


//obj[x]