package generator.template;

import AST.template.jinja.expr.JinjaAttrExpr;
import AST.template.jinja.expr.JinjaBinaryExpr;
import AST.template.jinja.expr.JinjaExpr;
import AST.template.jinja.expr.JinjaFilterExpr;
import AST.template.jinja.expr.JinjaIdentifierExpr;
import AST.template.jinja.expr.JinjaNumberLiteralExpr;
import AST.template.jinja.expr.JinjaStringLiteralExpr;
import AST.template.jinja.expr.JinjaUnaryExpr;
import generator.context.ContextData;
import generator.context.RuntimeValue;
import AST.template.jinja.expr.JinjaCallExpr;

import java.util.List;

/**
 * Evaluates Jinja expressions against a {@link ContextData}.
 * Supports identifiers, attributes, literals, {@code not}, and basic binary ops.
 */
public final class JinjaExpressionEvaluator {

    private JinjaExpressionEvaluator() {
    }

    public static RuntimeValue evaluate(JinjaExpr expr, ContextData context) {
        switch (expr) {
            case null -> {
                return RuntimeValue.none();
            }
            case JinjaIdentifierExpr id -> {
                String name = id.getName();
                if ("true".equalsIgnoreCase(name)) return RuntimeValue.ofBool(true);
                if ("false".equalsIgnoreCase(name)) return RuntimeValue.ofBool(false);
                if ("none".equalsIgnoreCase(name)) return RuntimeValue.none();
                return context.get(name);
            }
            case JinjaAttrExpr attr -> {
                RuntimeValue target = evaluate(attr.getTarget(), context);
                return target.getAttr(attr.getAttribute());
            }
            case JinjaCallExpr call -> {
                if (call.getCallee() instanceof JinjaIdentifierExpr id) {
                    List<RuntimeValue> args = call.getArgs().stream().map(arg -> evaluate(arg, context)).toList();
                    return switch (id.getName()) {
                        case "len" -> {
                            if (args.isEmpty()) {
                                throw new IllegalArgumentException("len() requires 1 argument");
                            }
                            yield RuntimeValue.ofInt(lengthOf(args.get(0)));
                        }
                        case "int" -> {
                            if (args.isEmpty()) {
                                throw new IllegalArgumentException("int() requires 1 argument");
                            }
                            yield RuntimeValue.ofInt(args.get(0).asInt());
                        }
                        case "float" -> {
                            if (args.isEmpty()) {
                                throw new IllegalArgumentException("float() requires 1 argument");
                            }
                            yield RuntimeValue.ofFloat(args.get(0).asFloat());
                        }
                        case "str" -> {
                            if (args.isEmpty()) {
                                throw new IllegalArgumentException("str() requires 1 argument");
                            }
                            yield RuntimeValue.ofString(args.get(0).toString());
                        }
                        default -> throw new UnsupportedOperationException("Unsupported call: " + id.getName());
                    };
                }

                throw new UnsupportedOperationException("Unsupported call target: " + call.getCallee().getClass().getSimpleName());
            }
            case JinjaStringLiteralExpr str -> {
                return RuntimeValue.ofString(stripQuotes(str.getRawText()));
            }
            case JinjaNumberLiteralExpr num -> {
                return parseNumber(num.getText());
            }
            case JinjaUnaryExpr unary -> {
                RuntimeValue inner = evaluate(unary.getExpr(), context);
                if ("not".equalsIgnoreCase(unary.getOp())) {
                    return RuntimeValue.ofBool(!inner.isTruthy());
                }
                throw new UnsupportedOperationException("Unsupported unary op: " + unary.getOp());
            }
            case JinjaBinaryExpr binary -> {
                return evaluateBinary(binary, context);
            }
            case JinjaFilterExpr filter -> {
                return applyFilter(filter, context);
            }
            default -> {
            }
        }

        throw new UnsupportedOperationException(
                "JinjaExpressionEvaluator does not support: " + expr.getClass().getSimpleName()
        );
    }

