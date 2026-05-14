package AST;

import java.util.List;

public abstract class ASTNode {
    protected String nodeName;
    protected int lineNumber;
    protected SourceRange sourceRange;

    public ASTNode(String nodeName, int lineNumber) {
        this(nodeName, lineNumber, null);
    }

    public ASTNode(String nodeName, SourceRange sourceRange) {
        this(nodeName,
                sourceRange != null && sourceRange.getStart() != null
                        ? sourceRange.getStart().getLine()
                        : -1,
                sourceRange);
    }

    protected ASTNode(String nodeName, int lineNumber, SourceRange sourceRange) {
        this.nodeName = nodeName;
        this.lineNumber = lineNumber;
        this.sourceRange = sourceRange;
    }

    public String getNodeName() {
        return nodeName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public SourceRange getSourceRange() {
        return sourceRange;
    }

    protected String formatLocation() {
        if (sourceRange != null) {
            return "[" + sourceRange + "]";
        }
        if (lineNumber >= 0) {
            return "(line " + lineNumber + ")";
        }
        return "(unknown location)";
    }


    public List<ASTNode> getChildren() {
        return List.of();
    }

    @Override
    public String toString() {
        return nodeName + " " + formatLocation();
    }

}
