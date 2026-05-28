package AST.flask.suite;

import AST.ASTNode;
import AST.SourceRange;
import AST.flask.stmt.Statement;

import java.util.ArrayList;
import java.util.List;

public class BlockSuite extends Suite {

    private List<Statement> statements;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public BlockSuite(List<Statement> statements, SourceRange sourceRange) {
        super("BlockSuite", sourceRange);
        this.statements = statements;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(statements);
    }
}
