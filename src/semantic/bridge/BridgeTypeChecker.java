package semantic.bridge;

import AST.SourceRange;
import AST.template.TemplateNode;
import AST.template.jinja.expr.*;
import AST.template.jinja.stmt.JinjaForStmt;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.ErrorCode;
import semantic.diagnostics.TypeKind;
import SymbolTable.SymbolTableRepository;

import java.util.List;

/**
 * Cross-context type checker for Template expressions against Flask variable types.
 * <p>
 * CRITICAL: This checker:
 * 1. Walks template AST looking for binary expressions like test+1
 * 2. For each identifier (ex: test), calls bridge.getFlaskSymbolType() to get Flask type
 * 3. For literal values (1), infers type directly (INT)
 * 4. Checks if operation is valid (STR + INT = ERROR E102)
 * 5. Reports error with diagnostic
 * <p>
 * Example:
 * - Template: {{ test+1 }}
 * - Flask passed: test="sarah" (STR)
 * - Checker infers: left=STR (from Flask), right=INT (literal 1)
 * - Operation +: STR + INT = E102 TYPE ERROR
 */


public class BridgeTypeChecker {

    private final SymbolTableRepository repository;
    private final DiagnosticCollector diagnosticCollector;

    public BridgeTypeChecker(SymbolTableRepository repository, DiagnosticCollector diagnosticCollector) {
        this.repository = repository;
        this.diagnosticCollector = diagnosticCollector;
    }

    /**
     * Main entry point: Walk template AST and check all expressions.
     */
    public void checkCrossContextTypes(TemplateNode templateRoot, TemplateContextBridge bridge) {
        if (templateRoot == null) return;
        walkTemplateNode(templateRoot, bridge);
    }

    private void walkTemplateNode(Object node, TemplateContextBridge bridge) {
        if (node == null) return;

        if (node instanceof JinjaBinaryExpr binaryExpr) {
            checkBinaryExpression(binaryExpr, bridge);
            walkChildren(binaryExpr.getChildren(), bridge);
            return;
        }

        if (node instanceof JinjaCallExpr callExpr) {
            checkCallExpression(callExpr, bridge);
            walkChildren(callExpr.getChildren(), bridge);
            return;
        }

        if (node instanceof JinjaForStmt forStmt) {
            checkForStatement(forStmt, bridge);
            walkChildren(forStmt.getChildren(), bridge);
            return;
        }

        if (node instanceof JinjaExpr expr) {
            walkChildren(expr.getChildren(), bridge);
            return;
        }

        if (node instanceof List<?> list) {
            for (Object child : list) {
                walkTemplateNode(child, bridge);
            }
            return;
        }

        // Reflection fallback
        try {
            var getChildren = node.getClass().getMethod("getChildren");
            Object children = getChildren.invoke(node);
            if (children instanceof List<?> childList) {
                walkChildren(childList, bridge);
            }
        } catch (Exception ignored) {
            // Ignore nodes without getChildren
        }
    }

    private void walkChildren(List<?> children, TemplateContextBridge bridge) {
        if (children == null) return;
        for (Object child : children) {
            walkTemplateNode(child, bridge);
        }
    }

    private void checkBinaryExpression(JinjaBinaryExpr binaryExpr, TemplateContextBridge bridge) {
        TypeKind leftType = inferType(binaryExpr.getLeft(), bridge);
        TypeKind rightType = inferType(binaryExpr.getRight(), bridge);
        String op = binaryExpr.getOp();
        SourceRange range = binaryExpr.getSourceRange();

        if (range == null) return;

        if (op.equals("+")) {
            checkAddition(leftType, rightType, range);
        } else if (op.equals("*")) {
            checkMultiplication(leftType, rightType, range);
        } else if (op.equals("%")) {
            checkModulo(leftType, rightType, range);
        } else if (op.equals("-") || op.equals("/") || op.equals("//")) {
            checkSubtractDivide(leftType, rightType, op, range);
        } else if (op.equals(">") || op.equals("<") || op.equals(">=") ||
                op.equals("<=") || op.equals("==") || op.equals("!=")) {
            checkComparison(leftType, rightType, op, range);
        }
    }


