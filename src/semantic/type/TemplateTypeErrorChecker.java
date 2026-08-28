package semantic.type;

import AST.SourceRange;
import AST.template.TemplateNode;
import AST.template.jinja.expr.*;

import SymbolTable.NameResolver;
import SymbolTable.ScopeBinding;
import SymbolTable.Symbol;
import SymbolTable.SymbolKind;
import SymbolTable.SymbolTableRepository;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.ErrorCode;
import semantic.diagnostics.TypeKind;

import java.util.List;
import java.util.Optional;

/**
 * Type checker for Jinja template expressions (template-LOCAL ONLY).
 * <p>
 * CRITICAL: This checker only processes TEMPLATE-LOCAL variables.
 * For variables passed via render_template(), use BridgeTypeChecker instead.
 * <p>
 * This prevents duplicate error reporting.
 */
public class TemplateTypeErrorChecker {

    private final SymbolTableRepository repository;
    private final DiagnosticCollector diagnostics;

    public TemplateTypeErrorChecker(SymbolTableRepository repository, DiagnosticCollector diagnostics) {
        this.repository = repository;
        this.diagnostics = diagnostics;
    }

    /**
     * Entry: walk template AST and check Jinja expressions (TEMPLATE-LOCAL ONLY).
     */
    public void checkTemplate(TemplateNode root) {
        if (root == null) return;
        walkNode(root);
    }

    private void walkNode(Object node) {
        if (node == null) return;

        if (node instanceof JinjaExpr expr) {
            checkJinjaExpr(expr);
            for (var child : expr.getChildren()) {
                walkNode(child);
            }
            return;
        }

        if (node instanceof List<?> list) {
            for (Object child : list) {
                walkNode(child);
            }
            return;
        }

        // Reflection fallback for other AST nodes
        try {
            var getChildren = node.getClass().getMethod("getChildren");
            Object children = getChildren.invoke(node);
            if (children instanceof List<?> childList) {
                for (Object child : childList) {
                    walkNode(child);
                }
            }
        } catch (Exception ignored) {
            // No children method or inaccessible
        }
    }

    /**
     * Infer type for TEMPLATE-LOCAL variables only.
     * Return UNKNOWN for anything that might come from Flask.
     */
    private TypeKind inferType(JinjaExpr expr) {
        if (expr == null) return TypeKind.UNKNOWN;

        return switch (expr) {
            case JinjaStringLiteralExpr ignored -> TypeKind.STR;
            case JinjaNumberLiteralExpr n -> inferNumberType(n);
            case JinjaIdentifierExpr id -> inferIdentifierType(id);
            case JinjaBinaryExpr bin -> inferBinaryType(bin);
            case JinjaUnaryExpr un -> inferUnaryType(un);
            case JinjaCallExpr call -> inferCallType(call);
            default -> TypeKind.UNKNOWN;
        };
    }

    private TypeKind inferNumberType(JinjaNumberLiteralExpr n) {
        String text = n.getText();
        if (text != null && text.contains(".")) {
            return TypeKind.FLOAT;
        }
        return TypeKind.INT;
    }

    private TypeKind inferIdentifierType(JinjaIdentifierExpr id) {
        String name = id.getName();
        if (repository.getTemplateGlobal() != null) {
            Optional<ScopeBinding> binding = NameResolver.resolve(repository.getTemplateGlobal(), name);
            if (binding.isPresent() && binding.get().getSymbol() != null) {
                return binding.get().getSymbol().getInferredType();
            }
        }
        // Not found locally -> might be from Flask, so UNKNOWN
        return TypeKind.UNKNOWN;
    }

    private TypeKind inferBinaryType(JinjaBinaryExpr bin) {
        TypeKind left = inferType(bin.getLeft());
        TypeKind right = inferType(bin.getRight());
        String op = bin.getOp();

        if (op.equals("+")) {
            if (left == TypeKind.STR && right == TypeKind.STR) return TypeKind.STR;
            if (left == TypeKind.LIST && right == TypeKind.LIST) return TypeKind.LIST;
            if (isNumeric(left) && isNumeric(right)) {
                return (left == TypeKind.FLOAT || right == TypeKind.FLOAT) ? TypeKind.FLOAT : TypeKind.INT;
            }
        }

        if (isArithmeticOperator(op)) {
            if (isNumeric(left) && isNumeric(right)) {
                return (left == TypeKind.FLOAT || right == TypeKind.FLOAT || op.equals("/"))
                        ? TypeKind.FLOAT : TypeKind.INT;
            }
        }

        if (isComparisonOperator(op)) {
            return TypeKind.BOOL;
        }

        return TypeKind.UNKNOWN;
    }

