package AST.template.jinja.expr;

import java.util.Set;

public class JinjaNumberLiteralExpr extends JinjaExpr {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final String text;

    public JinjaNumberLiteralExpr(String text, int lineNumber) {
        super("JinjaNumberLiteralExpr", lineNumber);
        this.text = text;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public String getText() {
        return text;
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaNumber " + text + " (line " + lineNumber + ")";
    }
    @Override
    public Set<String> getVariables() {
        return Set.of();
    }

}
