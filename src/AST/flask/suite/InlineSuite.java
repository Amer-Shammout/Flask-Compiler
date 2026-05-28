package AST.flask.suite;

import AST.ASTNode;
import AST.SourceRange;
import AST.flask.stmt.Statement;

import java.util.List;

public class InlineSuite extends Suite {

    private Statement statement;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public InlineSuite(Statement statement, SourceRange sourceRange) {
        super("InlineSuite", sourceRange);
        this.statement = statement;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Statement getStatement() {
        return statement;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(statement);
    }

}
