package semantic.scope;

import AST.SourceRange;
import SymbolTable.*;
import semantic.bridge.CrossContextResolutionIndex;
import semantic.bridge.CrossContextMatch;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.ErrorCode;
import semantic.diagnostics.Diagnostic;

public class TemplateUndefinedVariableChecker {

    private final DiagnosticCollector diagnostics;
    private final SymbolTableRepository repository;

    public TemplateUndefinedVariableChecker(SymbolTableRepository repository,
                                            DiagnosticCollector diagnostics) {
        this.repository = repository;
        this.diagnostics = diagnostics;
    }

    /**
     * Check unresolved references that are neither defined in template nor in Flask.
     */
    public void checkUndefinedVariables(CrossContextResolutionIndex resolutionIndex,
                                        TemplateReferenceIndex templateIndex) {
        if (resolutionIndex == null) return;

        for (CrossContextMatch match : resolutionIndex.getMatches(CrossContextMatch.MatchKind.UNRESOLVED)) {
            SymbolReference ref = match.getTemplateReference();
            if (ref == null) continue;

            String name = ref.getName();
            if (name == null || name.isBlank()) continue;

            if (templateIndex != null) {
                boolean definedInTemplate = templateIndex.getDefinitions().stream()
                        .anyMatch(def -> name.equals(def.getName()));
                if (definedInTemplate) {
                    continue;   // Handle as E203
                }
            }

//            handle as E004
            if (existsInFlask(name)) {
                continue;
            }

            if (PythonBuiltins.isBuiltin(name)) {
                continue;
            }

            SourceRange src = ref.getLocation();
            String message = String.format("Undefined variable '%s'", name);
            String hint = "Define this variable in the template or pass it from Flask via render_template().";
            diagnostics.reportUndefinedVariable(src, name, hint);
        }
    }

    private boolean existsInFlask(String name) {
        if (repository.getFlaskGlobal() == null) return false;
        return NameResolver.resolve(repository.getFlaskGlobal(), name).isPresent();
    }
}