package semantic.bridge;

import AST.Program;
import AST.SourceRange;
import AST.template.TemplateNode;
import SymbolTable.FlaskReferenceIndex;
import SymbolTable.NameResolver;
import SymbolTable.ScopeBinding;
import SymbolTable.Symbol;
import SymbolTable.SymbolKind;
import SymbolTable.SymbolReference;
import SymbolTable.SymbolTableRepository;
import SymbolTable.SymbolUseKind;
import SymbolTable.TemplateReferenceIndex;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.TypeKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Links Flask {@code render_template(...)} context with Jinja template variable references.
 */
public class TemplateContextBridge {

    private final SymbolTableRepository repository;
    private final DiagnosticCollector diagnosticCollector;
    private final Map<String, TemplateContext> contextMap = new LinkedHashMap<>();
    private final Set<String> flaskContextVariables = new LinkedHashSet<>();
    private final CrossContextResolutionIndex resolutionIndex = new CrossContextResolutionIndex();
    private boolean bridged;

    public TemplateContextBridge(SymbolTableRepository repository, DiagnosticCollector diagnosticCollector) {
        this.repository = repository;
        this.diagnosticCollector = diagnosticCollector;
    }

    /**
     * Bridge Flask render context to template references for one template file.
     */
    public void bridge(
            Program program,
            TemplateNode templateRoot,
            FlaskReferenceIndex flaskIndex,
            TemplateReferenceIndex templateIndex) {
        clear();
        String templateFileName = currentTemplateFileName();
        List<RenderTemplateCall> callsForFile = filterCallsForTemplate(
                FlaskContextExtractor.extract(program),
                templateFileName);

        for (RenderTemplateCall call : callsForFile) {
            resolutionIndex.recordRenderCall(call);
            flaskContextVariables.addAll(call.getContextVariableNames());
        }

        Set<String> renderContextKeys = mergeContextKeys(callsForFile);

        if (templateIndex != null) {
            bridgeTemplateReferences(templateFileName, templateIndex, renderContextKeys, callsForFile);
        }

        bridged = true;
    }

    /**
     * Convenience overload when reference indexes are not available.
     */
    public void bridge(Program program, TemplateNode templateRoot) {
        bridge(program, templateRoot, null, null);
    }

    private void bridgeTemplateReferences(
            String templateFileName,
            TemplateReferenceIndex templateIndex,
            Set<String> renderContextKeys,
            List<RenderTemplateCall> callsForFile) {

        for (SymbolReference reference : templateIndex.getReferences()) {
            if (reference.getUseKind() != SymbolUseKind.REFERENCE) {
                continue;
            }

            CrossContextMatch match = resolveReference(
                    templateFileName,
                    reference,
                    renderContextKeys,
                    callsForFile);
            resolutionIndex.recordMatch(match);
            registerFromMatch(match);
        }
    }

    private CrossContextMatch resolveReference(
            String templateFileName,
            SymbolReference reference,
            Set<String> renderContextKeys,
            List<RenderTemplateCall> callsForFile) {

        String name = reference.getName();

        if (!reference.isUnresolved()) {
            return new CrossContextMatch(
                    reference,
                    CrossContextMatch.MatchKind.TEMPLATE_LOCAL,
                    templateFileName,
                    null,
                    null);
        }

        if (renderContextKeys.contains(name)) {
            SourceRange callSite = findCallSiteProviding(callsForFile, name);
            return new CrossContextMatch(
                    reference,
                    CrossContextMatch.MatchKind.FLASK_RENDER_CONTEXT,
                    templateFileName,
                    name,
                    callSite);
        }

        if (!callsForFile.isEmpty()) {
            return new CrossContextMatch(
                    reference,
                    CrossContextMatch.MatchKind.MISSING_FROM_RENDER_CONTEXT,
                    templateFileName,
                    null,
                    null);
        }

        return new CrossContextMatch(
                reference,
                CrossContextMatch.MatchKind.UNRESOLVED,
                templateFileName,
                null,
                null);
    }

    private void registerFromMatch(CrossContextMatch match) {
        String name = match.getTemplateReference().getName();
        SymbolReference ref = match.getTemplateReference();

        switch (match.getMatchKind()) {
            case TEMPLATE_LOCAL -> {
                Symbol templateSymbol = ref.getResolvedSymbol();
                if (templateSymbol == null) {
                    templateSymbol = new Symbol(name, SymbolKind.VARIABLE, currentTemplateFileName());
                }
                contextMap.put(name, new TemplateContext(
                        templateSymbol,
                        null,
                        TemplateContext.SymbolOrigin.LOCAL,
                        TypeKind.UNKNOWN,
                        ref.getDefiningScopeName()));
            }
            case FLASK_RENDER_CONTEXT -> {
                Symbol templateSymbol = new Symbol(name, SymbolKind.VARIABLE, currentTemplateFileName());
                Optional<Symbol> flaskSymbol = lookupFlaskSymbol(name);
                contextMap.put(name, new TemplateContext(
                        templateSymbol,
                        flaskSymbol.orElse(null),
                        TemplateContext.SymbolOrigin.FLASK_CONTEXT,
                        TypeKind.UNKNOWN,
                        match.getFlaskContextKey()));
            }
            case MISSING_FROM_RENDER_CONTEXT -> {
                Symbol templateSymbol = new Symbol(name, SymbolKind.VARIABLE, currentTemplateFileName());
                contextMap.put(name, new TemplateContext(
                        templateSymbol,
                        null,
                        TemplateContext.SymbolOrigin.UNKNOWN,
                        TypeKind.UNKNOWN,
                        "not passed to render_template for " + match.getTemplateFileName()));
            }
            case UNRESOLVED -> {
                Symbol templateSymbol = new Symbol(name, SymbolKind.VARIABLE, currentTemplateFileName());
                contextMap.put(name, new TemplateContext(templateSymbol, TemplateContext.SymbolOrigin.UNKNOWN));
            }
            default -> {
            }
        }
    }

