package AST;

public class SourcePosition {
    private final int line;
    private final int column;

    // TODO(George): Validate line/column inputs if needed.
    public SourcePosition(int line, int column) {
        this.line = line;
        this.column = column;
    }

    // TODO(George): Review whether column should be 0-based or 1-based for reporting.
    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
