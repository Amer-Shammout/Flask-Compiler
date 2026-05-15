package AST.literal;

import AST.SourceRange;

public class NumberLiteralExpr extends LiteralExpr {

    private String value;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public NumberLiteralExpr(String value, SourceRange sourceRange) {
           super("NumberLiteralExpr", sourceRange);
        this.value = value;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public String getValue() {
        return value;
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "NumberLiteralExpr(" + value + ") " + formatLocation();
    }

}
