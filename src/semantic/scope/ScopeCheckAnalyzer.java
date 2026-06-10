package semantic.scope;

import AST.SourceRange;
import SymbolTable.*;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.ErrorCode;

import java.util.*;

/**
 * Analyzer for E202 (Use Before Definition) and E203 (Out Of Scope) errors.
 * <p>
 * This analyzer works with the already-built Flask symbol table and reference index
 * to detect:
 * - E202: Variable used before definition in the same scope
 * - E203: Variable referenced outside its valid scope
 * <p>
 * IMPORTANT: This analyzer queries the existing FlaskReferenceIndex built by
 * FlaskSymbolTableBuilder and does NOT attempt to re-build the symbol table.
 */
public class ScopeCheckAnalyzer {

    private final SymbolTableRepository repository;
    private final DiagnosticCollector diagnostics;
    private final FlaskSymbolTableBuilder builder;

    public ScopeCheckAnalyzer(SymbolTableRepository repository, DiagnosticCollector diagnostics, FlaskSymbolTableBuilder builder) {
        this.repository = repository;
        this.diagnostics = diagnostics;
        this.builder = builder;
    }

    /**
     * Perform scope analysis: detect E202 (use before definition) and E203 (out of scope).
     * <p>
     * This method analyzes the already-built reference index to identify scope violations.
     */
    public void analyze() {
        if (builder == null || repository == null) {
            return;
        }

        FlaskReferenceIndex index = builder.getReferenceIndex();
        if (index == null) {
            return;
        }

        // Step 1: Check for E202 (use before definition in same scope)
        checkUseBeforeDefinition(index);

        // Step 2: Check for E203 (out of scope access)
        checkOutOfScope(index);
    }

