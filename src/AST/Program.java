package AST;

import AST.flask.stmt.Statement;

import java.util.ArrayList;
import java.util.List;

public class Program extends ASTNode {
// TODO(George): Add SourceRange to Program constructor and store it via ASTNode.
    private List<Statement> statements;

    public Program(List<Statement> statements, SourceRange sourceRange) {
        super("Program", sourceRange);
        this.statements = statements;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(statements);
    }
}
