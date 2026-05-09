package AST.expr;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class CompareExpr extends Expression {

    private Expression left;
    private List<String> operators;
    private List<Expression> rights;

    public CompareExpr(
            Expression left,
            List<String> operators,
            List<Expression> rights,
            int lineNumber
    ) {
        super("CompareExpr", lineNumber);
        this.left = left;
        this.operators = operators;
        this.rights = rights;
    }

    public Expression getLeft() {
        return left;
    }

    public List<String> getOperators() {
        return operators;
    }

    public List<Expression> getRights() {
        return rights;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> kids = new ArrayList<>();
        kids.add(left);
        kids.addAll(rights);
        return kids;
    }

    @Override
    public String toString() {
        return "CompareExpr{operators=" + operators + "} (line " + lineNumber + ")";
    }

}
