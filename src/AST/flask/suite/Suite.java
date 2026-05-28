package AST.flask.suite;

import AST.ASTNode;
import AST.SourceRange;

public abstract class Suite extends ASTNode {

    // TODO(George): Add constructor overload with SourceRange and forward to ASTNode.

    public Suite(String nodeName, SourceRange sourceRange) {
        super(nodeName, sourceRange);
    }
}
