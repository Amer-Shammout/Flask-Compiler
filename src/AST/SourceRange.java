package AST;

public class SourceRange {
    private final SourcePosition start;
    private final SourcePosition end;

    // TODO(George): Decide how to handle multi-line ranges.
    public SourceRange(SourcePosition start, SourcePosition end) {
        this.start = start;
        this.end = end;
    }

    public SourcePosition getStart() {
        return start;
    }

    public SourcePosition getEnd() {
        return end;
    }
}
