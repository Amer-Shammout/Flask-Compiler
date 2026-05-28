package AST.flask.expr;

import AST.ASTNode;
import AST.SourceRange;

import java.util.ArrayList;
import java.util.List;

public class CallExpr extends Expression {

    private final Expression function;
    private final List<Argument> arguments;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public CallExpr(Expression function, List<Argument> arguments, SourceRange sourceRange) {
        super("CallExpr", sourceRange);
        this.function = function;
        this.arguments = arguments;
    }

    public Expression getFunction() { return function; }
    public List<Argument> getArguments() { return arguments; }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> ch = new ArrayList<>();
        ch.add(function);
        ch.addAll(arguments);
        return ch;
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "CallExpr " + formatLocation();
    }

}
