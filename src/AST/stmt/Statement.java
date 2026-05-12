package AST.stmt;

import AST.ASTNode;

public abstract class Statement extends ASTNode {

    // TODO(George): Add constructor overload with SourceRange and forward to ASTNode.

    public Statement(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
