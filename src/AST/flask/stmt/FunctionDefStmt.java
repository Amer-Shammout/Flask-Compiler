package AST.flask.stmt;

import AST.ASTNode;
import AST.SourceRange;
import AST.flask.suite.Suite;
import java.util.List;

public class FunctionDefStmt extends Statement {

    private String name;
    private List<String> parameters;
    private Suite body;



    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public FunctionDefStmt(String name, List<String> parameters,
                           Suite body, SourceRange sourceRange) {
        super("FunctionDefStmt", sourceRange);
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }
    // TODO(George): Ensure every AST node exposes getters for its fields.
    public String getName() {
        return name;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public Suite getBody() {
        return body;
    }


    @Override
    public List<ASTNode> getChildren() {
        return List.of(body);
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "FunctionDefStmt(" + name + ") " + formatLocation();
    }


}
