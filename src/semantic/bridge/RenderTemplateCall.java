package semantic.bridge;

import AST.SourceRange;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One {@code render_template(...)} site extracted from the Flask AST.
 */
public final class RenderTemplateCall {

    private final String templateName;
    private final Set<String> contextVariableNames;
    private final SourceRange sourceRange;

    public RenderTemplateCall(String templateName, Set<String> contextVariableNames, SourceRange sourceRange) {
        this.templateName = normalizeTemplateFileName(templateName);
        this.contextVariableNames = Collections.unmodifiableSet(new LinkedHashSet<>(contextVariableNames));
        this.sourceRange = sourceRange;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Set<String> getContextVariableNames() {
        return contextVariableNames;
    }

    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public static String normalizeTemplateFileName(String raw) {
        if (raw == null) {
            return "";
        }
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
        return "render_template(\"" + templateName + "\", " + contextVariableNames + ")"
                + (sourceRange != null ? " at " + sourceRange : "");
    }
}
