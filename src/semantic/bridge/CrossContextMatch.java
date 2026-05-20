package semantic.bridge;

import AST.SourceRange;
import SymbolTable.SymbolReference;

/**
 * Result of resolving one template variable reference against Flask render context.
 */
public final class CrossContextMatch {

    public enum MatchKind {
        /** Resolved via template-local scope (for/if/block). */
        TEMPLATE_LOCAL,
        /** Name passed to render_template(...) for this template file. */
        FLASK_RENDER_CONTEXT,
        /** Still unknown after bridge (not in template scope or Flask context). */
        UNRESOLVED,
        /** Used in template but not passed from any render_template call for this file. */
        MISSING_FROM_RENDER_CONTEXT
    }

    private final SymbolReference templateReference;
    private final MatchKind matchKind;
    private final String templateFileName;
    private final String flaskContextKey;
    private final SourceRange flaskCallSite;

    public CrossContextMatch(
            SymbolReference templateReference,
            MatchKind matchKind,
            String templateFileName,
            String flaskContextKey,
            SourceRange flaskCallSite) {
        this.templateReference = templateReference;
        this.matchKind = matchKind;
        this.templateFileName = templateFileName;
        this.flaskContextKey = flaskContextKey;
        this.flaskCallSite = flaskCallSite;
    }

    public SymbolReference getTemplateReference() {
        return templateReference;
    }

    public MatchKind getMatchKind() {
        return matchKind;
    }

    public String getTemplateFileName() {
        return templateFileName;
    }

    public String getFlaskContextKey() {
        return flaskContextKey;
    }

    public SourceRange getFlaskCallSite() {
        return flaskCallSite;
    }

    @Override
    public String toString() {
        return templateReference.getName()
                + " [" + matchKind + "]"
                + " (template " + templateFileName
                + (flaskContextKey != null ? ", flask key " + flaskContextKey : "")
                + ")";
    }
}
