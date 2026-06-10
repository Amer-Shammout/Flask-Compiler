package semantic.type;

import AST.flask.expr.*;
import AST.flask.literal.*;
import semantic.diagnostics.TypeKind;

import java.util.*;


public class TypeInferenceEngine {

    private final Stack<Map<String, TypeKind>> scopeStack = new Stack<>();

    // new: store inferred per-variable list-element types (non-parametric)
    private final Map<String, TypeKind> listElementTypes = new HashMap<>();

    public TypeInferenceEngine() {
        //  global scope
        scopeStack.push(new HashMap<>());
    }

    /**
     * open new scope (ex: in function)
     */
    public void enterScope() {
        scopeStack.push(new HashMap<>(scopeStack.peek()));
    }

    /**
     * close scope
     */
    public void exitScope() {
        if (scopeStack.size() > 1) {
            scopeStack.pop();
        }
    }


    public void recordVariableType(String name, TypeKind type) {
        if (!scopeStack.isEmpty()) {
            scopeStack.peek().put(name, type);
        }
    }

    // new: record per-variable list element type (e.g., arr -> INT / FLOAT / STR / UNKNOWN)
    public void recordListElementType(String name, TypeKind elementType) {
        if (name != null) {
            listElementTypes.put(name, elementType != null ? elementType : TypeKind.UNKNOWN);
        }
    }

    // new: lookup recorded element type for a variable (returns UNKNOWN if not recorded)
    public TypeKind lookupListElementType(String name) {
        return listElementTypes.getOrDefault(name, TypeKind.UNKNOWN);
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

    /**
     * infer type
     */
    public TypeKind inferType(Expression expr) {
        if (expr == null) {
            return TypeKind.UNKNOWN;
        }

        // Literals
        if (expr instanceof StringLiteralExpr) {
            return TypeKind.STR;
        }
        if (expr instanceof NumberLiteralExpr numExpr) {
            return inferNumberType(numExpr);
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
        if (expr instanceof SetLiteralExpr) {
            return TypeKind.SET;
        }

        // Identifiers - search of variable
        if (expr instanceof IdentifierExpr idExpr) {
            return lookupVariableType(idExpr.getName());
        }

        // Binary operations
        if (expr instanceof BinaryExpr binExpr) {
            return inferBinaryOpType(binExpr);
        }

        // Comparisons return bool
        if (expr instanceof CompareExpr) {
            return TypeKind.BOOL;
        }

        // Unary operations
        if (expr instanceof UnaryExpr unaryExpr) {
            return inferUnaryOpType(unaryExpr);
        }

        // Function calls
        if (expr instanceof CallExpr callExpr) {
            return inferCallType(callExpr);
        }

        // Attributes
        if (expr instanceof AttributeExpr) {
            return TypeKind.UNKNOWN;
        }

        // Index access
        if (expr instanceof IndexExpr idxExpr) {
            return inferIndexType(idxExpr);
        }

        // Lambda functions
        if (expr instanceof LambdaExpr) {
            return TypeKind.FUNCTION;
        }

        return TypeKind.UNKNOWN;
    }

    private TypeKind inferNumberType(NumberLiteralExpr numExpr) {
        String value = numExpr.getValue();
        if (value.contains(".")) {
            return TypeKind.FLOAT;
        }
        return TypeKind.INT;
    }


    private TypeKind inferBinaryOpType(BinaryExpr binExpr) {
        TypeKind left = inferType(binExpr.getLeft());
        TypeKind right = inferType(binExpr.getRight());
        String op = binExpr.getOperator();

        // Arithmetic operations
        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("//") || op.equals("%")) {
            return inferArithmeticType(left, right, op);
        }

        // Bitwise operations
        if (op.equals("&") || op.equals("|") || op.equals("^") || op.equals("<<") || op.equals(">>")) {
            if ((left == TypeKind.INT || left == TypeKind.UNKNOWN) && (right == TypeKind.INT || right == TypeKind.UNKNOWN)) {
                return TypeKind.INT;
            }
        }


        // Boolean operations
        if (op.equals("and") || op.equals("or")) {
            return TypeKind.BOOL;
        }

        return TypeKind.UNKNOWN;
    }

    private TypeKind inferArithmeticType(TypeKind left, TypeKind right, String op) {
        // String concatenation
        if (op.equals("+")) {
            if (left == TypeKind.STR && right == TypeKind.STR) {
                return TypeKind.STR;
            }
            if (left == TypeKind.LIST && right == TypeKind.LIST) {
                return TypeKind.LIST;
            }
        }

        // List/String repetition
        if (op.equals("*")) {
            if ((left == TypeKind.LIST && right == TypeKind.INT) || (left == TypeKind.INT && right == TypeKind.LIST) || (left == TypeKind.STR && right == TypeKind.INT) || (left == TypeKind.INT && right == TypeKind.STR)) {
                return left == TypeKind.INT ? right : left;
            }
        }

        // Numeric operations
        if (left == TypeKind.INT && right == TypeKind.INT) {
            return op.equals("/") ? TypeKind.FLOAT : TypeKind.INT;
        }
        if ((left == TypeKind.INT || left == TypeKind.FLOAT) && (right == TypeKind.INT || right == TypeKind.FLOAT)) {
            return op.equals("/") || left == TypeKind.FLOAT || right == TypeKind.FLOAT ? TypeKind.FLOAT : TypeKind.INT;
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
        switch (funcName) {
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
            case "set":
                return TypeKind.SET;
            case "tuple":
                return TypeKind.TUPLE;
            case "type":
                return TypeKind.CLASS;
            case "print":
                return TypeKind.NONE;
            case "range":
                return TypeKind.LIST;
            case "enumerate":
                return TypeKind.LIST;
            case "zip":
                return TypeKind.LIST;
            case "map":
                return TypeKind.LIST;
            case "filter":
                return TypeKind.LIST;
            case "sorted":
                return TypeKind.LIST;
            case "reversed":
                return TypeKind.LIST;
            case "sum":
                return TypeKind.INT;
            case "min":
            case "max":
                return TypeKind.UNKNOWN;
            default:
                return TypeKind.UNKNOWN;
        }
    }

    private TypeKind inferIndexType(IndexExpr indexExpr) {
        TypeKind containerType = inferType(indexExpr.getBase());
        if (containerType == TypeKind.LIST) {
            return TypeKind.UNKNOWN;
        }
        if (containerType == TypeKind.DICT) {
            return TypeKind.UNKNOWN;
        }
        if (containerType == TypeKind.STR) {
            return TypeKind.STR;
        }
        return TypeKind.UNKNOWN;
    }
}
