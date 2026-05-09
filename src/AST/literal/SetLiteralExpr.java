package AST.literal;

import AST.ASTNode;
import AST.expr.Expression;

import java.util.List;

public class SetLiteralExpr extends Expression {

    private List<Expression> elements;

    public SetLiteralExpr(List<Expression> elements, int line) {
        super("SetLiteralExpr", line);
        this.elements = elements;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.copyOf(elements);
    }
    @Override
    public String toString() {
        return "SetLiteralExpr(size=" + elements.size() + ") (line " + lineNumber + ")";
    }

}
