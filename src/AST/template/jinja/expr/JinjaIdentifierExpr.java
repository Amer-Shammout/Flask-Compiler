package AST.template.jinja.expr;

import java.util.HashSet;
import java.util.Set;

public class JinjaIdentifierExpr extends JinjaExpr {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final String name;

    public JinjaIdentifierExpr(String name, int lineNumber) {
        super("JinjaIdentifierExpr", lineNumber);
        this.name = name;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public String getName() {
        return name;
    }
    @Override
    public Set<String> getVariables() {
        return Set.of(name);
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaIdentifier \"" + name + "\" (line " + lineNumber + ")";
    }
}
