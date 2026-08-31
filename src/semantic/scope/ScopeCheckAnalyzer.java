package semantic.scope;

import AST.SourceRange;
import SymbolTable.*;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.ErrorCode;

import java.util.*;

/**
 * Analyzer for E202 (Use Before Definition) and E203 (Out Of Scope) errors.
 * Refactored to remove duplication and keep the original semantics intact.
 */
public class ScopeCheckAnalyzer {

    private final SymbolTableRepository repository;
    private final DiagnosticCollector diagnostics;
    private final FlaskSymbolTableBuilder builder;

    public ScopeCheckAnalyzer(SymbolTableRepository repository, DiagnosticCollector diagnostics,
                              FlaskSymbolTableBuilder builder) {
        this.repository = repository;
        this.diagnostics = diagnostics;
        this.builder = builder;
    }

    public void analyze() {
        if (builder == null || repository == null) {
            return;
        }

        FlaskReferenceIndex index = builder.getReferenceIndex();
        if (index == null) {
            return;
        }

        checkUseBeforeDefinition(index);
        checkOutOfScope(index);
        checkNonlocalDeclarations(index);
    }

    private void checkUseBeforeDefinition(FlaskReferenceIndex index) {
        Map<String, Map<String, List<SymbolReference>>> groupedReferences = groupByScopeAndName(index);

        FlaskSymbolTable flaskRoot = getFlaskRoot();
        for (Map.Entry<String, Map<String, List<SymbolReference>>> scopeEntry : groupedReferences.entrySet()) {
            String scopeName = scopeEntry.getKey();
            Set<String> localVars = getLocalVariablesInScope(scopeName, index);

            for (Map.Entry<String, List<SymbolReference>> nameEntry : scopeEntry.getValue().entrySet()) {
                String symbolName = nameEntry.getKey();
                List<SymbolReference> sites = sortReferences(nameEntry.getValue());

                SymbolReference firstDefInSameScope = findFirstDefinition(sites);
                if (hasNonlocalDeclaration(sites)) {
                    continue;
                }

                if (localVars.contains(symbolName)) {
                    reportEarlyReferenceBeforeDefinition(sites, firstDefInSameScope, symbolName);
                    continue;
                }

                reportEarlyReferenceBeforeDefinition(sites, firstDefInSameScope, symbolName, flaskRoot, index);
            }
        }
    }

    private Map<String, Map<String, List<SymbolReference>>> groupByScopeAndName(FlaskReferenceIndex index) {
        Map<String, Map<String, List<SymbolReference>>> grouped = new HashMap<>();

        for (SymbolReference ref : index.getAllSites()) {
            String scopeName = ref.getUseScopeName();
            String symbolName = ref.getName();

            if (scopeName == null || symbolName == null || symbolName.isBlank()) {
                continue;
            }

            grouped
                    .computeIfAbsent(scopeName, ignored -> new HashMap<>())
                    .computeIfAbsent(symbolName, ignored -> new ArrayList<>())
                    .add(ref);
        }

        return grouped;
    }

    private FlaskSymbolTable getFlaskRoot() {
        ISymbolTable flaskGlobal = repository.getFlaskGlobal();
        return flaskGlobal instanceof FlaskSymbolTable table ? table : null;
    }

    private List<SymbolReference> sortReferences(List<SymbolReference> references) {
        List<SymbolReference> copy = new ArrayList<>(references);
        copy.sort(this::compareByLocation);
        return copy;
    }

    private SymbolReference findFirstDefinition(List<SymbolReference> sites) {
        for (SymbolReference ref : sites) {
            if (ref.getUseKind() == SymbolUseKind.DEFINITION) {
                return ref;
            }
        }
        return null;
    }

    private boolean hasNonlocalDeclaration(List<SymbolReference> sites) {
        for (SymbolReference ref : sites) {
            if (ref.getUseKind() == SymbolUseKind.NONLOCAL_DECLARATION) {
                return true;
            }
        }
        return false;
    }

