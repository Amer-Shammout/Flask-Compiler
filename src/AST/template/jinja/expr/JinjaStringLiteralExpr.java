package AST.template.jinja.expr;

import java.util.Set;

public class JinjaStringLiteralExpr extends JinjaExpr {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final String rawText;

    public JinjaStringLiteralExpr(String rawText, int lineNumber) {
        super("JinjaStringLiteralExpr", lineNumber);
        this.rawText = rawText;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public String getRawText() {
        return rawText;
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaString " + rawText + " (line " + lineNumber + ")";
    }
    @Override
    public Set<String> getVariables() {
        return Set.of();
    }

}
