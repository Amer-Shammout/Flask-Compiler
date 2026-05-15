package AST.literal;

import AST.ASTNode;
import AST.expr.Expression;

import java.util.List;

public class SetLiteralExpr extends Expression {

    private List<Expression> elements;

    public SetLiteralExpr(List<Expression> elements, int line) {
            // TODO(George): Add SourceRange to constructor and store it via ASTNode.
        super("SetLiteralExpr", line);
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
        return "SetLiteralExpr(size=" + elements.size() + ") (line " + lineNumber + ")";
    }

}
