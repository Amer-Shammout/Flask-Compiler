package semantic.bridge;

import AST.ASTNode;
import AST.Program;
import AST.flask.expr.Argument;
import AST.flask.expr.AttributeExpr;
import AST.flask.expr.CallExpr;
import AST.flask.expr.Expression;
import AST.flask.expr.IdentifierExpr;
import AST.flask.expr.IndexExpr;
import AST.flask.expr.KeywordArgument;
import AST.flask.expr.PositionalArgument;
import AST.flask.literal.StringLiteralExpr;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Walks a Flask {@link Program} AST and collects {@code render_template(...)} call sites.
 */
public final class FlaskContextExtractor {

    private static final String RENDER_TEMPLATE = "render_template";

    private FlaskContextExtractor() {
    }

    public static List<RenderTemplateCall> extract(Program program) {
        List<RenderTemplateCall> calls = new ArrayList<>();
        if (program != null) {
            collectRenderTemplateCalls(program, calls);
        }
        return calls;
    }

    /**
     * Depth-first search over the entire AST for {@code render_template} call expressions.
     * This avoids missing calls when statement/suite {@link ASTNode#getChildren()} links are incomplete.
     */
    private static void collectRenderTemplateCalls(ASTNode node, List<RenderTemplateCall> calls) {
        if (node == null) {
            return;
        }

        if (node instanceof CallExpr call && isRenderTemplateCall(call)) {
            calls.add(toRenderTemplateCall(call));
        }

        for (ASTNode child : node.getChildren()) {
            collectRenderTemplateCalls(child, calls);
        }
    }

    public static boolean isRenderTemplateCall(CallExpr call) {
        return resolveCalleeName(call.getFunction())
                .map(RENDER_TEMPLATE::equals)
                .orElse(false);
    }

    /**
     * Resolves the invoked name from the callee expression.
     * Supports {@code render_template(...)} and {@code flask.render_template(...)}.
     */
    public static Optional<String> resolveCalleeName(Expression function) {
        if (function instanceof IdentifierExpr identifier) {
            return Optional.of(identifier.getName());
        }
        if (function instanceof AttributeExpr attribute
                && RENDER_TEMPLATE.equals(attribute.getAttribute())) {
            return Optional.of(attribute.getAttribute());
        }
        return Optional.empty();
    }

    public static RenderTemplateCall toRenderTemplateCall(CallExpr call) {
        List<Argument> arguments = call.getArguments();
        String templateName = "";
        Set<String> contextNames = new LinkedHashSet<>();

        if (!arguments.isEmpty()) {
            Argument first = arguments.get(0);
            int startIndex = 0;
            if (first instanceof PositionalArgument positional) {
                templateName = extractTemplateName(positional.getValue()).orElse("");
                startIndex = 1;
            }
            for (int i = startIndex; i < arguments.size(); i++) {
                collectContextName(arguments.get(i)).ifPresent(contextNames::add);
            }
        }

        return new RenderTemplateCall(templateName, contextNames, call.getSourceRange());
    }

    public static Optional<String> extractTemplateName(Expression expression) {
        if (expression instanceof StringLiteralExpr literal) {
            return Optional.of(RenderTemplateCall.normalizeTemplateFileName(literal.getValue()));
        }
        return Optional.empty();
    }

    /**
     * Derives the template context keyword from a call argument.
     * Supports {@code products} and {@code products[index]} (uses base name).
     */
    public static Optional<String> extractContextVariableName(Expression expression) {
        if (expression instanceof IdentifierExpr identifier) {
            return Optional.of(identifier.getName());
        }
        if (expression instanceof IndexExpr index
                && index.getBase() instanceof IdentifierExpr base) {
            return Optional.of(base.getName());
        }
        return Optional.empty();
    }

    private static Optional<String> collectContextName(Argument argument) {
        if (argument instanceof KeywordArgument keyword) {
            return Optional.of(keyword.getName());
        }
        if (argument instanceof PositionalArgument positional) {
            return extractContextVariableName(positional.getValue());
        }
        return Optional.empty();
    }
}
