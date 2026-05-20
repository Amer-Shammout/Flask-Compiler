package SymbolTable;

import AST.SourceRange;

/**
 * Records a template dependency introduced by {@code extends} or {@code include}.
 */
public final class TemplateLink {

    private final String linkKind;
    private final String templateName;
    private final SourceRange location;

    public TemplateLink(String linkKind, String templateName, SourceRange location) {
        this.linkKind = linkKind;
        this.templateName = templateName;
        this.location = location;
    }

    public String getLinkKind() {
        return linkKind;
    }

    public String getTemplateName() {
        return templateName;
    }

    public SourceRange getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return linkKind + " -> \"" + templateName + "\""
                + (location != null ? " at " + location : "");
    }
}