    private void checkAddition(TypeKind leftType, TypeKind rightType, SourceRange range) {
        boolean strAndInt = (leftType == TypeKind.STR && rightType == TypeKind.INT)
                || (leftType == TypeKind.INT && rightType == TypeKind.STR);
        if (strAndInt) {
            diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                    "TypeError: can only concatenate str (not \"int\") to str",
                    "Use str() to convert int to string"));
            return;
        }

        boolean listAndInt = (leftType == TypeKind.LIST && rightType == TypeKind.INT)
                || (leftType == TypeKind.INT && rightType == TypeKind.LIST);
        if (listAndInt) {
            diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E105_INVALID_LIST_OPERATION,
                    "TypeError: can only concatenate list (not \"int\") to list",
                    "Use list concatenation with another list"));
        }
    }

    private void checkMultiplication(TypeKind leftType, TypeKind rightType, SourceRange range) {
        boolean bothNumeric = isNumeric(leftType) && isNumeric(rightType);
        if (bothNumeric) {
            return;
        }
        if (leftType == TypeKind.UNKNOWN || rightType == TypeKind.UNKNOWN) {
            return;
        }

        diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                "TypeError: unsupported operand type(s) for *: '"
                        + leftType.getDisplayName() + "' and '" + rightType.getDisplayName() + "'",
                "This operation only works with numeric types (int, float)"));
    }

    private void checkSubtractDivide(TypeKind leftType, TypeKind rightType, String op, SourceRange range) {
        boolean invalidStrOperation =
                (leftType == TypeKind.STR && rightType == TypeKind.STR)
                        || (leftType == TypeKind.STR && (rightType == TypeKind.INT || rightType == TypeKind.FLOAT))
                        || (rightType == TypeKind.STR && (leftType == TypeKind.INT || leftType == TypeKind.FLOAT));

        if (invalidStrOperation) {
            diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                    "TypeError: unsupported operand type(s) for " + op + ": '"
                            + leftType.getDisplayName() + "' and '" + rightType.getDisplayName() + "'",
                    "This operation only works with numeric types (int, float)"));
        }
    }

    private void checkComparison(TypeKind leftType, TypeKind rightType, String op, SourceRange range) {
        boolean bothNumeric = isNumeric(leftType) && isNumeric(rightType);
        boolean bothStrings = leftType == TypeKind.STR && rightType == TypeKind.STR;

        if (bothNumeric || bothStrings) {
            return;
        }

        if (leftType == TypeKind.UNKNOWN || rightType == TypeKind.UNKNOWN) {
            return;
        }

        diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E103_INCOMPATIBLE_TYPES,
                "TypeError: '" + op + "' not supported between instances of '"
                        + leftType.getDisplayName() + "' and '" + rightType.getDisplayName() + "'",
                "Ensure both operands have compatible types"));
    }

    private void checkModulo(TypeKind leftType, TypeKind rightType, SourceRange range) {
        boolean bothNumeric = isNumeric(leftType) && isNumeric(rightType);
        if (bothNumeric) {
            return;
        }
        if (leftType == TypeKind.UNKNOWN || rightType == TypeKind.UNKNOWN) {
            return;
        }

        diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                "TypeError: unsupported operand type(s) for %: '"
                        + leftType.getDisplayName() + "' and '" + rightType.getDisplayName() + "'",
                "This operation only works with numeric types (int, float)"));
    }

    private void checkCallExpression(JinjaCallExpr callExpr, TemplateContextBridge bridge) {
        SourceRange range = callExpr.getSourceRange();
        if (range == null) return;

        JinjaExpr callee = callExpr.getCallee();
        TypeKind calleeType = inferType(callee, bridge);

        if (isNonCallableType(calleeType)) {
            String varName = extractVariableName(callee);
            diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E104_NOT_CALLABLE,
                    "TypeError: '" + calleeType.getDisplayName() + "' object is not callable",
                    varName != null ? "Remove () after " + varName : "Remove trailing ()"));
            return;
        }

        if (callee instanceof JinjaIdentifierExpr id) {
            checkBuiltinFunctionUsage(id.getName(), callExpr, bridge);
        }
    }

    private boolean isNonCallableType(TypeKind type) {
        return type == TypeKind.INT || type == TypeKind.STR || type == TypeKind.FLOAT
                || type == TypeKind.LIST || type == TypeKind.DICT || type == TypeKind.BOOL;
    }

    private void checkForStatement(JinjaForStmt forStmt, TemplateContextBridge bridge) {
        JinjaExpr iterable = forStmt.getIterable();
        SourceRange range = forStmt.getSourceRange();

        if (iterable == null || range == null) return;

        TypeKind iterableType = inferType(iterable, bridge);

        if (!isIterableType(iterableType)) {
            String varName = extractVariableName(iterable);
            String iterableName = varName != null ? varName : iterableType.getDisplayName();
            diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E107_NOT_ITERABLE,
                    "TypeError: '" + iterableType.getDisplayName() + "' object is not iterable",
                    "for loop requires an iterable type (list, dict, tuple, set, or str)"));
        }
    }

    private boolean isIterableType(TypeKind type) {
        if (type == null) return false;

        if (type == TypeKind.LIST || type == TypeKind.DICT || type == TypeKind.TUPLE
                || type == TypeKind.SET || type == TypeKind.STR) {
            return true;
        }

        return type == TypeKind.UNKNOWN || type == TypeKind.ANY;
    }

    private void checkBuiltinFunctionUsage(String funcName, JinjaCallExpr callExpr,
                                           TemplateContextBridge bridge) {
        SourceRange range = callExpr.getSourceRange();
        if (range == null) return;

        List<JinjaExpr> args = callExpr.getArgs();
        if (args.isEmpty()) return;

        if ("len".equals(funcName)) {
            checkLenArgument(args.get(0), bridge, range);
        } else if ("sum".equals(funcName)) {
            checkSumArgument(args.get(0), bridge, range);
        }
    }

    private void checkLenArgument(JinjaExpr arg, TemplateContextBridge bridge, SourceRange range) {
        TypeKind argType = inferType(arg, bridge);
        boolean validLenType = argType == TypeKind.STR || argType == TypeKind.LIST
                || argType == TypeKind.DICT || argType == TypeKind.TUPLE
                || argType == TypeKind.SET || argType == TypeKind.UNKNOWN;

        if (!validLenType) {
            diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE,
                    "TypeError: object of type '" + argType.getDisplayName() + "' has no len()",
                    "len() expects a sequence or collection type"));
        }
    }

    private void checkSumArgument(JinjaExpr arg, TemplateContextBridge bridge, SourceRange range) {
        TypeKind argType = inferType(arg, bridge);
        if (argType != TypeKind.LIST && argType != TypeKind.UNKNOWN) {
            diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE,
                    "TypeError: '" + argType.getDisplayName() + "' object is not iterable",
                    "sum() expects a list of numeric values"));
        }
    }

    private TypeKind inferType(JinjaExpr expr, TemplateContextBridge bridge) {
        if (expr == null) return TypeKind.UNKNOWN;

        if (expr instanceof JinjaStringLiteralExpr) {
            return TypeKind.STR;
        }

        if (expr instanceof JinjaNumberLiteralExpr number) {
            String text = number.getText();
            if (text != null && text.contains(".")) return TypeKind.FLOAT;
            return TypeKind.INT;
        }

        if (expr instanceof JinjaIdentifierExpr id) {
            return bridge.getFlaskSymbolType(id.getName());
        }

        if (expr instanceof JinjaBinaryExpr binary) {
            return inferBinaryType(binary, bridge);
        }

        if (expr instanceof JinjaUnaryExpr unary) {
            return inferUnaryType(unary, bridge);
        }

        if (expr instanceof JinjaCallExpr call) {
            return inferCallType(call);
        }

        return TypeKind.UNKNOWN;
    }

    private TypeKind inferBinaryType(JinjaBinaryExpr binary, TemplateContextBridge bridge) {
        TypeKind left = inferType(binary.getLeft(), bridge);
        TypeKind right = inferType(binary.getRight(), bridge);
        String op = binary.getOp();

        if (op.equals("+")) {
            if (left == TypeKind.STR && right == TypeKind.STR) return TypeKind.STR;
            if (left == TypeKind.LIST && right == TypeKind.LIST) return TypeKind.LIST;
            if (isNumeric(left) && isNumeric(right)) {
                return (left == TypeKind.FLOAT || right == TypeKind.FLOAT)
                        ? TypeKind.FLOAT : TypeKind.INT;
            }
        }

        if (op.equals("-") || op.equals("*") || op.equals("/") || op.equals("//") || op.equals("%")) {
            if (isNumeric(left) && isNumeric(right)) {
                return (left == TypeKind.FLOAT || right == TypeKind.FLOAT || op.equals("/"))
                        ? TypeKind.FLOAT : TypeKind.INT;
            }
        }

        if (op.equals("==") || op.equals("!=") || op.equals("<") || op.equals(">")
                || op.equals("<=") || op.equals(">=")) {
            return TypeKind.BOOL;
        }

        return TypeKind.UNKNOWN;
    }

    private TypeKind inferUnaryType(JinjaUnaryExpr unary, TemplateContextBridge bridge) {
        if ("not".equals(unary.getOp())) return TypeKind.BOOL;

        TypeKind type = inferType(unary.getExpr(), bridge);
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
                case "bool" -> TypeKind.BOOL;
                case "list" -> TypeKind.LIST;
                case "dict" -> TypeKind.DICT;
                default -> TypeKind.UNKNOWN;
            };
        }
        return TypeKind.UNKNOWN;
    }

    private boolean isNumeric(TypeKind type) {
        return type == TypeKind.INT || type == TypeKind.FLOAT;
    }

    private String extractVariableName(JinjaExpr expr) {
        if (expr instanceof JinjaIdentifierExpr id) return id.getName();
        if (expr instanceof JinjaNumberLiteralExpr num) return num.getText();
        if (expr instanceof JinjaStringLiteralExpr str) return "\"" + str.getRawText() + "\"";
        return null;
    }
}