    /**
     * E202: Variable used before definition in the same scope.
     * <p>
     * Strategy:
     * - Group all references and definitions by symbol name and scope
     * - For each scope and name, collect all sites (definitions & references)
     * - Sort by source location (line, column)
     * - If a REFERENCE appears before a DEFINITION in the same scope, emit E202
     */
    private void checkUseBeforeDefinition(FlaskReferenceIndex index) {
        // Group by (scope name, symbol name) -> list of references sorted by location
        Map<String, Map<String, List<SymbolReference>>> scopedSites = new HashMap<>();

        for (SymbolReference ref : index.getAllSites()) {
            String scopeName = ref.getUseScopeName();
            String symbolName = ref.getName();

            scopedSites.computeIfAbsent(scopeName, k -> new HashMap<>()).computeIfAbsent(symbolName, k -> new ArrayList<>()).add(ref);
        }

        // For each scope+name combination, check if any REFERENCE precedes a DEFINITION
        for (Map.Entry<String, Map<String, List<SymbolReference>>> scopeEntry : scopedSites.entrySet()) {
            Map<String, List<SymbolReference>> nameMap = scopeEntry.getValue();

            for (Map.Entry<String, List<SymbolReference>> nameEntry : nameMap.entrySet()) {
                String symbolName = nameEntry.getKey();
                List<SymbolReference> sites = nameEntry.getValue();

                // Sort by source location (line, then column)
                sites.sort(this::compareByLocation);

                // Find first DEFINITION
                SymbolReference firstDef = null;
                for (SymbolReference ref : sites) {
                    if (ref.getUseKind() == SymbolUseKind.DEFINITION) {
                        firstDef = ref;
                        break;
                    }
                }

                // If there's a definition, check for references before it
                if (firstDef != null) {
                    for (SymbolReference ref : sites) {
                        if (ref.getUseKind() == SymbolUseKind.REFERENCE) {
                            if (compareByLocation(ref, firstDef) < 0) {
                                // This reference precedes the first definition
                                emitE202(ref, symbolName);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * E203: Variable referenced outside its valid scope.
     * <p>
     * Strategy:
     * - For each REFERENCE that is UNRESOLVED, check if there exists a definition somewhere
     * in the Flask symbol table (deep definition).
     * - If a deep definition exists but is not accessible from the use scope -> E203.
     * <p>
     * Notes:
     * - We skip Python runtime/builtins
     * - We don't convert UNDEFINED to E203 when the deep definition is in a 'for' scope
     */
    private void checkOutOfScope(FlaskReferenceIndex index) {
        ISymbolTable flaskGlobal = repository.getFlaskGlobal();
        if (!(flaskGlobal instanceof FlaskSymbolTable flaskRoot)) {
            return;
        }

        for (SymbolReference ref : index.getReferences()) {
            String name = ref.getName();
            if (name == null || name.isBlank()) continue;

            // If the reference was resolved (status != UNDEFINED), skip here — accessible or shadowed
            if (ref.isResolved()) {
                continue;
            }

            // If the reference is unresolved, check if there is a definition somewhere else in the Flask tree
            Optional<Symbol> deep = flaskRoot.findDeepest(name);
            if (deep.isEmpty()) {
                // No deep definition exists; this is a true undefined (E001/E002). Skip here.
                continue;
            }

            // There is a deep definition somewhere -> check accessibility from use-scope
            String useScopeName = ref.getUseScopeName();
            // Map useScopeName to actual ISymbolTable
            ISymbolTable useScope = findScopeByName(flaskRoot, useScopeName);

            // If we cannot map use scope, fallback to global as use site
            if (useScope == null) useScope = flaskRoot;

            // Find defining scope where this name is declared
            ISymbolTable definingScope = findDefiningScope(flaskRoot, name);
            if (definingScope == null) {
                // Can't locate the defining scope object - be conservative and skip
                continue;
            }

            String definingScopeName = NameResolver.scopeName(definingScope);
            // Skip builtins/runtime as they are considered accessible
            if ("python-builtins".equals(definingScopeName) || "python-runtime".equals(definingScopeName)) {
                continue;
            }

            // Special-case loop variables: do not report E203 when definition is in a 'for' scope
            if (definingScopeName != null && definingScopeName.contains("for")) {
                continue;
            }

            // Check accessibility: a use-scope can access the defining scope when definingScope is an ancestor of useScope (or same scope)
            if (!canAccessDefiningScope(useScope, definingScope)) {
                emitE203(ref, name, definingScopeName);
            }
        }
    }

    /**
     * Search for a scope by name in the scope tree (BFS).
     * Returns the ISymbolTable with the given name, or null if not found.
     */
    private ISymbolTable findScope(ISymbolTable root, String scopeName) {
        if (root == null || scopeName == null) return null;

        Queue<ISymbolTable> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            ISymbolTable current = queue.poll();
            String currentName = NameResolver.scopeName(current);

            if (scopeName.equals(currentName)) {
                return current;
            }

            if (current instanceof AbstractSymbolTable abstractTable) {
                for (ISymbolTable child : abstractTable.getChildren()) {
                    queue.offer(child);
                }
            }
        }

        return null;
    }

    // Convenience wrapper with clearer name
    private ISymbolTable findScopeByName(ISymbolTable root, String scopeName) {
        return findScope(root, scopeName);
    }

    /**
     * Find the scope that defines a given name by searching lookupLocal across the tree.
     * Returns the first scope found (depth-first search preferring higher nodes to find the defining scope).
     */
    private ISymbolTable findDefiningScope(ISymbolTable root, String name) {
        if (root == null || name == null) return null;
        if (root.lookupLocal(name).isPresent()) {
            return root;
        }
        if (root instanceof AbstractSymbolTable abstractTable) {
            for (ISymbolTable child : abstractTable.getChildren()) {
                ISymbolTable found = findDefiningScope(child, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Check if useScope can access definingScope.
     * <p>
     * Rules:
     * - If useScope == definingScope: YES (same scope)
     * - If definingScope is ancestor of useScope: YES
     * - Otherwise: NO
     */
    private boolean canAccessDefiningScope(ISymbolTable useScope, ISymbolTable definingScope) {
        if (useScope == definingScope) {
            return true;
        }

        ISymbolTable cur = useScope;
        while (cur != null) {
            if (cur == definingScope) return true;
            cur = NameResolver.parentOf(cur);
        }
        return false;
    }

    /**
     * Compare two SymbolReferences by source location (line, then column).
     * Returns:
     * - negative if ref1 comes before ref2
     * - 0 if same location
     * - positive if ref1 comes after ref2
     */
    private int compareByLocation(SymbolReference ref1, SymbolReference ref2) {
        SourceRange loc1 = ref1.getLocation();
        SourceRange loc2 = ref2.getLocation();

        if (loc1 == null && loc2 == null) return 0;
        if (loc1 == null) return -1;
        if (loc2 == null) return 1;

        if (loc1.getStart() == null || loc2.getStart() == null) return 0;

        int lineCompare = Integer.compare(loc1.getStart().getLine(), loc2.getStart().getLine());
        if (lineCompare != 0) return lineCompare;

        return Integer.compare(loc1.getStart().getColumn(), loc2.getStart().getColumn());
    }

    /**
     * Emit diagnostic E202: Variable used before definition.
     */
    private void emitE202(SymbolReference reference, String symbolName) {
        SourceRange loc = reference.getLocation();
        String message = String.format("Variable '%s' used before definition in this scope", symbolName);
        String hint = "Ensure the variable is defined before it is used.";
        diagnostics.addDiagnostic(new Diagnostic(loc, ErrorCode.E202_USE_BEFORE_DEFINITION, message, hint));
    }

    /**
     * Emit diagnostic E203: Variable referenced out of scope.
     */
    private void emitE203(SymbolReference reference, String symbolName, String definingScopeName) {
        SourceRange loc = reference.getLocation();
        String message = String.format("Variable '%s' is out of scope (defined in %s)", symbolName, definingScopeName);
        String hint = "The variable was defined in a different scope. Ensure it is accessible in the current context.";
        diagnostics.addDiagnostic(new Diagnostic(loc, ErrorCode.E203_OUT_OF_SCOPE, message, hint));
    }
}