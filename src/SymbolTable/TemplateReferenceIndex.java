package SymbolTable;

import AST.SourceRange;
import semantic.diagnostics.ResolutionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collects template/Jinja definition and reference sites plus template dependency links.
 */
public class TemplateReferenceIndex {

    private final List<SymbolReference> allSites = new ArrayList<>();
    private final List<SymbolReference> unresolvedReferences = new ArrayList<>();
    private final List<TemplateLink> templateLinks = new ArrayList<>();

    public void record(SymbolReference reference) {
        allSites.add(reference);
        if (reference.getUseKind() == SymbolUseKind.REFERENCE && reference.isUnresolved()) {
            unresolvedReferences.add(reference);
        }
    }

    public void recordTemplateLink(String linkKind, String templateName, SourceRange location) {
        if (templateName == null || templateName.isBlank()) {
            return;
        }
        templateLinks.add(new TemplateLink(linkKind, templateName, location));
    }

    public List<SymbolReference> getAllSites() {
        return Collections.unmodifiableList(allSites);
    }

    public List<SymbolReference> getDefinitions() {
        return allSites.stream()
                .filter(ref -> ref.getUseKind() == SymbolUseKind.DEFINITION)
                .collect(Collectors.toList());
    }

    public List<SymbolReference> getReferences() {
        return allSites.stream()
                .filter(ref -> ref.getUseKind() == SymbolUseKind.REFERENCE)
                .collect(Collectors.toList());
    }

    public List<SymbolReference> getUnresolvedReferences() {
        return Collections.unmodifiableList(unresolvedReferences);
    }

    public List<SymbolReference> getReferencesWithStatus(ResolutionStatus status) {
        return allSites.stream()
                .filter(ref -> ref.getUseKind() == SymbolUseKind.REFERENCE && ref.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<TemplateLink> getTemplateLinks() {
        return Collections.unmodifiableList(templateLinks);
    }

    public void clear() {
        allSites.clear();
        unresolvedReferences.clear();
        templateLinks.clear();
    }

    public String formatReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Template name resolution report ===\n");
        sb.append("Definitions: ").append(getDefinitions().size()).append('\n');
        sb.append("References:  ").append(getReferences().size()).append('\n');
        sb.append("Unresolved:  ").append(unresolvedReferences.size()).append('\n');
        sb.append("Template links: ").append(templateLinks.size()).append('\n');

        long shadowed = getReferencesWithStatus(ResolutionStatus.SHADOWED).size();
        if (shadowed > 0) {
            sb.append("Shadowed uses: ").append(shadowed).append('\n');
        }

        if (!templateLinks.isEmpty()) {
            sb.append("\n-- Template links (extends/include) --\n");
            for (TemplateLink link : templateLinks) {
                sb.append("  ").append(link).append('\n');
            }
        }

        sb.append("\n-- Definitions --\n");
        for (SymbolReference ref : getDefinitions()) {
            sb.append("  ").append(ref).append('\n');
        }

        sb.append("\n-- References --\n");
        for (SymbolReference ref : getReferences()) {
            sb.append("  ").append(ref).append('\n');
        }

        if (!unresolvedReferences.isEmpty()) {
            sb.append("\n-- Unresolved (expect Flask-context names here until bridge runs) --\n");
            for (SymbolReference ref : unresolvedReferences) {
                sb.append("  ").append(ref).append('\n');
            }
        }

        return sb.toString();
    }
}
