package AST.template.jinja.expr;

import java.util.Set;

public class JinjaNumberLiteralExpr extends JinjaExpr {

    private final String text;

    public JinjaNumberLiteralExpr(String text, int lineNumber) {
        super("JinjaNumberLiteralExpr", lineNumber);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "JinjaNumber " + text + " (line " + lineNumber + ")";
    }
    @Override
    public Set<String> getVariables() {
        return Set.of();
    }

}
