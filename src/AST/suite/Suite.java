package AST.suite;

import AST.ASTNode;

public abstract class Suite extends ASTNode {

    // TODO(George): Add constructor overload with SourceRange and forward to ASTNode.

    public Suite(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
