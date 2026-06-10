package semantic.bridge;

import AST.SourceRange;
import AST.template.TemplateNode;
import AST.template.jinja.expr.*;
import AST.template.jinja.stmt.JinjaForStmt;
import SymbolTable.SymbolTableRepository;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.ErrorCode;
import semantic.diagnostics.TypeKind;

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

    /**
     * Recursive walk of template AST.
     */
    private void walkTemplateNode(Object node, TemplateContextBridge bridge) {
        if (node == null) return;

        // Binary expressions: CRITICAL - this catches test+1
        if (node instanceof JinjaBinaryExpr binaryExpr) {
            checkBinaryExpression(binaryExpr, bridge);
            for (var child : binaryExpr.getChildren()) {
                walkTemplateNode(child, bridge);
            }
            return;
        }

        // Function calls
        if (node instanceof JinjaCallExpr callExpr) {
            checkCallExpression(callExpr, bridge);
            for (var child : callExpr.getChildren()) {
                walkTemplateNode(child, bridge);
            }
            return;
        }

        // For loops
        if (node instanceof JinjaForStmt forStmt) {
            checkForStatement(forStmt, bridge);
            for (var child : forStmt.getChildren()) {
                walkTemplateNode(child, bridge);
            }
            return;
        }

        // Generic Jinja expressions
        if (node instanceof JinjaExpr expr) {
            for (var child : expr.getChildren()) {
                walkTemplateNode(child, bridge);
            }
            return;
        }

        // Lists
        if (node instanceof java.util.List<?> list) {
            for (Object o : list) walkTemplateNode(o, bridge);
            return;
        }

        // Reflection fallback
        try {
            var m = node.getClass().getMethod("getChildren");
            Object children = m.invoke(node);
            if (children instanceof java.util.List<?> childList) {
                for (Object c : childList) walkTemplateNode(c, bridge);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * CRITICAL: Check binary expressions like test+1
     */
    private void checkBinaryExpression(JinjaBinaryExpr binaryExpr, TemplateContextBridge bridge) {
        TypeKind leftType = inferType(binaryExpr.getLeft(), bridge);
        TypeKind rightType = inferType(binaryExpr.getRight(), bridge);
        String op = binaryExpr.getOp();
        SourceRange range = binaryExpr.getSourceRange();

        if (range == null) return;

        // Log for debugging
        String leftExprStr = binaryExpr.getLeft() instanceof JinjaIdentifierExpr id ? id.getName() : "?";
        String rightExprStr = binaryExpr.getRight() instanceof JinjaNumberLiteralExpr num ? num.getText() : "?";

        // E102: Type error in addition
        if (op.equals("+")) {
            if ((leftType == TypeKind.STR && rightType == TypeKind.INT) || (leftType == TypeKind.INT && rightType == TypeKind.STR)) {
                diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR, "TypeError: can only concatenate str (not \"int\") to str", "Use str() to convert int to string"));
                return;
            }
            if ((leftType == TypeKind.LIST && rightType == TypeKind.INT) || (leftType == TypeKind.INT && rightType == TypeKind.LIST)) {
                diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E105_INVALID_LIST_OPERATION, "TypeError: can only concatenate list (not \"int\") to list", "Use list concatenation with another list"));
                return;
            }
        }

        if (op.equals("-") || op.equals("/") || op.equals("//")) {
            if ((leftType == TypeKind.STR && rightType == TypeKind.STR) || (leftType == TypeKind.STR && (rightType == TypeKind.INT || rightType == TypeKind.FLOAT)) || (rightType == TypeKind.STR && (leftType == TypeKind.INT || leftType == TypeKind.FLOAT))) {
                diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR, "TypeError: unsupported operand type(s) for " + op + ": '" + leftType.getDisplayName() + "' and '" + rightType.getDisplayName() + "'", "This operation only works with numeric types (int, float)"));
                return;
            }
        }

        // E103: Incompatible types in comparisons
        if (op.equals(">") || op.equals("<") || op.equals(">=") || op.equals("<=") || op.equals("==") || op.equals("!=")) {
            if ((leftType != TypeKind.INT && rightType != TypeKind.FLOAT) || (rightType != TypeKind.INT && leftType != TypeKind.FLOAT) || (leftType != rightType)) {
                diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E103_INCOMPATIBLE_TYPES, "TypeError: '" + op + "' not supported between instances of '" + leftType.getDisplayName() + "' and '" + rightType.getDisplayName() + "'", "Ensure both operands have compatible types"));
            }
        }
    }

    /**
     * Check function calls.
     */
    private void checkCallExpression(JinjaCallExpr callExpr, TemplateContextBridge bridge) {
        SourceRange range = callExpr.getSourceRange();
        if (range == null) return;

        JinjaExpr callee = callExpr.getCallee();
        TypeKind calleeType = inferType(callee, bridge);

        // E104: Not callable
        if (calleeType == TypeKind.INT || calleeType == TypeKind.STR || calleeType == TypeKind.FLOAT || calleeType == TypeKind.LIST || calleeType == TypeKind.DICT || calleeType == TypeKind.BOOL) {
            String varName = extractVariableName(callee);
            diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E104_NOT_CALLABLE, "TypeError: '" + calleeType.getDisplayName() + "' object is not callable", varName != null ? "Remove () after " + varName : "Remove trailing ()"));
            return;
        }

        // E106: Invalid builtin usage
        if (callee instanceof JinjaIdentifierExpr id) {
            checkBuiltinFunctionUsage(id.getName(), callExpr, bridge);
        }
    }

    /**
     * Check for statement: verify iterable is actually iterable.
     */
    private void checkForStatement(JinjaForStmt forStmt, TemplateContextBridge bridge) {
        JinjaExpr iterable = forStmt.getIterable();
        SourceRange range = forStmt.getSourceRange();

        if (iterable == null || range == null) return;

        TypeKind iterableType = inferType(iterable, bridge);

        // E107: Check if type is iterable
        if (!isIterableType(iterableType)) {
            String varName = extractVariableName(iterable);
            String iterableName = varName != null ? varName : iterableType.getDisplayName();
            diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E107_NOT_ITERABLE, "TypeError: '" + iterableType.getDisplayName() + "' object is not iterable", "for loop requires an iterable type (list, dict, tuple, set, or str)"));
        }
    }

    /**
     * Check if a type is iterable (list, dict, tuple, set, str, or unknown/any for recovery).
     */
    private boolean isIterableType(TypeKind type) {
        if (type == null) return false;

        // Iterable types
        if (type == TypeKind.LIST || type == TypeKind.DICT || type == TypeKind.TUPLE || type == TypeKind.SET || type == TypeKind.STR) {
            return true;
        }

        // Allow UNKNOWN and ANY for recovery (no error reported)
        if (type == TypeKind.UNKNOWN || type == TypeKind.ANY) {
            return true;
        }

        return false;
    }

    private void checkBuiltinFunctionUsage(String funcName, JinjaCallExpr callExpr, TemplateContextBridge bridge) {
        SourceRange range = callExpr.getSourceRange();
        if (range == null) return;

        java.util.List<JinjaExpr> args = callExpr.getArgs();

        if ("len".equals(funcName) && !args.isEmpty()) {
            TypeKind argType = inferType(args.get(0), bridge);
            if (argType != TypeKind.STR && argType != TypeKind.LIST && argType != TypeKind.DICT && argType != TypeKind.TUPLE && argType != TypeKind.SET && argType != TypeKind.UNKNOWN) {
                diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE, "TypeError: object of type '" + argType.getDisplayName() + "' has no len()", "len() expects a sequence or collection type"));
            }
        } else if ("sum".equals(funcName) && !args.isEmpty()) {
            TypeKind argType = inferType(args.get(0), bridge);
            if (argType != TypeKind.LIST && argType != TypeKind.UNKNOWN) {
                diagnosticCollector.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE, "TypeError: '" + argType.getDisplayName() + "' object is not iterable", "sum() expects a list of numeric values"));
            }
        }
    }

    /**
     * CRITICAL: Infer type of any Jinja expression.
     * <p>
     * For identifiers like "test", CALLS bridge.getFlaskSymbolType("test")
     * which returns STR (because test="sarah" was passed from Flask).
     */
    private TypeKind inferType(JinjaExpr expr, TemplateContextBridge bridge) {
        if (expr == null) return TypeKind.UNKNOWN;

        // String literals
        if (expr instanceof JinjaStringLiteralExpr) {
            return TypeKind.STR;
        }

        // Number literals
        if (expr instanceof JinjaNumberLiteralExpr n) {
            String text = n.getText();
            if (text != null && text.contains(".")) return TypeKind.FLOAT;
            return TypeKind.INT;
        }

        // CRITICAL: Identifiers - get type from Flask
        // This is where test="sarah" → STR translation happens
        if (expr instanceof JinjaIdentifierExpr id) {
            TypeKind flaskType = bridge.getFlaskSymbolType(id.getName());
            return flaskType;
        }

        // Binary expressions
        if (expr instanceof JinjaBinaryExpr bin) {
            TypeKind left = inferType(bin.getLeft(), bridge);
            TypeKind right = inferType(bin.getRight(), bridge);
            String op = bin.getOp();

            if (op.equals("+")) {
                if (left == TypeKind.STR && right == TypeKind.STR) return TypeKind.STR;
                if (left == TypeKind.LIST && right == TypeKind.LIST) return TypeKind.LIST;
                if ((left == TypeKind.INT || left == TypeKind.FLOAT) && (right == TypeKind.INT || right == TypeKind.FLOAT)) {
                    if (left == TypeKind.FLOAT || right == TypeKind.FLOAT) return TypeKind.FLOAT;
                    return TypeKind.INT;
                }
            }
            if (op.equals("-") || op.equals("*") || op.equals("/") || op.equals("//") || op.equals("%")) {
                if ((left == TypeKind.INT || left == TypeKind.FLOAT) && (right == TypeKind.INT || right == TypeKind.FLOAT)) {
                    if (left == TypeKind.FLOAT || right == TypeKind.FLOAT || op.equals("/")) return TypeKind.FLOAT;
                    return TypeKind.INT;
                }
            }
            if (op.equals("==") || op.equals("!=") || op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
                return TypeKind.BOOL;
            }
            return TypeKind.UNKNOWN;
        }

        // Unary expressions
        if (expr instanceof JinjaUnaryExpr u) {
            if ("not".equals(u.getOp())) return TypeKind.BOOL;
            TypeKind t = inferType(u.getExpr(), bridge);
            if (t == TypeKind.INT || t == TypeKind.FLOAT) return t;
            return TypeKind.UNKNOWN;
        }

        // Function calls
        if (expr instanceof JinjaCallExpr call) {
            JinjaExpr callee = call.getCallee();
            if (callee instanceof JinjaIdentifierExpr id) {
                String fname = id.getName();
                switch (fname) {
                    case "len":
                        return TypeKind.INT;
                    case "str":
                        return TypeKind.STR;
                    case "int":
                        return TypeKind.INT;
                    case "float":
                        return TypeKind.FLOAT;
                    case "bool":
                        return TypeKind.BOOL;
                    case "list":
                        return TypeKind.LIST;
                    case "dict":
                        return TypeKind.DICT;
                    case "sum":
                        return TypeKind.INT;
                    default:
                        return TypeKind.UNKNOWN;
                }
            }
            return TypeKind.UNKNOWN;
        }

        return TypeKind.UNKNOWN;
    }

    private String extractVariableName(JinjaExpr expr) {
        if (expr instanceof JinjaIdentifierExpr id) return id.getName();
        if (expr instanceof JinjaNumberLiteralExpr num) return num.getText();
        if (expr instanceof JinjaStringLiteralExpr str) return "\"" + str.getRawText() + "\"";
        return null;
    }
}