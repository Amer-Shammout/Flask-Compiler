package AST.expr;

public class IdentifierExpr extends Expression {
    private String name;

    public IdentifierExpr(String name, int line) {
        super("IdentifierExpr", line);
        this.name = name;
    }

    public String getName() { return name; }
    @Override
    public String toString() {
        return "IdentifierExpr(\"" + name + "\") (line " + lineNumber + ")";
    }

}