    private static SourceRange findCallSiteProviding(List<RenderTemplateCall> calls, String variableName) {
        for (RenderTemplateCall call : calls) {
            if (call.getContextVariableNames().contains(variableName)) {
                return call.getSourceRange();
            }
        }
        return null;
    }

    private static Set<String> mergeContextKeys(List<RenderTemplateCall> calls) {
        Set<String> keys = new LinkedHashSet<>();
        for (RenderTemplateCall call : calls) {
            keys.addAll(call.getContextVariableNames());
        }
        return keys;
    }

    private static List<RenderTemplateCall> filterCallsForTemplate(
            List<RenderTemplateCall> allCalls,
            String templateFileName) {
        List<RenderTemplateCall> filtered = new ArrayList<>();
        for (RenderTemplateCall call : allCalls) {
            if (templateFileName.equalsIgnoreCase(call.getTemplateName())) {
                filtered.add(call);
            }
        }
        return filtered;
    }

    private String currentTemplateFileName() {
        return repository.getTemplateGlobal().getTemplateName() != null
                ? repository.getTemplateGlobal().getTemplateName()
                : "";
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public Optional<TemplateContext> resolveTemplateSymbol(String templateSymbolName) {
        if (contextMap.containsKey(templateSymbolName)) {
            return Optional.of(contextMap.get(templateSymbolName));
        }
        return Optional.empty();
    }

    public Optional<TemplateContext> resolveTemplateSymbol(Symbol templateSymbol) {
        return resolveTemplateSymbol(templateSymbol.getName());
    }

    public Optional<Symbol> lookupFlaskSymbol(String templateSymbolName) {
        Optional<ScopeBinding> binding = NameResolver.resolve(repository.getFlaskGlobal(), templateSymbolName);
        return binding.map(ScopeBinding::getSymbol);
    }

    public void registerContext(String templateSymbolName, TemplateContext context) {
        contextMap.put(templateSymbolName, context);
    }

    public Map<String, TemplateContext> getAllContexts() {
        return Map.copyOf(contextMap);
    }

    public List<TemplateContext> getContextsByOrigin(TemplateContext.SymbolOrigin origin) {
        List<TemplateContext> result = new ArrayList<>();
        for (TemplateContext ctx : contextMap.values()) {
            if (ctx.getOrigin() == origin) {
                result.add(ctx);
            }
        }
        return result;
    }

    public List<TemplateContext> getFlaskLinkedContexts() {
        List<TemplateContext> result = new ArrayList<>();
        for (TemplateContext ctx : contextMap.values()) {
            if (ctx.isLinkedToFlask()) {
                result.add(ctx);
            }
        }
        return result;
    }

    public void registerFlaskContextVariables(java.util.Collection<String> variableNames) {
        flaskContextVariables.addAll(variableNames);
    }

    public boolean isFlaskContextVariable(String name) {
        return flaskContextVariables.contains(name);
    }

    public boolean isBridged() {
        return bridged;
    }

    public int getContextCount() {
        return contextMap.size();
    }

    public DiagnosticCollector getDiagnosticCollector() {
        return diagnosticCollector;
    }

    public CrossContextResolutionIndex getResolutionIndex() {
        return resolutionIndex;
    }

    public String formatReport() {
        return resolutionIndex.formatReport();
    }

    public void generateUndefinedSymbolDiagnostic(
            String templateSymbolName,
            AST.SourceRange sourceRange,
            List<String> suggestions) {
        String suggestion = null;
        if (suggestions != null && !suggestions.isEmpty()) {
            suggestion = "Did you mean " + suggestions.get(0) + "?";
        }
        diagnosticCollector.reportUndefinedVariable(sourceRange, templateSymbolName, suggestion);
    }

    public void generateShadowingDiagnostic(
            String templateSymbolName,
            String flaskSymbolName,
            AST.SourceRange sourceRange) {
        diagnosticCollector.reportShadowing(
                sourceRange,
                templateSymbolName,
                flaskSymbolName,
                "Flask context");
    }

    public void generateTypeMismatchDiagnostic(
            String templateSymbolName,
            TypeKind expectedType,
            TypeKind usageType,
            AST.SourceRange sourceRange) {
        diagnosticCollector.reportTypeMismatch(
                sourceRange,
                templateSymbolName,
                expectedType,
                usageType,
                "Ensure the template usage matches the Flask variable type.");
    }

    public void clear() {
        contextMap.clear();
        flaskContextVariables.clear();
        resolutionIndex.clear();
        bridged = false;
    }

    @Override
    public String toString() {
        return String.format(
                "TemplateContextBridge { contexts: %d, bridged: %b, flaskVars: %d }",
                contextMap.size(),
                bridged,
                flaskContextVariables.size());
    }
}
