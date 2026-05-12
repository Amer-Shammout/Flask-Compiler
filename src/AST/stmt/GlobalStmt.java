package AST.stmt;

import java.util.List;

public class GlobalStmt extends Statement {

    private List<String> names;

    public GlobalStmt(List<String> names, int line) {
        super("GlobalStmt", line);
        this.names = names;
        }
        // TODO(George): Add SourceRange to constructor and store it via ASTNode.

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public List<String> getNames() {
        return names;
    }
}
