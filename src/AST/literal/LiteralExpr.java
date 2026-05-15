package AST.literal;

import AST.expr.Expression;

public abstract class LiteralExpr extends Expression {

    // TODO(George): Add constructor overload with SourceRange and forward to ASTNode.

    public LiteralExpr(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
