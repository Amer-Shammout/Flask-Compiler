package generator.context;

import AST.flask.expr.Expression;
import AST.flask.literal.BooleanLiteralExpr;
import AST.flask.literal.ListLiteralExpr;
import AST.flask.literal.NoneLiteralExpr;
import AST.flask.literal.NumberLiteralExpr;
import AST.flask.literal.StringLiteralExpr;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates simple Flask AST expressions into {@link RuntimeValue}s.
 * <p>
 * Step 2 scope: literals and nested lists only (enough for {@code products = [...]}).
 * Later steps will add identifiers, attributes, calls, etc.
 */
public final class ExpressionEvaluator {

    private ExpressionEvaluator() {
    }

    /** True when every node in the expression tree is a supported literal/list. */
    public static boolean isSupported(Expression expr) {
        if (expr == null) {
            return true;
        }
        if (expr instanceof NoneLiteralExpr
                || expr instanceof BooleanLiteralExpr
                || expr instanceof NumberLiteralExpr
                || expr instanceof StringLiteralExpr) {
            return true;
        }
        if (expr instanceof ListLiteralExpr list) {
            for (Expression element : list.getElements()) {
                if (!isSupported(element)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static RuntimeValue evaluate(Expression expr) {
        if (expr == null) {
            return RuntimeValue.none();
        }

        if (expr instanceof NoneLiteralExpr) {
            return RuntimeValue.none();
        }

        if (expr instanceof BooleanLiteralExpr bool) {
            return RuntimeValue.ofBool(bool.getValue());
        }

        if (expr instanceof NumberLiteralExpr number) {
            return evaluateNumber(number.getValue());
        }

        if (expr instanceof StringLiteralExpr string) {
            return RuntimeValue.ofString(stripQuotes(string.getValue()));
        }

        if (expr instanceof ListLiteralExpr list) {
            List<RuntimeValue> items = new ArrayList<>();
            for (Expression element : list.getElements()) {
                items.add(evaluate(element));
            }
            return RuntimeValue.ofList(items);
        }

        throw new UnsupportedOperationException(
                "Step 2 ExpressionEvaluator does not yet support: " + expr.getClass().getSimpleName()
        );
    }

    private static RuntimeValue evaluateNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return RuntimeValue.none();
        }
        String text = raw.trim();
        if (text.contains(".") || text.contains("e") || text.contains("E")) {
            return RuntimeValue.ofFloat(Double.parseDouble(text));
        }
        return RuntimeValue.ofInt(Long.parseLong(text));
    }

    /** ANTLR stores string tokens with quotes, e.g. {@code "Laptop"}. */
    static String stripQuotes(String raw) {
        if (raw == null || raw.length() < 2) {
            return raw == null ? "" : raw;
        }
        char first = raw.charAt(0);
        char last = raw.charAt(raw.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }
}
