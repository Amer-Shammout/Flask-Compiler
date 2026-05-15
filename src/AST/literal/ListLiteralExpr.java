package AST.literal;

import AST.ASTNode;
import AST.expr.Expression;

import java.util.List;

public class ListLiteralExpr extends Expression {

    private List<Expression> elements;

    public ListLiteralExpr(List<Expression> elements, int line) {
        super("ListLiteralExpr", line);
           this.elements = elements;
           // TODO(George): Add SourceRange to constructor and store it via ASTNode.
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
        return "ListLiteralExpr(size=" + elements.size() + ") (line " + lineNumber + ")";
    }

}
