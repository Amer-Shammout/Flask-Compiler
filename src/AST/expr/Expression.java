package AST.expr;

import AST.ASTNode;

public abstract class Expression extends ASTNode {

    // TODO(George): Add constructor overload with SourceRange and forward to ASTNode.

    public Expression(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}