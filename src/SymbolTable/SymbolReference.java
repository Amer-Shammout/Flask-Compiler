package SymbolTable;

import AST.ASTNode;
import AST.SourceRange;
import semantic.diagnostics.ResolutionStatus;

/**
 * Records one name site (definition or reference) and how it resolved.
 */
public final class SymbolReference {

    private final String name;
    private final SymbolUseKind useKind;
    private final SourceRange location;
    private final ASTNode sourceNode;
    private final String useScopeName;
    private final ResolutionStatus status;
    private final Symbol resolvedSymbol;
    private final String definingScopeName;

    public SymbolReference(
            String name,
            SymbolUseKind useKind,
            SourceRange location,
            ASTNode sourceNode,
            String useScopeName,
            ResolutionStatus status,
            Symbol resolvedSymbol,
            String definingScopeName) {
        this.name = name;
        this.useKind = useKind;
        this.location = location;
        this.sourceNode = sourceNode;
        this.useScopeName = useScopeName;
        this.status = status;
        this.resolvedSymbol = resolvedSymbol;
        this.definingScopeName = definingScopeName;
    }

    public String getName() {
        return name;
    }

    public SymbolUseKind getUseKind() {
        return useKind;
    }

    public SourceRange getLocation() {
        return location;
    }

    public ASTNode getSourceNode() {
        return sourceNode;
    }

    public String getUseScopeName() {
        return useScopeName;
    }

    public ResolutionStatus getStatus() {
        return status;
    }

    public Symbol getResolvedSymbol() {
        return resolvedSymbol;
    }

    public String getDefiningScopeName() {
        return definingScopeName;
    }

    public boolean isResolved() {
        return status.isResolved();
    }

    public boolean isUnresolved() {
        return status == ResolutionStatus.UNDEFINED;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(useKind).append(' ').append(name);
        sb.append(" [").append(status).append(']');
        sb.append(" in ").append(useScopeName);
        if (definingScopeName != null) {
            sb.append(" -> ").append(definingScopeName);
        }
        if (location != null) {
            sb.append(" at ").append(location);
        }
        return sb.toString();
    }
}
