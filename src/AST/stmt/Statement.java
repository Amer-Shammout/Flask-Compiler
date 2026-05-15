package AST.stmt;

import AST.ASTNode;
import AST.SourceRange;

public abstract class Statement extends ASTNode {

    // TODO(George): Add constructor overload with SourceRange and forward to ASTNode.

    public Statement(String nodeName, SourceRange sourceRange) {
        super(nodeName, sourceRange);
    }
}