    private void reportEarlyReferenceBeforeDefinition(List<SymbolReference> sites,
                                                      SymbolReference firstDefinition,
                                                      String symbolName) {
        reportEarlyReferenceBeforeDefinition(sites, firstDefinition, symbolName, null, null);
    }

    private void reportEarlyReferenceBeforeDefinition(List<SymbolReference> sites,
                                                      SymbolReference firstDefinition,
                                                      String symbolName,
                                                      FlaskSymbolTable flaskRoot,
                                                      FlaskReferenceIndex index) {
        if (firstDefinition == null) {
            return;
        }

        for (SymbolReference ref : sites) {
            if (ref.getUseKind() != SymbolUseKind.REFERENCE) {
                continue;
            }

            if (compareByLocation(ref, firstDefinition) >= 0) {
                continue;
            }

            if (flaskRoot != null && index != null) {
                Optional<ISymbolTable> useScopeOpt = ref.getUseScope();
                if (useScopeOpt.isPresent() && hasAncestorDefinitionBefore(index, useScopeOpt.get(), symbolName, ref)) {
                    continue;
                }
            }

            emitE202(ref, symbolName);
        }
    }

    /**
     * Ensures that a nonlocal declaration points to a real binding in
     * an enclosing function scope, not global.
     */
    private void checkNonlocalDeclarations(FlaskReferenceIndex index) {
        for (SymbolReference ref : index.getAllSites()) {
            if (ref.getUseKind() != SymbolUseKind.NONLOCAL_DECLARATION) {
                continue;
            }

            String name = ref.getName();
            Optional<ISymbolTable> useScopeOpt = ref.getUseScope();

            if (useScopeOpt.isEmpty() || !hasEnclosingFunctionBinding(index, useScopeOpt.get(), name)) {
                emitNonlocalBindingError(ref, name);
            }
        }
    }

    private boolean hasEnclosingFunctionBinding(FlaskReferenceIndex index, ISymbolTable useScope, String name) {
        ISymbolTable current = NameResolver.parentOf(useScope);

        while (current != null) {
            String currentScopeName = NameResolver.scopeName(current);

            if (currentScopeName == null || isModuleScope(currentScopeName)) {
                break;
            }

            for (SymbolReference def : index.getDefinitions()) {
                if (name.equals(def.getName()) && currentScopeName.equals(def.getUseScopeName())) {
                    return true;
                }
            }

            current = NameResolver.parentOf(current);
        }

        return false;
    }

    private boolean isModuleScope(String scopeName) {
        return scopeName == null || scopeName.equals("flask-global") || scopeName.equals("global");
    }

    private void emitNonlocalBindingError(SymbolReference ref, String symbolName) {
        SourceRange loc = ref.getLocation();
        String message = String.format(
                "nonlocal declaration of '%s' has no binding in any enclosing function scope",
                symbolName
        );
        String hint = "A nonlocal name must be defined in an enclosing function scope, not in the global scope.";
        diagnostics.addDiagnostic(new Diagnostic(loc, ErrorCode.E203_OUT_OF_SCOPE, message, hint));
    }

    /**
     * Returns the set of names that are truly local to a scope.
     * A name is local only if:
     * - it is defined in this scope
     * - and is not declared as global/nonlocal
     */
    private Set<String> getLocalVariablesInScope(String scopeName, FlaskReferenceIndex index) {
        Set<String> localVars = new HashSet<>();
        Set<String> globalVars = new HashSet<>();
        Set<String> nonlocalVars = new HashSet<>();

        for (SymbolReference def : index.getDefinitions()) {
            if (scopeName.equals(def.getUseScopeName())) {
                localVars.add(def.getName());
            }
        }

        for (SymbolReference ref : index.getAllSites()) {
            if (ref.getUseKind() == SymbolUseKind.GLOBAL_DECLARATION &&
                    scopeName.equals(ref.getUseScopeName())) {
                globalVars.add(ref.getName());
            }
        }

        for (SymbolReference ref : index.getAllSites()) {
            if (ref.getUseKind() == SymbolUseKind.NONLOCAL_DECLARATION &&
                    scopeName.equals(ref.getUseScopeName())) {
                nonlocalVars.add(ref.getName());
            }
        }

        localVars.removeAll(globalVars);
        localVars.removeAll(nonlocalVars);

        return localVars;
    }

