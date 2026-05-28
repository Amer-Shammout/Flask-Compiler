package AST.flask.stmt;

import AST.SourceRange;
import java.util.List;

public class FromImportStmt extends Statement {
    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    private String module;
    private List<String> names;

    public FromImportStmt(String module, List<String> names, SourceRange sourceRange) {
        super("FromImportStmt", sourceRange);
        this.module = module;
        this.names = names;
    }
    // TODO(George): Ensure every AST node exposes getters for its fields.
    public String getModule() {
        return module;
    }

    public List<String> getNames() {
        return names;
    }
    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "FromImportStmt " + formatLocation();
    }

}