    /** Supports the common Jinja filters likely to show up in course templates. */
    private static RuntimeValue applyFilter(JinjaFilterExpr filter, ContextData context) {
        RuntimeValue base = evaluate(filter.getBase(), context);
        List<RuntimeValue> args = new java.util.ArrayList<>();
        for (JinjaExpr arg : filter.getArgs()) {
            args.add(evaluate(arg, context));
        }

        String name = filter.getFilterName();
        return switch (name) {
            case "upper" -> RuntimeValue.ofString(base.toString().toUpperCase());
            case "lower" -> RuntimeValue.ofString(base.toString().toLowerCase());
            case "capitalize" -> RuntimeValue.ofString(capitalize(base.toString()));
            case "title" -> RuntimeValue.ofString(titleCase(base.toString()));
            case "trim" -> RuntimeValue.ofString(base.toString().trim());
            case "length", "count" -> RuntimeValue.ofInt(lengthOf(base));
            case "round" -> roundFilter(base, args);
            case "int" -> RuntimeValue.ofInt(base.asInt());
            case "float" -> RuntimeValue.ofFloat(base.asFloat());
            case "abs" -> absFilter(base);
            case "default" -> defaultFilter(base, args);
            default -> throw new UnsupportedOperationException("Unsupported filter: " + name);
        };
    }

    private static RuntimeValue defaultFilter(RuntimeValue base, List<RuntimeValue> args) {
        if (args.isEmpty()) {
            return base;
        }
        // Real Jinja's default() only replaces Undefined unless a truthy 2nd arg is given
        // to also replace other falsy values (0, "", empty list, etc.).
        boolean replaceOnAnyFalsy = args.size() > 1 && args.get(1).isTruthy();
        boolean shouldReplace = base.isNone() || (replaceOnAnyFalsy && !base.isTruthy());
        return shouldReplace ? args.get(0) : base;
    }

    private static RuntimeValue roundFilter(RuntimeValue base, List<RuntimeValue> args) {
        double value = base.asFloat();
        int precision = args.isEmpty() ? 0 : (int) args.get(0).asInt();
        double factor = Math.pow(10, precision);
        return RuntimeValue.ofFloat(Math.round(value * factor) / factor);
    }

    private static RuntimeValue absFilter(RuntimeValue base) {
        if (base.getKind() == RuntimeValue.Kind.FLOAT) {
            return RuntimeValue.ofFloat(Math.abs(base.asFloat()));
        }
        return RuntimeValue.ofInt(Math.abs(base.asInt()));
    }

    private static long lengthOf(RuntimeValue value) {
        return switch (value.getKind()) {
            case LIST -> value.asList().size();
            case STRING -> value.asString().length();
            case OBJECT -> value.asObject().size();
            default -> throw new UnsupportedOperationException(
                    "length/count filter not supported for kind " + value.getKind());
        };
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private static String titleCase(String s) {
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(capitalize(words[i]));
        }
        return sb.toString();
    }

