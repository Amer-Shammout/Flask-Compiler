package AST.literal;

public class NoneLiteralExpr extends LiteralExpr {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.

    public NoneLiteralExpr(int lineNumber) {
        super("NoneLiteralExpr", lineNumber);
    }
    
    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "NoneLiteralExpr (line " + lineNumber + ")";
    }

}
