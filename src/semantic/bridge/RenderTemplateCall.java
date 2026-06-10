package semantic.bridge;

import AST.SourceRange;
import semantic.diagnostics.TypeKind;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * One {@code render_template(...)} call site extracted from the Flask AST.
 * <p>
 * CRITICAL: Now tracks inferred types for all context variables.
 * For test="sarah", stores type STR.
 * For products=view_products, stores type that will be looked up from Flask.
 */
public final class RenderTemplateCall {

    private final String templateName;
    private final Set<String> contextVariableNames;
    private final Map<String, TypeKind> contextVariableTypes;
    private final SourceRange sourceRange;

    public RenderTemplateCall(String templateName, Set<String> contextVariableNames, SourceRange sourceRange) {
        this(templateName, contextVariableNames, new HashMap<>(), sourceRange);
    }

    public RenderTemplateCall(String templateName, Set<String> contextVariableNames, Map<String, TypeKind> contextVariableTypes, SourceRange sourceRange) {
        this.templateName = normalizeTemplateFileName(templateName);
        this.contextVariableNames = Collections.unmodifiableSet(new LinkedHashSet<>(contextVariableNames));
        this.contextVariableTypes = Collections.unmodifiableMap(new HashMap<>(contextVariableTypes));
        this.sourceRange = sourceRange;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Set<String> getContextVariableNames() {
        return contextVariableNames;
    }

    /**
     * Get the inferred type for a context variable.
     * For literal values, returns the direct type (STR, INT, etc).
     * For variables, returns UNKNOWN (to be resolved from Flask later).
     */
    public TypeKind getContextVariableType(String varName) {
        return contextVariableTypes.getOrDefault(varName, TypeKind.UNKNOWN);
    }

    public Map<String, TypeKind> getAllContextVariableTypes() {
        return contextVariableTypes;
    }

    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public static String normalizeTemplateFileName(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("render_template(\"").append(templateName).append("\", ");
        for (String var : contextVariableNames) {
            TypeKind type = contextVariableTypes.get(var);
            sb.append(var).append(":").append(type != null ? type : "?").append(" ");
        }
        sb.append(")");
        if (sourceRange != null) sb.append(" at ").append(sourceRange);
        return sb.toString();
    }
}