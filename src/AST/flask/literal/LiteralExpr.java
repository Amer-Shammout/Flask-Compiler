package AST.flask.literal;

import AST.flask.expr.Expression;
import AST.SourceRange;

public abstract class LiteralExpr extends Expression {

    // TODO(George): Add constructor overload with SourceRange and forward to ASTNode.

    public LiteralExpr(String nodeName, SourceRange sourceRange) {
        super(nodeName, sourceRange);
    }
}
