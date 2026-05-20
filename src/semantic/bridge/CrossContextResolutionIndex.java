package semantic.bridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collects cross-language binding results from {@link TemplateContextBridge}.
 */
public class CrossContextResolutionIndex {

    private final List<RenderTemplateCall> renderCalls = new ArrayList<>();
    private final List<CrossContextMatch> matches = new ArrayList<>();

    public void recordRenderCall(RenderTemplateCall call) {
        renderCalls.add(call);
    }

    public void recordMatch(CrossContextMatch match) {
        matches.add(match);
    }

    public List<RenderTemplateCall> getRenderCalls() {
        return Collections.unmodifiableList(renderCalls);
    }

    public List<CrossContextMatch> getMatches() {
        return Collections.unmodifiableList(matches);
    }

    public List<CrossContextMatch> getMatches(CrossContextMatch.MatchKind kind) {
        return matches.stream()
                .filter(match -> match.getMatchKind() == kind)
                .collect(Collectors.toList());
    }

    public void clear() {
        renderCalls.clear();
        matches.clear();
    }

    public String formatReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Flask <-> Template bridge report ===\n");
        sb.append("render_template calls: ").append(renderCalls.size()).append('\n');
        sb.append("Template references analyzed: ").append(matches.size()).append('\n');

        long linked = getMatches(CrossContextMatch.MatchKind.FLASK_RENDER_CONTEXT).size();
        long local = getMatches(CrossContextMatch.MatchKind.TEMPLATE_LOCAL).size();
        long missing = getMatches(CrossContextMatch.MatchKind.MISSING_FROM_RENDER_CONTEXT).size();
        long unresolved = getMatches(CrossContextMatch.MatchKind.UNRESOLVED).size();

        sb.append("Linked via render_template: ").append(linked).append('\n');
        sb.append("Template-local: ").append(local).append('\n');
        sb.append("Missing from render context: ").append(missing).append('\n');
        sb.append("Unresolved: ").append(unresolved).append('\n');

        if (!renderCalls.isEmpty()) {
            sb.append("\n-- render_template calls --\n");
            for (RenderTemplateCall call : renderCalls) {
                sb.append("  ").append(call).append('\n');
            }
        }

        if (linked > 0) {
            sb.append("\n-- Linked (Flask context) --\n");
            for (CrossContextMatch match : getMatches(CrossContextMatch.MatchKind.FLASK_RENDER_CONTEXT)) {
                sb.append("  ").append(match).append('\n');
            }
        }

        if (missing > 0) {
            sb.append("\n-- Missing from render context --\n");
            for (CrossContextMatch match : getMatches(CrossContextMatch.MatchKind.MISSING_FROM_RENDER_CONTEXT)) {
                sb.append("  ").append(match).append('\n');
            }
        }

        if (unresolved > 0) {
            sb.append("\n-- Still unresolved --\n");
            for (CrossContextMatch match : getMatches(CrossContextMatch.MatchKind.UNRESOLVED)) {
                sb.append("  ").append(match).append('\n');
            }
        }

        return sb.toString();
    }
}
