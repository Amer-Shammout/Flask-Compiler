package AST.expr;

import AST.SourceRange;

public class IdentifierExpr extends Expression {
    private String name;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public IdentifierExpr(String name, SourceRange sourceRange) {
        super("IdentifierExpr", sourceRange);
        this.name = name;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public String getName() { return name; }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "IdentifierExpr(\"" + name + "\") " + formatLocation();
    }

}
