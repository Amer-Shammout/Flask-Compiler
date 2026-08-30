package semantic.bridge;

import AST.ASTNode;
import AST.flask.Program;
import AST.flask.expr.Argument;
import AST.flask.expr.AttributeExpr;
import AST.flask.expr.CallExpr;
import AST.flask.expr.Expression;
import AST.flask.expr.IdentifierExpr;
import AST.flask.expr.IndexExpr;
import AST.flask.expr.KeywordArgument;
import AST.flask.expr.PositionalArgument;
import AST.flask.literal.BooleanLiteralExpr;
import AST.flask.literal.ListLiteralExpr;
import AST.flask.literal.NoneLiteralExpr;
import AST.flask.literal.NumberLiteralExpr;
import AST.flask.literal.StringLiteralExpr;
import semantic.diagnostics.TypeKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Walks a Flask {@link Program} AST and collects {@code render_template(...)} call sites.
 * <p>
 * CRITICAL FUNCTION: Also infers types for all context variables:
 * - render_template("add_product.html", test="sarah") → test has type STR
 * - render_template("products.html", products=view_products) → products type will be resolved later
 * <p>
 * For variables, we return UNKNOWN here (not in repository yet).
 * TemplateContextBridge will resolve variable types from Flask Symbol Table.
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

    private static void collectRenderTemplateCalls(ASTNode node, List<RenderTemplateCall> calls) {
        if (node == null) return;
        if (node instanceof CallExpr call && isRenderTemplateCall(call)) {
            calls.add(toRenderTemplateCall(call));
        }
        for (ASTNode child : node.getChildren()) {
            collectRenderTemplateCalls(child, calls);
        }
    }

    public static boolean isRenderTemplateCall(CallExpr call) {
        return resolveCalleeName(call.getFunction()).map(RENDER_TEMPLATE::equals).orElse(false);
    }

    public static Optional<String> resolveCalleeName(Expression function) {
        if (function instanceof IdentifierExpr identifier) {
            return Optional.of(identifier.getName());
        }
        if (function instanceof AttributeExpr attribute && RENDER_TEMPLATE.equals(attribute.getAttribute())) {
            return Optional.of(attribute.getAttribute());
        }
        return Optional.empty();
    }

    /**
     * CRITICAL: Extract render_template call AND infer types for context variables.
     */
    public static RenderTemplateCall toRenderTemplateCall(CallExpr call) {
        List<Argument> arguments = call.getArguments();
        String templateName = "";
        Set<String> contextNames = new LinkedHashSet<>();
        Map<String, TypeKind> contextTypes = new HashMap<>();
        if (!arguments.isEmpty()) {
            Argument first = arguments.get(0);
            int startIndex = 0;
            if (first instanceof PositionalArgument positional) {
                templateName = extractTemplateName(positional.getValue()).orElse("");
                startIndex = 1;
            }
            for (int i = startIndex; i < arguments.size(); i++) {
                Argument arg = arguments.get(i);
                if (arg instanceof KeywordArgument keyword) {
                    String varName = keyword.getName();
                    contextNames.add(varName);
                    TypeKind inferredType = inferExpressionType(keyword.getValue());
                    contextTypes.put(varName, inferredType);
                } else if (arg instanceof PositionalArgument positional) {
                    Optional<String> varName = extractContextVariableName(positional.getValue());
                    if (varName.isPresent()) {
                        contextNames.add(varName.get());
                        TypeKind inferredType = inferExpressionType(positional.getValue());
                        contextTypes.put(varName.get(), inferredType);
                    }
                }
            }
        }
        return new RenderTemplateCall(templateName, contextNames, contextTypes, call.getSourceRange());
    }

    public static Optional<String> extractTemplateName(Expression expression) {
        if (expression instanceof StringLiteralExpr literal) {
            return Optional.of(RenderTemplateCall.normalizeTemplateFileName(literal.getValue()));
        }
        return Optional.empty();
    }

    public static Optional<String> extractContextVariableName(Expression expression) {
        if (expression instanceof IdentifierExpr identifier) {
            return Optional.of(identifier.getName());
        }
        if (expression instanceof IndexExpr index && index.getBase() instanceof IdentifierExpr base) {
            return Optional.of(base.getName());
        }
        return Optional.empty();
    }

    /**
     * CRITICAL: Infer expression type for render_template context values.
     * <p>
     * For literals like "sarah", returns STR immediately.
     * For variables like nu, returns UNKNOWN (to be resolved from Flask Symbol Table later).
     */
    private static TypeKind inferExpressionType(Expression expr) {
        if (expr == null) return TypeKind.UNKNOWN;

        if (expr instanceof StringLiteralExpr) {
            return TypeKind.STR;
        }

        if (expr instanceof NumberLiteralExpr num) {
            String value = num.getValue();
            if (value != null && value.contains(".")) {
                return TypeKind.FLOAT;
            }
            return TypeKind.INT;
        }

        if (expr instanceof BooleanLiteralExpr) {
            return TypeKind.BOOL;
        }

        if (expr instanceof NoneLiteralExpr) {
            return TypeKind.NONE;
        }

        if (expr instanceof ListLiteralExpr) {
            return TypeKind.LIST;
        }

        // CRITICAL: For variables and expressions, return UNKNOWN
        // TemplateContextBridge will resolve from Flask Symbol Table
        if (expr instanceof IdentifierExpr) {
            return TypeKind.UNKNOWN;
        }

        return TypeKind.UNKNOWN;
    }
}