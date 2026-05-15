package AST.stmt;

import AST.ASTNode;
import AST.SourceRange;
import AST.expr.Expression;
import AST.expr.IdentifierExpr;
import AST.suite.Suite;

import java.util.List;

public class ForStmt extends Statement {

    private IdentifierExpr iterator;
    private Expression iterable;
    private Suite body;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public ForStmt(IdentifierExpr iterator, Expression iterable, Suite body, SourceRange lineNumber) {
        super("ForStmt", lineNumber);
        this.iterator = iterator;
        this.iterable = iterable;
        this.body = body;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public IdentifierExpr getIterator() {
        return iterator;
    }

    public Expression getIterable() {
        return iterable;
    }

    public Suite getBody() {
        return body;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(iterator, iterable, body);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "ForStmt (line " + lineNumber + ")";
    }

}
