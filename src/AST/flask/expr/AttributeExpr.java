package AST.flask.expr;

import java.util.List;
import AST.ASTNode;
import AST.SourceRange;

public class AttributeExpr extends Expression {
    private final Expression base;
    private final String attribute;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public AttributeExpr(Expression base, String attribute, SourceRange sourceRange) {
        super("AttributeExpr", sourceRange);
        this.base = base;
        this.attribute = attribute;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getBase() {
        return base;
    }

    public String getAttribute() {
        return attribute;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(base);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "AttributeExpr(." + attribute + ") " + formatLocation();
    }

}


//obj.x
