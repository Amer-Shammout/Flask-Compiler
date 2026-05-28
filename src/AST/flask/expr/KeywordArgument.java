package AST.flask.expr;

import AST.ASTNode;
import AST.SourceRange;

import java.util.List;

public class KeywordArgument extends Argument {

    private final String name;
    private final Expression value;

    public KeywordArgument(String name, Expression value, SourceRange sourceRange) {
        super("KeywordArgument", sourceRange);
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
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
        return "KeywordArgument(" + name + ") " + formatLocation();
    }
}
