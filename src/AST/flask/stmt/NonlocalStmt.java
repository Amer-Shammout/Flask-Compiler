package AST.flask.stmt;

import AST.SourceRange;
import java.util.List;

public class NonlocalStmt extends Statement {

    private final List<String> names;

    public NonlocalStmt(List<String> names, SourceRange sourceRange) {
        super("NonlocalStmt", sourceRange);
        this.names = names;
    }

    public List<String> getNames() {
        return names;
    }
}