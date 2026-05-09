package AST.expr;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class CallExpr extends Expression {

    private final Expression function;
    private final List<Expression> arguments;

    public CallExpr(Expression function, List<Expression> arguments, int line) {
        super("CallExpr", line);
        this.function = function;
        this.arguments = arguments;
    }

    public Expression getFunction() { return function; }
    public List<Expression> getArguments() { return arguments; }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> ch = new ArrayList<>();
        ch.add(function);
        ch.addAll(arguments);
        return ch;
    }
    @Override
    public String toString() {
        return "CallExpr (line " + lineNumber + ")";
    }

}
