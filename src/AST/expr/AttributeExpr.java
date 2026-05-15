package AST.expr;

import java.util.List;
import AST.ASTNode;

public class AttributeExpr extends Expression {
    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    private final Expression base;
    private final String attribute;

    public AttributeExpr(Expression base, String attribute, int line) {
        super("AttributeExpr", line);
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
        return "AttributeExpr(." + attribute + ") (line " + lineNumber + ")";
    }

}


//obj.x