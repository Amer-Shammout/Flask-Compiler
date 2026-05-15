package AST.template.jinja.stmt;

import AST.ASTNode;
import AST.template.jinja.JinjaBody;

import java.util.List;

public class JinjaBlockStmt extends JinjaStmt {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final String name;
    private final JinjaBody body;

    public JinjaBlockStmt(String name, JinjaBody body, int lineNumber) {
        super("JinjaBlockStmt", lineNumber);
        this.name = name;
        this.body = body;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
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

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaBlock \"" + name + "\" (line " + lineNumber + ")";
    }
}
