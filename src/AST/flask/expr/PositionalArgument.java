package AST.flask.expr;

import AST.ASTNode;
import AST.SourceRange;

import java.util.List;

public class PositionalArgument extends Argument {

    private final Expression value;

    public PositionalArgument(Expression value, SourceRange sourceRange) {
        super("PositionalArgument", sourceRange);
        this.value = value;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(value);
    }

    @Override
    public String toString() {
        return "PositionalArgument " + formatLocation();
    }
}
