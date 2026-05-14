package semantic.bridge;

import SymbolTable.*;
import AST.Program;
import AST.template.TemplateNode;
import semantic.diagnostics.*;
import java.util.*;

/**
 * TemplateContextBridge links Flask context with Template context.
 *
 * This class acts as a bridge between the Flask semantic analysis (from FlaskSymbolTable)
 * and Template semantic analysis (from TemplateSymbolTable), enabling:
 *
 * 1. Symbol Resolution: Resolve template variables by looking them up in Flask scope.
 * 2. Type Propagation: Infer template variable types from Flask definitions.
 * 3. Cross-Context Diagnostics: Generate diagnostics that reference both Flask and Template.
 * 4. Context Linking: Track which template variables come from which Flask variables.
 *
 * Architecture:
 * - Holds references to both SymbolTableRepository and DiagnosticCollector.
 * - Provides methods to resolve template symbols via Flask context.
 * - Maintains a map of template symbols to their Flask origins (TemplateContext).
 *
 * Usage:
 *   TemplateContextBridge bridge = new TemplateContextBridge(repository, collector);
 *   // During template analysis:
 *   TemplateContext ctx = bridge.resolveTemplateSymbol(templateSymbol);
 *   if (ctx.isLinkedToFlask()) {
 *       / Use Flask symbol for type/value
 *   } else if (ctx.getOrigin() == SymbolOrigin.UNKNOWN) {
 *       / Generate undefined symbol diagnostic
 *   }
 *
 * TODO(Member 5): Implement full cross-linking and type propagation logic.
 * TODO(Member 5): Add support for Flask context expressions (e.g., app.config, request.args).
 */
public class TemplateContextBridge {

    // === Fields ===

    /** Repository holding both Flask and Template symbol tables. */
    private final SymbolTableRepository repository;

    /** Collector for diagnostics generated during bridging. */
    private final DiagnosticCollector diagnosticCollector;

    /**
     * Map from template symbol names to their resolved context (linking Flask + Template).
     * Key: symbol name in template.
     * Value: TemplateContext describing its resolution.
     */
    private final Map<String, TemplateContext> contextMap;

    /** Set of known Flask context variable names (passed from Flask app). */
    private final Set<String> flaskContextVariables;

    /** Flag indicating whether bridging has been performed. */
    private boolean bridged;


    // === Constructors ===

    /**
     * Construct a TemplateContextBridge.
     *
     * @param repository SymbolTableRepository holding both tables.
     * @param diagnosticCollector DiagnosticCollector for reporting cross-context issues.
     */
    public TemplateContextBridge(SymbolTableRepository repository,
                                  DiagnosticCollector diagnosticCollector) {
        this.repository = repository;
        this.diagnosticCollector = diagnosticCollector;
        this.contextMap = new LinkedHashMap<>();
        this.flaskContextVariables = new HashSet<>();
        this.bridged = false;
    }


    // === Core Bridging Methods ===

    /**
     * Perform the bridging process: link all template symbols to Flask scope where possible.
     *
     * This method:
     * 1. Iterates over template symbol table.
     * 2. For each template symbol, attempts to find a corresponding symbol in Flask table.
     * 3. Creates a TemplateContext for each with appropriate origin and linking.
     * 4. Generates diagnostics for unresolved or ambiguous symbols.
     *
     * TODO(Member 5): Implement full bridging algorithm:
     *   - Exact name matching (template var X matches Flask var X).
     *   - Scope matching (account for function/class scopes).
     *   - Type inference (infer template var type from Flask var).
     *   - Recursive context resolution (parent templates, imports).
     *
     * @param program Flask Program AST (optional, for reference).
     * @param templateRoot Template AST root (optional, for reference).
     */
    public void bridge(Program program, TemplateNode templateRoot) {
        // TODO(Member 5): Implement bridging logic.
        // 1. Extract template symbols from TemplateSymbolTable (via repository).
        // 2. For each, resolve via Flask symbol table using SymbolTableRepository.resolveAcross(...).
        // 3. Create TemplateContext with appropriate origin.
        // 4. Store in contextMap.
        // 5. Generate diagnostics for unresolved or suspicious symbols.

        this.bridged = true;
    }


    // === Symbol Resolution Methods ===

    /**
     * Resolve a template symbol to a TemplateContext.
     *
     * First checks contextMap (if bridged). If not found or not bridged, attempts on-demand resolution.
     *
     * TODO(Member 5): Implement on-demand resolution logic.
     *
     * @param templateSymbolName Name of the symbol in template.
     * @return Optional TemplateContext; empty if symbol not found.
     */
    public Optional<TemplateContext> resolveTemplateSymbol(String templateSymbolName) {
        // TODO(Member 5): Implement resolution.
        // 1. Check contextMap first.
        // 2. If not present and not yet bridged, perform lazy resolution.
        // 3. Return Optional.

        return contextMap.containsKey(templateSymbolName)
            ? Optional.of(contextMap.get(templateSymbolName))
            : Optional.empty();
    }

    /**
     * Resolve a template symbol by Symbol object (not just name).
     *
     * TODO(Member 5): Implement Symbol-based resolution.
     *
     * @param templateSymbol Symbol from TemplateSymbolTable.
     * @return Optional TemplateContext.
     */
    public Optional<TemplateContext> resolveTemplateSymbol(Symbol templateSymbol) {
        return resolveTemplateSymbol(templateSymbol.getName());
    }

