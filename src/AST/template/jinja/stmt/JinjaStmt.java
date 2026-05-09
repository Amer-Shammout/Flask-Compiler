package AST.template.jinja.stmt;

import AST.template.jinja.JinjaNode;

public abstract class JinjaStmt extends JinjaNode {

    public JinjaStmt(String nodeName, int lineNumber) {
        super(nodeName, lineNumber);
    }
}
