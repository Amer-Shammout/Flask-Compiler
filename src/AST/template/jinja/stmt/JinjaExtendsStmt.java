package AST.template.jinja.stmt;

public class JinjaExtendsStmt extends JinjaStmt {

    private final String templateName;

    public JinjaExtendsStmt(String templateName, int lineNumber) {
        super("JinjaExtendsStmt", lineNumber);
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    @Override
    public String toString() {
        return "JinjaExtendsStmt \"" + templateName + "\" (line " + lineNumber + ")";
    }
}
