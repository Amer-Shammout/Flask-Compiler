package AST.template.jinja.expr;

import java.util.HashSet;
import java.util.Set;

public class JinjaIdentifierExpr extends JinjaExpr {

    private final String name;

    public JinjaIdentifierExpr(String name, int lineNumber) {
        super("JinjaIdentifierExpr", lineNumber);
        this.name = name;
    }

    public String getName() {
        return name;
    }
    @Override
    public Set<String> getVariables() {
        return Set.of(name);
    }

    @Override
    public String toString() {
        return "JinjaIdentifier \"" + name + "\" (line " + lineNumber + ")";
    }
}
