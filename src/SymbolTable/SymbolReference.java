package SymbolTable;

import AST.ASTNode;
import AST.SourceRange;
import semantic.diagnostics.ResolutionStatus;

import java.util.Optional;

/**
 * Records one name site (definition or reference) and how it resolved.
 */
public final class SymbolReference {

    private final String name;
    private final SymbolUseKind useKind;
    private final SourceRange location;
    private final ASTNode sourceNode;
    private final Optional<ISymbolTable> useScope;
    private final String useScopeName;
    private final ResolutionStatus status;
    private final Symbol resolvedSymbol;
    private final String definingScopeName;


    //    With useScope
    public SymbolReference(
            String name,
            SymbolUseKind useKind,
            SourceRange location,
            ASTNode sourceNode,
            String useScopeName,
            ResolutionStatus status,
            Symbol resolvedSymbol,
            String definingScopeName,
            ISymbolTable useScope) {
        this.name = name;
        this.useKind = useKind;
        this.location = location;
        this.sourceNode = sourceNode;
        this.useScopeName = useScopeName;
        this.status = status;
        this.resolvedSymbol = resolvedSymbol;
        this.definingScopeName = definingScopeName;
        this.useScope = Optional.ofNullable(useScope);
    }

    public SymbolReference(
            String name,
            SymbolUseKind useKind,
            SourceRange location,
            ASTNode sourceNode,
            String useScopeName,
            ResolutionStatus status,
            Symbol resolvedSymbol,
            String definingScopeName) {
        this(name, useKind, location, sourceNode, useScopeName, status, resolvedSymbol, definingScopeName, null);

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

    public Optional<ISymbolTable> getUseScope() {
        return useScope;
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