    /**
     * Resolves a dotted path like {@code product.name} against the context
     * (used for Jinja embedded inside HTML attribute strings).
     */
    public static RuntimeValue evaluatePath(String path, ContextData context) {
        if (path == null || path.isBlank()) {
            return RuntimeValue.none();
        }
        String[] parts = path.trim().split("\\.");
        RuntimeValue current = context.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            current = current.getAttr(parts[i]);
        }
        return current;
    }

    private static RuntimeValue evaluateBinary(JinjaBinaryExpr binary, ContextData context) {
        String op = binary.getOp() == null ? "" : binary.getOp().trim();
        RuntimeValue left = evaluate(binary.getLeft(), context);
        RuntimeValue right = evaluate(binary.getRight(), context);

        return switch (op) {
            case "and" -> RuntimeValue.ofBool(left.isTruthy() && right.isTruthy());
            case "or" -> RuntimeValue.ofBool(left.isTruthy() || right.isTruthy());
            case "==" -> RuntimeValue.ofBool(valuesEqual(left, right));
            case "!=" -> RuntimeValue.ofBool(!valuesEqual(left, right));
            case "<" -> RuntimeValue.ofBool(compare(left, right) < 0);
            case ">" -> RuntimeValue.ofBool(compare(left, right) > 0);
            case "<=" -> RuntimeValue.ofBool(compare(left, right) <= 0);
            case ">=" -> RuntimeValue.ofBool(compare(left, right) >= 0);
            case "+" -> add(left, right);
            case "-" -> subtract(left, right);
            case "*" -> multiply(left, right);
            case "/" -> divide(left, right);
            case "%" -> modulo(left, right);
            case "~" -> RuntimeValue.ofString(left.toString() + right.toString());
            default -> throw new UnsupportedOperationException("Unsupported binary op: " + op);
        };
    }

    /** Ordering compare for {@code < > <= >=}: lexicographic for strings, numeric otherwise. */
    private static int compare(RuntimeValue left, RuntimeValue right) {
        if (left.getKind() == RuntimeValue.Kind.STRING && right.getKind() == RuntimeValue.Kind.STRING) {
            return left.asString().compareTo(right.asString());
        }
        return Double.compare(left.asFloat(), right.asFloat());
    }

    private static RuntimeValue subtract(RuntimeValue left, RuntimeValue right) {
        if (left.getKind() == RuntimeValue.Kind.FLOAT || right.getKind() == RuntimeValue.Kind.FLOAT) {
            return RuntimeValue.ofFloat(left.asFloat() - right.asFloat());
        }
        return RuntimeValue.ofInt(left.asInt() - right.asInt());
    }

    private static RuntimeValue multiply(RuntimeValue left, RuntimeValue right) {
        if (left.getKind() == RuntimeValue.Kind.FLOAT || right.getKind() == RuntimeValue.Kind.FLOAT) {
            return RuntimeValue.ofFloat(left.asFloat() * right.asFloat());
        }
        return RuntimeValue.ofInt(left.asInt() * right.asInt());
    }

    /** Python 3 semantics: {@code /} is always true (float) division. */
    private static RuntimeValue divide(RuntimeValue left, RuntimeValue right) {
        return RuntimeValue.ofFloat(left.asFloat() / right.asFloat());
    }

    private static RuntimeValue modulo(RuntimeValue left, RuntimeValue right) {
        if (left.getKind() == RuntimeValue.Kind.FLOAT || right.getKind() == RuntimeValue.Kind.FLOAT) {
            return RuntimeValue.ofFloat(left.asFloat() % right.asFloat());
        }
        return RuntimeValue.ofInt(left.asInt() % right.asInt());
    }

    private static boolean valuesEqual(RuntimeValue left, RuntimeValue right) {
        if (left.getKind() == RuntimeValue.Kind.NONE && right.getKind() == RuntimeValue.Kind.NONE) {
            return true;
        }
        return left.equals(right) || left.toString().equals(right.toString());
    }

    private static RuntimeValue add(RuntimeValue left, RuntimeValue right) {
        if (left.getKind() == RuntimeValue.Kind.STRING || right.getKind() == RuntimeValue.Kind.STRING) {
            return RuntimeValue.ofString(left.toString() + right.toString());
        }
        if (left.getKind() == RuntimeValue.Kind.FLOAT || right.getKind() == RuntimeValue.Kind.FLOAT) {
            return RuntimeValue.ofFloat(left.asFloat() + right.asFloat());
        }
        return RuntimeValue.ofInt(left.asInt() + right.asInt());
    }

    private static RuntimeValue parseNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return RuntimeValue.none();
        }
        String text = raw.trim();
        if (text.contains(".") || text.contains("e") || text.contains("E")) {
            return RuntimeValue.ofFloat(Double.parseDouble(text));
        }
        return RuntimeValue.ofInt(Long.parseLong(text));
    }

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
