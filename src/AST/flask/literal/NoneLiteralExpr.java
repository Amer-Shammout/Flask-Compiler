package AST.flask.literal;

import AST.SourceRange;

public class NoneLiteralExpr extends LiteralExpr {

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public NoneLiteralExpr(SourceRange sourceRange) {
        super("NoneLiteralExpr", sourceRange);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "NoneLiteralExpr " + formatLocation();
    }

}
