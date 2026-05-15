package AST.literal;

public class StringLiteralExpr extends LiteralExpr {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    private String value;

    public StringLiteralExpr(String value, int lineNumber) {
        super("StringLiteralExpr", lineNumber);
        this.value = value;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public String getValue() {
        return value;
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "StringLiteralExpr(\"" + value + "\") (line " + lineNumber + ")";
    }

}
