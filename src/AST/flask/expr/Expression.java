package AST.flask.expr;

import AST.ASTNode;
import AST.SourceRange;

public abstract class Expression extends ASTNode {

    // TODO(George): Add constructor overload with SourceRange and forward to ASTNode.

    public Expression(String nodeName, SourceRange sourceRange) {
        super(nodeName, sourceRange);
    }
}
