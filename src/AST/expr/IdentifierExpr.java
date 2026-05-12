package AST.expr;

public class IdentifierExpr extends Expression {
    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    private String name;

    public IdentifierExpr(String name, int line) {
        super("IdentifierExpr", line);
        this.name = name;
    }

    public String getName() { return name; }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "IdentifierExpr(\"" + name + "\") (line " + lineNumber + ")";
    }

}
