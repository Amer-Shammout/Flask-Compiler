package AST.flask.stmt;

import AST.SourceRange;
import java.util.List;

public class GlobalStmt extends Statement {

    private List<String> names;
     // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public GlobalStmt(List<String> names, SourceRange sourceRange) {
        super("GlobalStmt", sourceRange);
        this.names = names;
    }
    // TODO(George): Ensure every AST node exposes getters for its fields.
    public List<String> getNames() {
        return names;
    }
}
