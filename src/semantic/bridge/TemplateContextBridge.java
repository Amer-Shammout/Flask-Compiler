package semantic.bridge;


import AST.ASTNode;
import AST.flask.Program;
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
import SymbolTable.FlaskSymbolTable;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.TypeKind;
import semantic.diagnostics.ErrorCode;
import semantic.scope.TemplateUndefinedVariableChecker;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final Map<String, TypeKind> contextVariableTypeCache = new HashMap<>();
    private final Set<String> flaskContextVariables = new LinkedHashSet<>();
    private final CrossContextResolutionIndex resolutionIndex = new CrossContextResolutionIndex();
    private final BridgeTypeChecker typeChecker;
    private final MissingFlaskVariableChecker missingVariableChecker;
    private boolean bridged;

    private final TemplateUndefinedVariableChecker undefinedChecker;

    public TemplateContextBridge(SymbolTableRepository repository, DiagnosticCollector diagnosticCollector) {
        this.repository = repository;
        this.diagnosticCollector = diagnosticCollector;
        this.typeChecker = new BridgeTypeChecker(repository, diagnosticCollector);
        this.missingVariableChecker = new MissingFlaskVariableChecker(diagnosticCollector, repository);
        this.undefinedChecker = new TemplateUndefinedVariableChecker(repository, diagnosticCollector);
    }

    /**
     * Bridge Flask render context to template references for one template file.
     */
    public void bridge(Program program, TemplateNode templateRoot, FlaskReferenceIndex flaskIndex, TemplateReferenceIndex templateIndex) {
        clear();

        String templateFileName = currentTemplateFileName();
        List<RenderTemplateCall> callsForFile = filterCallsForTemplate(FlaskContextExtractor.extract(program), templateFileName);

        // Track Flask context variables and their types
        for (RenderTemplateCall call : callsForFile) {
            resolutionIndex.recordRenderCall(call);
            flaskContextVariables.addAll(call.getContextVariableNames());

            for (String varName : call.getContextVariableNames()) {
                TypeKind inferredType = call.getContextVariableType(varName);

                // If type is UNKNOWN (from variable reference), look up in Flask Symbol Table
                if (inferredType == TypeKind.UNKNOWN) {
                    inferredType = resolveContextVariableType(varName, program);
                }

                // Store the resolved type
                if (inferredType != TypeKind.UNKNOWN) {
                    contextVariableTypeCache.put(varName, inferredType);
                }
            }
        }

        Set<String> renderContextKeys = mergeContextKeys(callsForFile);

        // Perform template reference resolution
        if (templateIndex != null) {
            bridgeTemplateReferences(templateFileName, templateIndex, renderContextKeys, callsForFile);
        }

        // Perform cross-context type checking
        if (templateRoot != null && !callsForFile.isEmpty()) {
            typeChecker.checkCrossContextTypes(templateRoot, this);
        }

        // E004 Detection: Check for missing Flask variables (CRITICAL: must run after bridge)
        missingVariableChecker.checkMissingFlaskVariables(resolutionIndex, templateIndex);
        // E001 Detection: Check for Undefined template variables (CRITICAL: must run after checkMissingFlaskVariables)
        undefinedChecker.checkUndefinedVariables(resolutionIndex, templateIndex);

        bridged = true;
    }

    /**
     * CRITICAL: Resolve context variable type by searching Flask render_template calls.
     */
    private TypeKind resolveContextVariableType(String contextVarName, Program program) {
        if (program == null) return TypeKind.UNKNOWN;

        List<RenderTemplateCall> calls = FlaskContextExtractor.extract(program);
        for (RenderTemplateCall call : calls) {
            if (call.getContextVariableNames().contains(contextVarName)) {
                TypeKind resolvedType = findContextVariableSourceType(program, contextVarName);
                if (resolvedType != TypeKind.UNKNOWN) {
                    return resolvedType;
                }
            }
        }
        return TypeKind.UNKNOWN;
    }

    /**
     * CRITICAL: Find the actual source type of a context variable.
     * Walks Flask AST looking for render_template calls with this context variable.
     */
    private TypeKind findContextVariableSourceType(ASTNode node, String contextVarName) {
        if (node == null) return TypeKind.UNKNOWN;

        // Check if this is a render_template call
        if (node instanceof AST.flask.expr.CallExpr call && isRenderTemplateCall(call)) {
            List<AST.flask.expr.Argument> args = call.getArguments();
            for (AST.flask.expr.Argument arg : args) {
                if (arg instanceof AST.flask.expr.KeywordArgument kwarg) {
                    if (contextVarName.equals(kwarg.getName())) {
                        AST.flask.expr.Expression valueExpr = kwarg.getValue();

                        if (valueExpr instanceof AST.flask.expr.IdentifierExpr idExpr) {
                            String sourceVarName = idExpr.getName();

                            // CRITICAL: Search in ALL scopes (global + local nested)
                            TypeKind resolvedType = searchSymbolInAllScopes(sourceVarName);
                            return resolvedType;
                        }
                    }
                }
            }
        }

        // Recursively search children
        for (AST.ASTNode child : node.getChildren()) {
            TypeKind result = findContextVariableSourceType(child, contextVarName);
            if (result != TypeKind.UNKNOWN) {
                return result;
            }
        }

        return TypeKind.UNKNOWN;
    }

    /**
     * CRITICAL NEW METHOD: Search for symbol in ALL scopes (including nested local scopes).
     * This uses FlaskSymbolTable.findDeepest() to find variables in any nested scope.
     */
    private TypeKind searchSymbolInAllScopes(String varName) {
        if (repository == null || repository.getFlaskGlobal() == null) {
            return TypeKind.UNKNOWN;
        }

        if (repository.getFlaskGlobal() instanceof FlaskSymbolTable flaskRoot) {
            Optional<Symbol> deepest = flaskRoot.findDeepest(varName);
            if (deepest.isPresent()) {
                TypeKind type = deepest.get().getInferredType();
                return type;
            }
        }

        return TypeKind.UNKNOWN;
    }

    private boolean isRenderTemplateCall(AST.flask.expr.CallExpr call) {
        return FlaskContextExtractor.isRenderTemplateCall(call);
    }

    /**
     * Convenience overload.
     */
    public void bridge(Program program, TemplateNode templateRoot) {
        bridge(program, templateRoot, null, null);
    }

    private void bridgeTemplateReferences(String templateFileName, TemplateReferenceIndex templateIndex, Set<String> renderContextKeys, List<RenderTemplateCall> callsForFile) {

        for (SymbolReference reference : templateIndex.getReferences()) {
            if (reference.getUseKind() != SymbolUseKind.REFERENCE) {
                continue;
            }

            CrossContextMatch match = resolveReference(templateFileName, reference, renderContextKeys, callsForFile, templateIndex);
            resolutionIndex.recordMatch(match);
            registerFromMatch(match);
        }
    }

    /**
     * Modified resolveReference: accept TemplateReferenceIndex and detect names that are defined
     * somewhere in the template (possibly in an inner scope). If a name is defined in the template
     * but the reference here couldn't resolve, emit E203 (Out of scope) and classify as UNRESOLVED
     * so that the correct scope diagnostic is produced (instead of E004).
     */
    private CrossContextMatch resolveReference(String templateFileName, SymbolReference reference, Set<String> renderContextKeys, List<RenderTemplateCall> callsForFile, TemplateReferenceIndex templateIndex) {

        String name = reference.getName();

        if (!reference.isUnresolved()) {
            return new CrossContextMatch(reference, CrossContextMatch.MatchKind.TEMPLATE_LOCAL, templateFileName, null, null);
        }

        if (renderContextKeys.contains(name)) {
            SourceRange callSite = findCallSiteProviding(callsForFile, name);
            return new CrossContextMatch(reference, CrossContextMatch.MatchKind.FLASK_RENDER_CONTEXT, templateFileName, name, callSite);
        }

        // If the template defines this name anywhere (e.g., a for-loop variable),
        // but the reference here couldn't resolve (likely a scope misuse — variable used outside its block),
        // emit E203 and classify as UNRESOLVED (scope error).
        /*if (templateIndex != null) {
            boolean definedInTemplate = templateIndex.getDefinitions().stream().anyMatch(def -> name.equals(def.getName()));
            if (definedInTemplate) {
                // Emit scope error diagnostic E203 (Out of scope)
                SourceRange src = reference.getLocation();
                String message = String.format("Variable '%s' referenced outside its defining scope", name);
                String suggestion = "Move usage inside the block where it is defined (e.g., inside the for-loop) or define it in an outer scope.";
                diagnosticCollector.reportScopeError(src, name, ErrorCode.E203_OUT_OF_SCOPE, message, suggestion);

                return new CrossContextMatch(reference, CrossContextMatch.MatchKind.UNRESOLVED, templateFileName, null, null);
            }
        }*/

        if (!callsForFile.isEmpty()) {
            if (existsInFlask(name)) {
                return new CrossContextMatch(reference, CrossContextMatch.MatchKind.MISSING_FROM_RENDER_CONTEXT, templateFileName, null, null);
            }
            // not exist in flask -> template undefined variable
            return new CrossContextMatch(reference, CrossContextMatch.MatchKind.UNRESOLVED, templateFileName, null, null);
        }
        return new CrossContextMatch(reference, CrossContextMatch.MatchKind.UNRESOLVED, templateFileName, null, null);
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
                contextMap.put(name, new TemplateContext(templateSymbol, null, TemplateContext.SymbolOrigin.LOCAL, TypeKind.UNKNOWN, ref.getDefiningScopeName()));
            }
            case FLASK_RENDER_CONTEXT -> {
                Symbol templateSymbol = new Symbol(name, SymbolKind.VARIABLE, currentTemplateFileName());
                Optional<Symbol> flaskSymbol = lookupFlaskSymbol(name);

                TypeKind type = TypeKind.UNKNOWN;
                if (flaskSymbol.isPresent()) {
                    type = flaskSymbol.get().getInferredType();
                } else if (contextVariableTypeCache.containsKey(name)) {
                    type = contextVariableTypeCache.get(name);
                }

                contextMap.put(name, new TemplateContext(templateSymbol, flaskSymbol.orElse(null), TemplateContext.SymbolOrigin.FLASK_CONTEXT, type, match.getFlaskContextKey()));
            }
            case MISSING_FROM_RENDER_CONTEXT -> {
                Symbol templateSymbol = new Symbol(name, SymbolKind.VARIABLE, currentTemplateFileName());
                contextMap.put(name, new TemplateContext(templateSymbol, null, TemplateContext.SymbolOrigin.UNKNOWN, TypeKind.UNKNOWN, "not passed to render_template for " + match.getTemplateFileName()));
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

    private static List<RenderTemplateCall> filterCallsForTemplate(List<RenderTemplateCall> allCalls, String templateFileName) {
        List<RenderTemplateCall> filtered = new ArrayList<>();
        for (RenderTemplateCall call : allCalls) {
            if (templateFileName.equalsIgnoreCase(call.getTemplateName())) {
                filtered.add(call);
            }
        }
        return filtered;
    }

    private String currentTemplateFileName() {
        return repository.getTemplateGlobal().getTemplateName() != null ? repository.getTemplateGlobal().getTemplateName() : "";
    }

    /**
     * CRITICAL: Get Flask symbol type.
     */
    public TypeKind getFlaskSymbolType(String name) {
        // First check if we have a cached type
        if (contextVariableTypeCache.containsKey(name)) {
            TypeKind cached = contextVariableTypeCache.get(name);
            if (cached != null && cached != TypeKind.UNKNOWN) {
                return cached;
            }
        }

        // Fall back to Flask Symbol Table lookup
        Optional<Symbol> symbol = lookupFlaskSymbol(name);
        if (symbol.isPresent()) {
            TypeKind inferred = symbol.get().getInferredType();
            return inferred;
        }

        return TypeKind.UNKNOWN;
    }

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
        if (repository.getFlaskGlobal() == null) {
            return Optional.empty();
        }
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

    public void generateUndefinedSymbolDiagnostic(String templateSymbolName, AST.SourceRange sourceRange, List<String> suggestions) {
        String suggestion = null;
        if (suggestions != null && !suggestions.isEmpty()) {
            suggestion = "Did you mean " + suggestions.get(0) + "?";
        }
        diagnosticCollector.reportUndefinedVariable(sourceRange, templateSymbolName, suggestion);
    }

    public void generateShadowingDiagnostic(String templateSymbolName, String flaskSymbolName, AST.SourceRange sourceRange) {
        diagnosticCollector.reportShadowing(sourceRange, templateSymbolName, flaskSymbolName, "Flask context");
    }

    public void generateTypeMismatchDiagnostic(String templateSymbolName, TypeKind expectedType, TypeKind usageType, AST.SourceRange sourceRange) {
        diagnosticCollector.reportTypeMismatch(sourceRange, templateSymbolName, expectedType, usageType, "Ensure the template usage matches the Flask variable type.");
    }

    public void generateTypeErrorDiagnostic(String operation, TypeKind leftType, TypeKind rightType, AST.SourceRange sourceRange, String suggestion) {
        diagnosticCollector.reportTypeError(sourceRange, operation + " between " + leftType.getDisplayName() + " and " + rightType.getDisplayName(), suggestion);
    }

    public void clear() {
        contextMap.clear();
        contextVariableTypeCache.clear();
        flaskContextVariables.clear();
        resolutionIndex.clear();
        bridged = false;
    }

    @Override
    public String toString() {
        return String.format("TemplateContextBridge { contexts: %d, bridged: %b, flaskVars: %d, cachedTypes: %d }", contextMap.size(), bridged, flaskContextVariables.size(), contextVariableTypeCache.size());
    }

    private boolean existsInFlask(String name) {
        if (repository.getFlaskGlobal() == null) return false;
        return NameResolver.resolve(repository.getFlaskGlobal(), name).isPresent();
    }
}