    private boolean hasAncestorDefinitionBefore(FlaskReferenceIndex index, ISymbolTable useScope, String name,
                                                SymbolReference reference) {
        ISymbolTable current = NameResolver.parentOf(useScope);

        while (current != null) {
            String currentScopeName = NameResolver.scopeName(current);

            for (SymbolReference def : index.getDefinitions()) {
                if (name.equals(def.getName()) &&
                        currentScopeName != null &&
                        currentScopeName.equals(def.getUseScopeName()) &&
                        compareByLocation(def, reference) < 0) {
                    return true;
                }
            }

            current = NameResolver.parentOf(current);
        }

        return false;
    }

    private void checkOutOfScope(FlaskReferenceIndex index) {
        ISymbolTable flaskGlobal = repository.getFlaskGlobal();
        if (!(flaskGlobal instanceof FlaskSymbolTable flaskRoot)) {
            return;
        }

        for (SymbolReference ref : index.getReferences()) {
            String name = ref.getName();
            if (name == null || name.isBlank() || ref.isResolved()) {
                continue;
            }

            Optional<Symbol> deep = flaskRoot.findDeepest(name);
            if (deep.isEmpty()) {
                continue;
            }

            Optional<ISymbolTable> useScopeOpt = ref.getUseScope();
            if (useScopeOpt.isEmpty()) {
                useScopeOpt = Optional.of(flaskRoot);
            }
            Optional<ISymbolTable> definingScopeOpt = ref.getDefiningScope();
            if (definingScopeOpt.isEmpty()) {
                continue;
            }

            ISymbolTable useScope = useScopeOpt.get();
            ISymbolTable definingScope = definingScopeOpt.get();
            String definingScopeName = NameResolver.scopeName(definingScope);

            if (isBuiltInScope(definingScopeName)) {
                continue;
            }

            if (!canAccessDefiningScope(useScope, definingScope)) {
                emitE203(ref, name, definingScopeName);
            }
        }
    }

    private boolean isBuiltInScope(String scopeName) {
        return "python-builtins".equals(scopeName) || "python-runtime".equals(scopeName);
    }

    private boolean canAccessDefiningScope(ISymbolTable useScope, ISymbolTable definingScope) {
        if (useScope == definingScope) {
            return true;
        }

        ISymbolTable current = useScope;
        while (current != null) {
            if (current == definingScope) {
                return true;
            }
            current = NameResolver.parentOf(current);
        }
        return false;
    }

    private int compareByLocation(SymbolReference ref1, SymbolReference ref2) {
        SourceRange loc1 = ref1.getLocation();
        SourceRange loc2 = ref2.getLocation();

        if (loc1 == null && loc2 == null) return 0;
        if (loc1 == null) return -1;
        if (loc2 == null) return 1;

        if (loc1.getStart() == null || loc2.getStart() == null) return 0;

        int lineCompare = Integer.compare(loc1.getStart().getLine(), loc2.getStart().getLine());
        if (lineCompare != 0) {
            return lineCompare;
        }

        return Integer.compare(loc1.getStart().getColumn(), loc2.getStart().getColumn());
    }

    private void emitE202(SymbolReference reference, String symbolName) {
        SourceRange loc = reference.getLocation();
        String message = String.format("Variable '%s' used before definition in this scope", symbolName);
        String hint = "Ensure the variable is defined before it is used.";
        diagnostics.addDiagnostic(new Diagnostic(loc, ErrorCode.E202_USE_BEFORE_DEFINITION, message, hint));
    }

    private void emitE203(SymbolReference reference, String symbolName, String definingScopeName) {
        SourceRange loc = reference.getLocation();
        String message = String.format("Variable '%s' is out of scope (defined in %s)", symbolName, definingScopeName);
        String hint = "The variable was defined in a different scope. Ensure it is accessible in the current context.";
        diagnostics.addDiagnostic(new Diagnostic(loc, ErrorCode.E203_OUT_OF_SCOPE, message, hint));
    }
}