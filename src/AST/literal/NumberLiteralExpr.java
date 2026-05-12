package AST.literal;

public class NumberLiteralExpr extends LiteralExpr {

    private String value;

    public NumberLiteralExpr(String value, int lineNumber) {
           super("NumberLiteralExpr", lineNumber); 
           // TODO(George): Add SourceRange to constructor and store it via ASTNode.
        this.value = value;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public String getValue() {
        return value;
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "NumberLiteralExpr(" + value + ") (line " + lineNumber + ")";
    }

}
