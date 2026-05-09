package AST.template.jinja.stmt;

public class JinjaIncludeStmt extends JinjaStmt {

    private final String templateName;

    public JinjaIncludeStmt(String templateName, int lineNumber) {
        super("JinjaIncludeStmt", lineNumber);
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    @Override
    public String toString() {
        return "JinjaInclude \"" + templateName + "\" (line " + lineNumber + ")";
    }
}