    private TypeKind inferUnaryType(JinjaUnaryExpr un) {
        if ("not".equals(un.getOp())) return TypeKind.BOOL;

        TypeKind type = inferType(un.getExpr());
        if (type == TypeKind.INT || type == TypeKind.FLOAT) {
            return type;
        }
        return TypeKind.UNKNOWN;
    }

    private TypeKind inferCallType(JinjaCallExpr call) {
        JinjaExpr callee = call.getCallee();
        if (callee instanceof JinjaIdentifierExpr id) {
            return switch (id.getName()) {
                case "len", "int", "sum" -> TypeKind.INT;
                case "str" -> TypeKind.STR;
                case "float" -> TypeKind.FLOAT;
                default -> TypeKind.UNKNOWN;
            };
        }
        return TypeKind.UNKNOWN;
    }

    /**
     * Check Jinja expression for TEMPLATE-LOCAL type errors only.
     * CRITICAL: Skip checking if both operands are UNKNOWN (might come from Flask).
     */
    private void checkJinjaExpr(JinjaExpr expr) {
        if (expr == null) return;

        if (expr instanceof JinjaBinaryExpr bin) {
            checkBinaryExpression(bin);
        } else if (expr instanceof JinjaCallExpr call) {
            checkCallExpression(call);
        }
    }

    private void checkBinaryExpression(JinjaBinaryExpr bin) {
        TypeKind left = inferType(bin.getLeft());
        TypeKind right = inferType(bin.getRight());
        String op = bin.getOp();
        SourceRange range = bin.getSourceRange();

        if (left == TypeKind.UNKNOWN && right == TypeKind.UNKNOWN) {
            return; // leave for BridgeTypeChecker
        }

        if (op.equals("+")) {
            checkAddition(left, right, range);
        } else if (op.equals("*")) {
            checkMultiplication(left, right, range);
        } else if (op.equals("%")) {
            checkModulo(left, right, range);
        } else if (isSubtractDivideOperator(op)) {
            checkSubtractDivide(left, right, op, range);
        } else if (isOrderComparison(op)) {
            checkOrderComparison(left, right, op, range);
        }
    }

