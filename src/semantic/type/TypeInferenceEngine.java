package semantic.type;

import AST.flask.expr.*;
import AST.flask.literal.*;
import semantic.diagnostics.TypeKind;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class TypeInferenceEngine {

    private final Stack<Map<String, TypeKind>> scopeStack = new Stack<>();
    private final Map<String, TypeKind> listElementTypes = new HashMap<>();

    public TypeInferenceEngine() {
        scopeStack.push(new HashMap<>()); // global scope
    }

    /* -------------------- Scope management -------------------- */

    public void enterScope() {
        scopeStack.push(new HashMap<>(scopeStack.peek()));
    }

    public void exitScope() {
        if (scopeStack.size() > 1) {
            scopeStack.pop();
        }
    }

    /* -------------------- Variable types -------------------- */

    public void recordVariableType(String name, TypeKind type) {
        if (!scopeStack.isEmpty()) {
            scopeStack.peek().put(name, type);
        }
    }

    public TypeKind lookupVariableType(String name) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, TypeKind> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return TypeKind.UNKNOWN;
    }

    /* -------------------- List element types -------------------- */

    public void recordListElementType(String name, TypeKind elementType) {
        if (name != null) {
            listElementTypes.put(name, elementType != null ? elementType : TypeKind.UNKNOWN);
        }
    }

    public TypeKind lookupListElementType(String name) {
        return listElementTypes.getOrDefault(name, TypeKind.UNKNOWN);
    }

    /* -------------------- Type inference -------------------- */

    public TypeKind inferType(Expression expr) {
        return switch (expr) {
            case null -> TypeKind.UNKNOWN;

            // Literals
            case StringLiteralExpr ignored -> TypeKind.STR;
            case NumberLiteralExpr numExpr -> inferNumberType(numExpr);
            case BooleanLiteralExpr ignored -> TypeKind.BOOL;
            case NoneLiteralExpr ignored -> TypeKind.NONE;
            case ListLiteralExpr ignored -> TypeKind.LIST;
            case SetLiteralExpr ignored -> TypeKind.SET;

            // Identifiers
            case IdentifierExpr idExpr -> lookupVariableType(idExpr.getName());

            // Binary operations
            case BinaryExpr binExpr -> inferBinaryOpType(binExpr);

            // Comparisons
            case CompareExpr ignored -> TypeKind.BOOL;

            // Unary operations
            case UnaryExpr unaryExpr -> inferUnaryOpType(unaryExpr);

            // Function calls
            case CallExpr callExpr -> inferCallType(callExpr);

            // Attributes
            case AttributeExpr ignored -> TypeKind.UNKNOWN;

            // Index access
            case IndexExpr idxExpr -> inferIndexType(idxExpr);

            // Lambda functions
            case LambdaExpr ignored -> TypeKind.FUNCTION;

            default -> TypeKind.UNKNOWN;
        };
    }

    /* -------------------- Helpers -------------------- */

    private TypeKind inferNumberType(NumberLiteralExpr numExpr) {
        String value = numExpr.getValue();
        if (value.contains(".") || value.contains("e") || value.contains("E")) {
            return TypeKind.FLOAT;
        }
        return TypeKind.INT;
    }

    private TypeKind inferBinaryOpType(BinaryExpr binExpr) {
        TypeKind left = inferType(binExpr.getLeft());
        TypeKind right = inferType(binExpr.getRight());
        String op = binExpr.getOperator();

        if (isArithmeticOperator(op)) {
            return inferArithmeticType(left, right, op);
        }

        if (isBitwiseOperator(op)) {
            boolean bothIntOrUnknown = (left == TypeKind.INT || left == TypeKind.UNKNOWN)
                    && (right == TypeKind.INT || right == TypeKind.UNKNOWN);
            if (bothIntOrUnknown) {
                return TypeKind.INT;
            }
        }

        if (isBooleanOperator(op)) {
            return TypeKind.BOOL;
        }

        return TypeKind.UNKNOWN;
    }

    private TypeKind inferArithmeticType(TypeKind left, TypeKind right, String op) {
        // String/List concatenation
        if (op.equals("+")) {
            if (left == TypeKind.STR && right == TypeKind.STR) return TypeKind.STR;
            if (left == TypeKind.LIST && right == TypeKind.LIST) return TypeKind.LIST;
        }

        // List/String repetition
        if (op.equals("*")) {
            boolean listTimesInt = (left == TypeKind.LIST && right == TypeKind.INT)
                    || (left == TypeKind.INT && right == TypeKind.LIST);
            boolean strTimesInt = (left == TypeKind.STR && right == TypeKind.INT)
                    || (left == TypeKind.INT && right == TypeKind.STR);

            if (listTimesInt || strTimesInt) {
                return left == TypeKind.INT ? right : left;
            }
        }

        // Numeric operations
        if (left == TypeKind.INT && right == TypeKind.INT) {
            return op.equals("/") ? TypeKind.FLOAT : TypeKind.INT;
        }

        boolean bothNumeric = (left == TypeKind.INT || left == TypeKind.FLOAT)
                && (right == TypeKind.INT || right == TypeKind.FLOAT);
        if (bothNumeric) {
            boolean shouldBeFloat = op.equals("/") || left == TypeKind.FLOAT || right == TypeKind.FLOAT;
            return shouldBeFloat ? TypeKind.FLOAT : TypeKind.INT;
        }

        return TypeKind.UNKNOWN;
    }

    private TypeKind inferUnaryOpType(UnaryExpr unaryExpr) {
        String op = unaryExpr.getOperator();
        TypeKind operandType = inferType(unaryExpr.getExpression());

        if (op.equals("-") || op.equals("+")) {
            if (operandType == TypeKind.INT || operandType == TypeKind.FLOAT) {
                return operandType;
            }
        }

        if (op.equals("~")) {
            if (operandType == TypeKind.INT) {
                return TypeKind.INT;
            }
        }

        if (op.equals("not")) {
            return TypeKind.BOOL;
        }

        return TypeKind.UNKNOWN;
    }

    private TypeKind inferCallType(CallExpr callExpr) {
        Expression func = callExpr.getFunction();

        if (func instanceof IdentifierExpr idExpr) {
            return inferBuiltinCallType(idExpr.getName());
        }

        return TypeKind.UNKNOWN;
    }

    private TypeKind inferBuiltinCallType(String funcName) {
        return switch (funcName) {
            case "len", "int", "sum" -> TypeKind.INT;
            case "str" -> TypeKind.STR;
            case "float" -> TypeKind.FLOAT;
            case "bool" -> TypeKind.BOOL;
            case "list", "range", "enumerate", "zip", "map", "filter", "sorted", "reversed" -> TypeKind.LIST;
            case "dict" -> TypeKind.DICT;
            case "set" -> TypeKind.SET;
            case "tuple" -> TypeKind.TUPLE;
            case "type" -> TypeKind.CLASS;
            case "print" -> TypeKind.NONE;
            case "max", "min" -> TypeKind.UNKNOWN;
            default -> TypeKind.UNKNOWN;
        };
    }

    private TypeKind inferIndexType(IndexExpr indexExpr) {
        TypeKind containerType = inferType(indexExpr.getBase());

        return switch (containerType) {
            case LIST, DICT -> TypeKind.UNKNOWN;
            case STR -> TypeKind.STR;
            default -> TypeKind.UNKNOWN;
        };
    }

    /* -------------------- Operator helpers -------------------- */

    private boolean isArithmeticOperator(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*")
                || op.equals("/") || op.equals("//") || op.equals("%");
    }

    private boolean isBitwiseOperator(String op) {
        return op.equals("&") || op.equals("|") || op.equals("^")
                || op.equals("<<") || op.equals(">>");
    }

    private boolean isBooleanOperator(String op) {
        return op.equals("and") || op.equals("or");
    }
}