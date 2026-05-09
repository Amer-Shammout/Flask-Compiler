package AST.template.jinja.stmt;

import AST.ASTNode;
import AST.template.jinja.JinjaBody;

import java.util.List;

public class JinjaBlockStmt extends JinjaStmt {

    private final String name;
    private final JinjaBody body;

    public JinjaBlockStmt(String name, JinjaBody body, int lineNumber) {
        super("JinjaBlockStmt", lineNumber);
        this.name = name;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public JinjaBody getBody() {
        return body;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(body);
    }

    @Override
    public String toString() {
        return "JinjaBlock \"" + name + "\" (line " + lineNumber + ")";
    }
}