    private void checkAddition(TypeKind left, TypeKind right, SourceRange range) {
        boolean strAndInt = (left == TypeKind.STR && right == TypeKind.INT)
                || (left == TypeKind.INT && right == TypeKind.STR);
        if (strAndInt) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                    "TypeError: can only concatenate str (not \"int\") to str",
                    "Use str() to convert int to string"));
            return;
        }

        boolean listAndInt = (left == TypeKind.LIST && right == TypeKind.INT)
                || (left == TypeKind.INT && right == TypeKind.LIST);
        if (listAndInt) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E105_INVALID_LIST_OPERATION,
                    "TypeError: can only concatenate list (not \"int\") to list",
                    "Use list concatenation with another list"));
        }
    }

    private void checkMultiplication(TypeKind left, TypeKind right, SourceRange range) {

        if (isNumeric(left) && isNumeric(right)) {
            return;
        }
        if (left == TypeKind.UNKNOWN || right == TypeKind.UNKNOWN) {
            return;
        }

        diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                "TypeError: unsupported operand type(s) for *: '" +
                        left.getDisplayName() + "' and '" + right.getDisplayName() + "'",
                "This operation only works with numeric types (int, float)"));
    }

    private void checkModulo(TypeKind left, TypeKind right, SourceRange range) {
        if (isNumeric(left) && isNumeric(right)) {
            return;
        }
        if (left == TypeKind.UNKNOWN || right == TypeKind.UNKNOWN) {
            return;
        }

        diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                "TypeError: unsupported operand type(s) for %: '" + left.getDisplayName() + "' and '" + right.getDisplayName() + "'",
                "This operation only works with numeric types (int, float)"));
    }

    private void checkSubtractDivide(TypeKind left, TypeKind right, String op, SourceRange range) {
        boolean invalid = (left == TypeKind.STR && right == TypeKind.STR)
                || (left == TypeKind.STR && (right == TypeKind.INT || right == TypeKind.FLOAT))
                || (right == TypeKind.STR && (left == TypeKind.INT || left == TypeKind.FLOAT));
        if (invalid) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                    "TypeError: unsupported operand type(s) for " + op + ": '" + left.getDisplayName() + "' and '" + right.getDisplayName() + "'",
                    "This operation only works with numeric types (int, float)"));
        }
    }

    private void checkOrderComparison(TypeKind left, TypeKind right, String op, SourceRange range) {
        boolean bothNumeric = isNumeric(left) && isNumeric(right);
        boolean bothStrings = left == TypeKind.STR && right == TypeKind.STR;

        if (bothNumeric || bothStrings) {
            return;
        }

        if (left == TypeKind.UNKNOWN || right == TypeKind.UNKNOWN) {
            return;
        }

        diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E103_INCOMPATIBLE_TYPES,
                "TypeError: '" + op + "' not supported between instances of '" +
                        left.getDisplayName() + "' and '" + right.getDisplayName() + "'",
                "Ensure both operands have compatible types"));
    }

    private void checkCallExpression(JinjaCallExpr call) {
        SourceRange range = call.getSourceRange();
        JinjaExpr callee = call.getCallee();
        TypeKind calleeType = inferType(callee);

        if (calleeType != TypeKind.UNKNOWN && isNonCallableType(calleeType)) {
            String varName = extractName(callee);
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E104_NOT_CALLABLE,
                    "TypeError: '" + calleeType.getDisplayName() + "' object is not callable",
                    varName != null ? "Remove () after " + varName : "Remove trailing ()"));
            return;
        }

        if (callee instanceof JinjaIdentifierExpr id) {
            checkBuiltinFunctionArgs(id.getName(), call);
        }
    }

    private boolean isNonCallableType(TypeKind type) {
        return type == TypeKind.INT || type == TypeKind.STR || type == TypeKind.FLOAT
                || type == TypeKind.LIST || type == TypeKind.DICT || type == TypeKind.BOOL;
    }

    private void checkBuiltinFunctionArgs(String funcName, JinjaCallExpr call) {
        SourceRange range = call.getSourceRange();
        if (range == null) return;

        List<JinjaExpr> args = call.getArgs();
        if (args.isEmpty()) return;

        if ("len".equals(funcName)) {
            checkLenArgument(args.get(0), range);
        } else if ("sum".equals(funcName)) {
            checkSumArgument(args.get(0), range);
        }
    }

    private void checkLenArgument(JinjaExpr arg, SourceRange range) {
        TypeKind type = inferType(arg);
        if (type != TypeKind.UNKNOWN && type != TypeKind.STR && type != TypeKind.LIST
                && type != TypeKind.DICT && type != TypeKind.TUPLE && type != TypeKind.SET) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE,
                    "TypeError: object of type '" + type.getDisplayName() + "' has no len()",
                    "len() expects a sequence or collection type (str, list, dict, tuple, set)"));
        }
    }

    private void checkSumArgument(JinjaExpr arg, SourceRange range) {
        TypeKind type = inferType(arg);
        if (type != TypeKind.UNKNOWN && type != TypeKind.LIST) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE,
                    "TypeError: '" + type.getDisplayName() + "' object is not iterable",
                    "sum() expects a list of numeric values"));
        }
    }

    private boolean isNumeric(TypeKind type) {
        return type == TypeKind.INT || type == TypeKind.FLOAT;
    }

    private boolean isArithmeticOperator(String op) {
        return op.equals("-") || op.equals("*") || op.equals("/") || op.equals("//") || op.equals("%");
    }

    private boolean isSubtractDivideOperator(String op) {
        return op.equals("-") || op.equals("/") || op.equals("//");
    }

    private boolean isComparisonOperator(String op) {
        return op.equals("==") || op.equals("!=") || op.equals("<") || op.equals(">")
                || op.equals("<=") || op.equals(">=");
    }

    private boolean isOrderComparison(String op) {
        return op.equals(">") || op.equals("<") || op.equals(">=") || op.equals("<=");
    }

    private String extractName(JinjaExpr expr) {
        if (expr instanceof JinjaIdentifierExpr id) return id.getName();
        if (expr instanceof JinjaNumberLiteralExpr num) return num.getText();
        if (expr instanceof JinjaStringLiteralExpr) return "\"string\"";
        return null;
    }
}