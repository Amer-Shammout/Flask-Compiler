package semantic.bridge;

import AST.SourceRange;
import SymbolTable.PythonBuiltins;
import SymbolTable.SymbolReference;
import SymbolTable.TemplateReferenceIndex;
import semantic.diagnostics.DiagnosticCollector;

import java.util.List;

/**
 * E004 Error Detector: Missing Flask Variable
 */
public class MissingFlaskVariableChecker {

    private final DiagnosticCollector diagnosticCollector;

    public MissingFlaskVariableChecker(DiagnosticCollector diagnosticCollector) {
        this.diagnosticCollector = diagnosticCollector;
    }

    /**
     * Main entry point: Check for E004 errors by scanning resolution index.
     *
     * @param resolutionIndex Index containing classified cross-context matches from bridge phase.
     * @param templateIndex   TemplateReferenceIndex produced while building template symbol table.
     */
    public void checkMissingFlaskVariables(CrossContextResolutionIndex resolutionIndex, TemplateReferenceIndex templateIndex) {
        if (resolutionIndex == null) {
            return;
        }

        // Get all template references categorized as MISSING_FROM_RENDER_CONTEXT
        List<CrossContextMatch> missingMatches = resolutionIndex.getMatches(CrossContextMatch.MatchKind.MISSING_FROM_RENDER_CONTEXT);

        for (CrossContextMatch match : missingMatches) {

            // Defensive: ensure we have a reference
            SymbolReference templateReference = match.getTemplateReference();
            if (templateReference == null) continue;

            String variableName = templateReference.getName();
            if (variableName == null || variableName.isBlank()) continue;

            // If the template defines this name anywhere (e.g., loop variable),
            // then this is not a missing Flask variable — it's a template-local name (possible out-of-scope).
            if (templateIndex != null) {
                boolean definedInTemplate = templateIndex.getDefinitions().stream().anyMatch(def -> variableName.equals(def.getName()));
                if (definedInTemplate) {
                    // Skip reporting E004 for this name — E203 (out-of-scope) should be reported elsewhere.
                    continue;
                }
            }

            // Skip builtins (e.g. len, sum)
            if (PythonBuiltins.isBuiltin(variableName)) {
                continue;
            }

            reportMissingFlaskVariable(match);
        }
    }

    private void reportMissingFlaskVariable(CrossContextMatch match) {
        SymbolReference templateReference = match.getTemplateReference();
        if (templateReference == null) {
            return;
        }

        String variableName = templateReference.getName();
        if (variableName == null || variableName.isBlank()) {
            return;
        }

        SourceRange sourceRange = templateReference.getLocation();
        String templateFileName = match.getTemplateFileName();

        String suggestion = String.format(
                "Variable '%s' is used in template '%s' but not passed to render_template(). Add '%s' as a keyword argument: render_template(..., %s=value)",
                variableName, templateFileName, variableName, variableName);

        diagnosticCollector.reportMissingFlaskVariable(sourceRange, variableName, suggestion);
    }
}