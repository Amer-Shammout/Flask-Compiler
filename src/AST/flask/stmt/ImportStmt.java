package AST.flask.stmt;

import AST.SourceRange;
import java.util.List;

/**
 * Plain {@code import a, b.c} statement (as opposed to {@code from x import y}, see {@link FromImportStmt}).
 * Each entry in {@code modules} is the dotted name exactly as written (e.g. "os" or "os.path").
 * Python semantics bind only the first segment of each dotted name in the enclosing scope
 * (e.g. {@code import os.path} binds the name {@code os}), which is handled at symbol-table build time.
 */
public class ImportStmt extends Statement {

    private final List<String> modules;

    public ImportStmt(List<String> modules, SourceRange sourceRange) {
        super("ImportStmt", sourceRange);
        this.modules = modules;
    }

    public List<String> getModules() {
        return modules;
    }

    @Override
    public String toString() {
        return "ImportStmt " + formatLocation();
    }
}
