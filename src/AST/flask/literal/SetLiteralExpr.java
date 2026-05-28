package AST.flask.literal;

import AST.ASTNode;
import AST.SourceRange;
import AST.flask.expr.Expression;

import java.util.List;

public class SetLiteralExpr extends Expression {

    private List<Expression> elements;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public SetLiteralExpr(List<Expression> elements, SourceRange sourceRange) {
        super("SetLiteralExpr", sourceRange);
        this.elements = elements;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public List<Expression> getElements() {
        return elements;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.copyOf(elements);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "SetLiteralExpr(size=" + elements.size() + ") " + formatLocation();
    }

}
