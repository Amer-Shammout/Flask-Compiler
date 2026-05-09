package AST.stmt;

import AST.ASTNode;
import AST.suite.Suite;

import java.util.ArrayList;
import java.util.List;

public class ClassDefStmt extends Statement {

    private String name;
    private String parent; // may be null
    private Suite body;

    public ClassDefStmt(String name, String parent,
                        Suite body, int lineNumber) {
        super("ClassDefStmt", lineNumber);
        this.name = name;
        this.parent = parent;
        this.body = body;
    }
    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
    }

    public Suite getBody() {
        return body;
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of(body);
    }

    @Override
    public String toString() {
        return "ClassDefStmt(" + name + ") (line " + lineNumber + ")";
    }

}
