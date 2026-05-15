package AST.expr;

import AST.ASTNode;
import AST.SourceRange;
import java.util.List;

public class LambdaExpr extends Expression {
    private List<String> params;
    private Expression body;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public LambdaExpr(List<String> params, Expression body, SourceRange sourceRange) {
        super("LambdaExpr", sourceRange);
        this.params = params;
        this.body = body;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public List<String> getParams() {
        return params;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getBody() {
        return body;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(body);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "LambdaExpr(params=" + params + ") " + formatLocation();
    }

}