    /**
     * Look up a Flask symbol that corresponds to a template symbol.
     *
     * TODO(Member 5): Implement Flask lookup with scope/type matching.
     *
     * @param templateSymbolName Name from template.
     * @return Optional Symbol from Flask scope.
     */
    public Optional<Symbol> lookupFlaskSymbol(String templateSymbolName) {
        // TODO(Member 5): Query SymbolTableRepository.resolveAcross(...) or similar.
        return Optional.empty(); // Placeholder
    }


    // === Context Mapping Methods ===

    /**
     * Manually register a TemplateContext (for testing or explicit linking).
     *
     * @param templateSymbolName Name of template symbol.
     * @param context TemplateContext to register.
     */
    public void registerContext(String templateSymbolName, TemplateContext context) {
        contextMap.put(templateSymbolName, context);
    }

    /**
     * Get all registered contexts.
     *
     * @return Unmodifiable map of all TemplateContexts.
     */
    public Map<String, TemplateContext> getAllContexts() {
        return Collections.unmodifiableMap(contextMap);
    }

    /**
     * Get contexts filtered by origin.
     *
     * @param origin SymbolOrigin to filter by.
     * @return List of TemplateContexts with the given origin.
     */
    public List<TemplateContext> getContextsByOrigin(TemplateContext.SymbolOrigin origin) {
        List<TemplateContext> result = new ArrayList<>();
        for (TemplateContext ctx : contextMap.values()) {
            if (ctx.getOrigin() == origin) {
                result.add(ctx);
            }
        }
        return result;
    }

    /**
     * Get contexts that are linked to Flask.
     *
     * @return List of TemplateContexts with Flask symbols.
     */
    public List<TemplateContext> getFlaskLinkedContexts() {
        List<TemplateContext> result = new ArrayList<>();
        for (TemplateContext ctx : contextMap.values()) {
            if (ctx.isLinkedToFlask()) {
                result.add(ctx);
            }
        }
        return result;
    }


    // === Flask Context Variable Registration ===

    /**
     * Register Flask context variable names (e.g., from Flask app.context_processor).
     *
     * This allows the bridge to recognize which template variables come from Flask context.
     *
     * @param variableNames Collection of variable names available in Flask context.
     */
    public void registerFlaskContextVariables(Collection<String> variableNames) {
        flaskContextVariables.addAll(variableNames);
    }

    /**
     * Check if a name is a known Flask context variable.
     *
     * @param name Variable name to check.
     * @return true if name is in registered Flask context.
     */
    public boolean isFlaskContextVariable(String name) {
        return flaskContextVariables.contains(name);
    }


    // === Query Methods ===

    /**
     * Check if bridging has been completed.
     *
     * @return true if bridge() has been called.
     */
    public boolean isBridged() {
        return bridged;
    }

    /**
     * Get the number of registered contexts.
     *
     * @return Context count.
     */
    public int getContextCount() {
        return contextMap.size();
    }

    /**
     * Get diagnostics collected during bridging.
     *
     * @return DiagnosticCollector used for bridging.
     */
    public DiagnosticCollector getDiagnosticCollector() {
        return diagnosticCollector;
    }


    // === Diagnostic Generation (Member 5 Helpers) ===

    /**
     * Generate a diagnostic for an undefined template variable.
     *
     * Uses DiagnosticCollector helper: reportUndefinedVariable(...).
     *
     * @param templateSymbolName Name of undefined symbol.
     * @param sourceRange Location in template.
     * @param suggestions Optional list of similar Flask symbols (for "did you mean?").
     */
    public void generateUndefinedSymbolDiagnostic(String templateSymbolName,
                                                   AST.SourceRange sourceRange,
                                                   List<String> suggestions) {
        String suggestion = null;
        if (suggestions != null && !suggestions.isEmpty()) {
            suggestion = "Did you mean " + suggestions.get(0) + "?";
        }
        diagnosticCollector.reportUndefinedVariable(sourceRange, templateSymbolName, suggestion);
    }

    /**
     * Generate a diagnostic for shadowing (template var shadows Flask var).
     *
     * Uses DiagnosticCollector helper: reportShadowing(...).
     *
     * @param templateSymbolName Name of shadowing symbol.
     * @param flaskSymbolName Name of shadowed Flask symbol.
     * @param sourceRange Location in template.
     */
    public void generateShadowingDiagnostic(String templateSymbolName,
                                             String flaskSymbolName,
                                             AST.SourceRange sourceRange) {
        diagnosticCollector.reportShadowing(
            sourceRange,
            templateSymbolName,
            flaskSymbolName,
            "Flask context"
        );
    }

    /**
     * Generate a diagnostic for type mismatch between template usage and Flask definition.
     *
     * Uses DiagnosticCollector helper: reportTypeMismatch(...).
     *
     * @param templateSymbolName Name of symbol.
     * @param expectedType Type from Flask (TypeKind).
     * @param usageType Type inferred from template usage (TypeKind).
     * @param sourceRange Location in template.
     */
    public void generateTypeMismatchDiagnostic(String templateSymbolName,
                                                TypeKind expectedType,
                                                TypeKind usageType,
                                                AST.SourceRange sourceRange) {
        diagnosticCollector.reportTypeMismatch(
            sourceRange,
            templateSymbolName,
            expectedType,
            usageType,
            "Ensure the template usage matches the Flask variable type."
        );
    }


    // === Utility Methods ===

    /**
     * Clear all registered contexts and reset state (for reuse or cleanup).
     */
    public void clear() {
        contextMap.clear();
        flaskContextVariables.clear();
        bridged = false;
    }

    /**
     * Get a string summary of bridge state.
     *
     * @return Summary string.
     */
    @Override
    public String toString() {
        return String.format("TemplateContextBridge { contexts: %d, bridged: %b, flaskVars: %d }",
                contextMap.size(), bridged, flaskContextVariables.size());
    }
}
