package AST.literal;

public class NoneLiteralExpr extends LiteralExpr {

    public NoneLiteralExpr(int lineNumber) {
        super("NoneLiteralExpr", lineNumber);
    }
    @Override
    public String toString() {
        return "NoneLiteralExpr (line " + lineNumber + ")";
    }

}
