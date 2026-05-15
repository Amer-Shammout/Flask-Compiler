package AST.template.jinja.stmt;

import AST.template.jinja.JinjaNode;

public abstract class JinjaStmt extends JinjaNode {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    public JinjaStmt(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
