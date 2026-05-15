package AST.stmt;

import AST.ASTNode;
import java.util.ArrayList;
import java.util.List;

public class DecoratedStmt extends Statement {

    private List<Decorator> decorators;
    private Statement target;

    public DecoratedStmt(List<Decorator> decorators,
                         Statement target,
                         int line) {
        super("DecoratedStmt", line);
            // TODO(George): Add SourceRange to constructor and store it via ASTNode.
        this.decorators = decorators;
        this.target = target;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public List<Decorator> getDecorators() {
        return decorators;
    }

    public Statement getTarget() {
        return target;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> list = new ArrayList<>(decorators);
        list.add(target);
        return list;
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "DecoratedStmt (line " + lineNumber + ")";
    }

}
