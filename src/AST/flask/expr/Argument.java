package AST.flask.expr;

import AST.ASTNode;
import AST.SourceRange;

public abstract class Argument extends ASTNode {

    protected Argument(String nodeName, SourceRange sourceRange) {
        super(nodeName, sourceRange);
    }
}
