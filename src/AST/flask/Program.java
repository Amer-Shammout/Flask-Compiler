package AST.flask;

import AST.ASTNode;
import AST.SourceRange;
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

    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(statements);
    }
}
