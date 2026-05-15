package AST.expr;

import AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class CallExpr extends Expression {

    private final Expression function;
    private final List<Expression> arguments;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
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

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "CallExpr (line " + lineNumber + ")";
    }

}
