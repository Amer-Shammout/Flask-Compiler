package AST.flask.literal;

import AST.SourceRange;

public class BooleanLiteralExpr extends LiteralExpr {

    private boolean value;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public BooleanLiteralExpr(boolean value, SourceRange sourceRange) {
        super("BooleanLiteralExpr", sourceRange);
        this.value = value;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public boolean getValue() {
        return value;
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "BooleanLiteralExpr(" + value + ") " + formatLocation();
    }

}
