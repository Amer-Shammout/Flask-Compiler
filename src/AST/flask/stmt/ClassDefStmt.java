package AST.flask.stmt;

import AST.ASTNode;
import AST.SourceRange;
import AST.flask.suite.Suite;

import java.util.ArrayList;
import java.util.List;

public class ClassDefStmt extends Statement {

    private String name;
    private String parent; // may be null
    private Suite body;

    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public ClassDefStmt(String name, String parent,
                        Suite body, SourceRange sourceRange) {
        super("ClassDefStmt", sourceRange);
        this.name = name;
        this.parent = parent;
        this.body = body;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
    }

    public Suite getBody() {
        return body;
    }


    // TODO(George): Ensure every AST node exposes getters for its fields.
    public String getClassName() {
        return name;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(body);
    }


    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "ClassDefStmt(" + name + ") " + formatLocation();
    }

}
