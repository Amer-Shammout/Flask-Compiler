package AST;

import java.util.List;

public abstract class ASTNode {
    // TODO(George): Replace line-only tracking with SourceRange (start/end) and add getters.
    protected String nodeName;
    protected int lineNumber;

    public ASTNode(String nodeName, int lineNumber) {
        this.nodeName = nodeName;
        this.lineNumber = lineNumber;
    }

    public String getNodeName() {
        return nodeName;
    }

    public int getLineNumber() {
        return lineNumber;
    }


    public List<ASTNode> getChildren() {
        return List.of();
    }

    // TODO(George OR 3): Modify toString for AST node.
    @Override
    public String toString() {
        return nodeName + " (line " + lineNumber + ")";
    }

}
