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

        if (node instanceof java.util.List<?> list) {
            for (Object o : list) walkNode(o);
            return;
        }

        try {
            var m = node.getClass().getMethod("getChildren");
            Object children = m.invoke(node);
            if (children instanceof List<?>) {
                for (Object c : (List<?>) children) walkNode(c);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * CRITICAL: Infer type for TEMPLATE-LOCAL variables only.
     * Return UNKNOWN for anything that might come from Flask.
     */
    private TypeKind inferType(JinjaExpr expr) {
        if (expr == null) return TypeKind.UNKNOWN;

        if (expr instanceof JinjaStringLiteralExpr) {
            return TypeKind.STR;
        }

        if (expr instanceof JinjaNumberLiteralExpr n) {
            String text = n.getText();
            if (text != null && text.contains(".")) return TypeKind.FLOAT;
            return TypeKind.INT;
        }

        // CRITICAL: For identifiers, check ONLY template-local scope.
        // If it's not in template-local scope, return UNKNOWN.
        // This will be checked by BridgeTypeChecker with Flask context.
        if (expr instanceof JinjaIdentifierExpr id) {
            String name = id.getName();
            if (repository.getTemplateGlobal() != null) {
                Optional<ScopeBinding> tb = NameResolver.resolve(repository.getTemplateGlobal(), name);
                if (tb.isPresent() && tb.get().getSymbol() != null) {
                    return tb.get().getSymbol().getInferredType();
                }
            }
            // Not found in template-local scope. Could be from Flask.
            // Let BridgeTypeChecker handle it.
            return TypeKind.UNKNOWN;
        }

        if (expr instanceof JinjaBinaryExpr bin) {
            TypeKind left = inferType(bin.getLeft());
            TypeKind right = inferType(bin.getRight());
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

        if (expr instanceof JinjaUnaryExpr u) {
            if ("not".equals(u.getOp())) return TypeKind.BOOL;
            TypeKind t = inferType(u.getExpr());
            if (t == TypeKind.INT || t == TypeKind.FLOAT) return t;
            return TypeKind.UNKNOWN;
        }

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
                    case "sum":
                        return TypeKind.INT;
                }
            }
            return TypeKind.UNKNOWN;
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
            TypeKind left = inferType(bin.getLeft());
            TypeKind right = inferType(bin.getRight());
            String op = bin.getOp();
            SourceRange range = bin.getSourceRange();

            // CRITICAL: Skip if both are UNKNOWN (will be checked by BridgeTypeChecker)
            if (left == TypeKind.UNKNOWN && right == TypeKind.UNKNOWN) {
                return;
            }

            // E102 cases - ONLY FOR TEMPLATE-LOCAL TYPES
            if (op.equals("+")) {
                if ((left == TypeKind.STR && right == TypeKind.INT) || (left == TypeKind.INT && right == TypeKind.STR)) {
                    diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR, "TypeError: can only concatenate str (not \"int\") to str", "Use str() to convert int to string"));
                    return;
                }
                if ((left == TypeKind.LIST && right == TypeKind.INT) || (left == TypeKind.INT && right == TypeKind.LIST)) {
                    diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E105_INVALID_LIST_OPERATION, "TypeError: can only concatenate list (not \"int\") to list", "Use list concatenation with another list"));
                    return;
                }
            }

            if (op.equals("-") || op.equals("/") || op.equals("//")) {
                if ((left == TypeKind.STR && right == TypeKind.STR) || (left == TypeKind.STR && (right == TypeKind.INT || right == TypeKind.FLOAT)) || (right == TypeKind.STR && (left == TypeKind.INT || left == TypeKind.FLOAT))) {
                    diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR, "TypeError: unsupported operand type(s) for " + op + ": '" + left.getDisplayName() + "' and '" + right.getDisplayName() + "'", "This operation only works with numeric types (int, float)"));
                    return;
                }
            }

            if (op.equals(">") || op.equals("<") || op.equals(">=") || op.equals("<=")) {
                if ((left != TypeKind.UNKNOWN && right != TypeKind.UNKNOWN) && ((left != TypeKind.INT && left != TypeKind.FLOAT) || (right != TypeKind.INT && right != TypeKind.FLOAT) || (left != right))) {
                    diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E103_INCOMPATIBLE_TYPES, "TypeError: '" + op + "' not supported between instances of '" + left.getDisplayName() + "' and '" + right.getDisplayName() + "'", "Ensure both operands have compatible types"));
                }
            }
        } else if (expr instanceof JinjaCallExpr call) {
            SourceRange range = call.getSourceRange();
            JinjaExpr callee = call.getCallee();
            TypeKind calleeType = inferType(callee);

            // E104: Not callable - ONLY FOR TEMPLATE-LOCAL TYPES
            if (calleeType != TypeKind.UNKNOWN && (calleeType == TypeKind.INT || calleeType == TypeKind.STR || calleeType == TypeKind.FLOAT || calleeType == TypeKind.LIST || calleeType == TypeKind.DICT || calleeType == TypeKind.BOOL)) {
                String varName = extractName(callee);
                diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E104_NOT_CALLABLE, "TypeError: '" + calleeType.getDisplayName() + "' object is not callable", varName != null ? "Remove () after " + varName : "Remove trailing ()"));
                return;
            }

            if (callee instanceof JinjaIdentifierExpr id) {
                checkBuiltinFunctionArgs(id.getName(), call);
            }
        }
    }

    private void checkBuiltinFunctionArgs(String funcName, JinjaCallExpr call) {
        SourceRange range = call.getSourceRange();
        if (range == null) return;
        List<JinjaExpr> args = call.getArgs();

        if ("len".equals(funcName) && !args.isEmpty()) {
            TypeKind t = inferType(args.get(0));
            if (t != TypeKind.UNKNOWN && t != TypeKind.STR && t != TypeKind.LIST && t != TypeKind.DICT && t != TypeKind.TUPLE && t != TypeKind.SET) {
                diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE, "TypeError: object of type '" + t.getDisplayName() + "' has no len()", "len() expects a sequence or collection type (str, list, dict, tuple, set)"));
            }
        } else if ("sum".equals(funcName) && !args.isEmpty()) {
            JinjaExpr value = args.get(0);
            TypeKind t = inferType(value);
            if (t != TypeKind.UNKNOWN && t != TypeKind.LIST) {
                diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE, "TypeError: '" + t.getDisplayName() + "' object is not iterable", "sum() expects a list of numeric values"));
            }
        }
    }

    private String extractName(JinjaExpr expr) {
        if (expr instanceof JinjaIdentifierExpr id) return id.getName();
        if (expr instanceof JinjaNumberLiteralExpr num) return num.getText();
        if (expr instanceof JinjaStringLiteralExpr) return "\"string\"";
        return null;
    }
}