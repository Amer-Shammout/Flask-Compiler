package AST.template.jinja.stmt;

public class JinjaExtendsStmt extends JinjaStmt {

    // TODO(Ghalia): Add SourceRange to constructor and store it via ASTNode.

    private final String templateName;

    public JinjaExtendsStmt(String templateName, AST.SourceRange sourceRange) {
        super("JinjaExtendsStmt", sourceRange);
        this.templateName = templateName;
    }

    // TODO(Ghalia): Ensure every AST node exposes getters for its fields.
    public String getTemplateName() {
        return templateName;
    }

    // TODO(Ghalia): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "JinjaExtendsStmt \"" + templateName + "\" " + formatLocation();
    }
}
