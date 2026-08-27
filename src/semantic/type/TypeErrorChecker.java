package semantic.type;

import AST.SourceRange;
import AST.flask.expr.*;
import AST.flask.literal.*;
import AST.flask.stmt.*;
import AST.flask.suite.BlockSuite;
import AST.flask.suite.InlineSuite;
import AST.flask.suite.Suite;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.ErrorCode;
import semantic.diagnostics.TypeKind;
import SymbolTable.NameResolver;
import SymbolTable.ScopeBinding;
import SymbolTable.Symbol;
import SymbolTable.SymbolTableRepository;
import SymbolTable.FlaskSymbolTable;

import java.util.List;
import java.util.Optional;

/**
 * TypeErrorChecker for Flask scopes (global + local)
 */
public class TypeErrorChecker {

    private final TypeInferenceEngine typeEngine;
    private final DiagnosticCollector diagnostics;
    private final SymbolTableRepository repository;

    public TypeErrorChecker(DiagnosticCollector diagnostics, SymbolTableRepository repository) {
        this.diagnostics = diagnostics;
        this.repository = repository;
        this.typeEngine = new TypeInferenceEngine();
    }

    /* -------------------- Program / Statement entry points -------------------- */

    public void checkProgram(AST.Program program) {
        if (program == null) return;
        for (var child : program.getChildren()) {
            if (child instanceof Statement stmt) {
                checkStatement(stmt);
            }
        }
    }

    private void checkStatement(Statement stmt) {
        switch (stmt) {
            case null -> {}
            case DecoratedStmt decoratedStmt -> checkStatement(decoratedStmt.getTarget());
            case AssignmentStmt asgnStmt -> checkAssignment(asgnStmt);
            case AssignmentChainStmt chainStmt -> checkAssignmentChain(chainStmt);
            case ExpressionStmt exprStmt -> checkExpression(exprStmt.getExpression(), exprStmt.getSourceRange());
            case IfStmt ifStmt -> checkIfStatement(ifStmt);
            case WhileStmt whileStmt -> checkWhileStatement(whileStmt);
            case ForStmt forStmt -> checkForStatement(forStmt);
            case FunctionDefStmt funcStmt -> checkFunctionDef(funcStmt);
            case ClassDefStmt classStmt -> checkClassDef(classStmt);
            case ReturnStmt retStmt -> {
                if (retStmt.getValue() != null) {
                    checkExpression(retStmt.getValue(), retStmt.getSourceRange());
                }
            }
            default -> {}
        }
    }

    private void checkSuite(Suite suite) {
        if (suite == null) return;
        if (suite instanceof BlockSuite blockSuite) {
            for (var child : blockSuite.getChildren()) {
                if (child instanceof Statement s) checkStatement(s);
            }
        } else if (suite instanceof InlineSuite inlineSuite) {
            for (var child : inlineSuite.getChildren()) {
                if (child instanceof Statement s) checkStatement(s);
            }
        }
    }

    /* -------------------- Compound statements -------------------- */

    private void checkIfStatement(IfStmt ifStmt) {
        checkExpression(ifStmt.getCondition(), ifStmt.getSourceRange());

        checkSuite(ifStmt.getThenSuite());

        for (Expression elifCond : ifStmt.getElifConditions()) {
            checkExpression(elifCond, elifCond.getSourceRange());
        }
        for (Suite elifSuite : ifStmt.getElifSuites()) {
            checkSuite(elifSuite);
        }

        checkSuite(ifStmt.getElseSuite());
    }

    private void checkWhileStatement(WhileStmt whileStmt) {
        checkExpression(whileStmt.getCondition(), whileStmt.getSourceRange());
        checkSuite(whileStmt.getBody());
    }

    private void checkForStatement(ForStmt forStmt) {
        checkExpression(forStmt.getIterable(), forStmt.getSourceRange());
        checkForIterableType(forStmt);

        if (forStmt.getIterator() instanceof IdentifierExpr idExpr) {
            typeEngine.recordVariableType(idExpr.getName(), TypeKind.UNKNOWN);
        }

        checkSuite(forStmt.getBody());
    }

    private void checkFunctionDef(FunctionDefStmt funcStmt) {
        typeEngine.enterScope();
        if (funcStmt.getParameters() != null) {
            for (String param : funcStmt.getParameters()) {
                typeEngine.recordVariableType(param, TypeKind.UNKNOWN);
            }
        }
        checkSuite(funcStmt.getBody());
        typeEngine.exitScope();
    }

    private void checkClassDef(ClassDefStmt classStmt) {
        typeEngine.enterScope();
        checkSuite(classStmt.getBody());
        typeEngine.exitScope();
    }

    /* -------------------- Assignments -------------------- */

    private void checkAssignment(AssignmentStmt asgnStmt) {
        Expression target = asgnStmt.getTarget();
        Expression value = asgnStmt.getValue();

        checkExpression(value, asgnStmt.getSourceRange());
        TypeKind valueType = typeEngine.inferType(value);

        if (target instanceof IdentifierExpr id) {
            typeEngine.recordVariableType(id.getName(), valueType);
            persistTypeToSymbol(id.getName(), valueType);
            recordListElementTypeIfNeeded(id.getName(), value);
        }
    }

    private void checkAssignmentChain(AssignmentChainStmt chainStmt) {
        Expression value = chainStmt.getValue();

        checkExpression(value, chainStmt.getSourceRange());
        TypeKind valueType = typeEngine.inferType(value);

        for (Expression target : chainStmt.getTargets()) {
            if (target instanceof IdentifierExpr id) {
                typeEngine.recordVariableType(id.getName(), valueType);
                persistTypeToSymbol(id.getName(), valueType);
                recordListElementTypeIfNeeded(id.getName(), value);
            }
        }
    }

    private void recordListElementTypeIfNeeded(String variableName, Expression value) {
        if (value instanceof ListLiteralExpr listLiteral) {
            TypeKind elementType = inferListElementType(listLiteral);
            typeEngine.recordListElementType(variableName, elementType);
        }
    }

    /* -------------------- Expressions -------------------- */

    private void checkExpression(Expression expr, SourceRange sr) {
        switch (expr) {
            case null -> {}
            case BinaryExpr b -> checkBinaryExpression(b);
            case CompareExpr c -> checkComparison(c);
            case CallExpr call -> checkCallExpression(call);
            case UnaryExpr u -> checkUnaryExpression(u);
            default -> {}
        }

        for (var child : expr.getChildren()) {
            if (child instanceof Expression sub) {
                checkExpression(sub, sub.getSourceRange());
            }
        }
    }

    private void checkExpression(Expression expr) {
        checkExpression(expr, expr != null ? expr.getSourceRange() : null);
    }

    private void checkBinaryExpression(BinaryExpr bin) {
        TypeKind left = typeEngine.inferType(bin.getLeft());
        TypeKind right = typeEngine.inferType(bin.getRight());
        String op = bin.getOperator();
        SourceRange range = bin.getSourceRange();

        if (range == null) return;
        if (isArithmeticOp(op)) {
            checkArithmeticValidity(left, right, op, range);
        }
    }

    private void checkArithmeticValidity(TypeKind left, TypeKind right, String op, SourceRange range) {
        if (op.equals("+")) {
            checkAdditionValidity(left, right, op, range);
        }

        if (op.equals("-") || op.equals("/") || op.equals("//")) {
            checkSubDivValidity(left, right, op, range);
        }
    }

    private void checkAdditionValidity(TypeKind left, TypeKind right, String op, SourceRange range) {
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
                    "Use list concatenation with another list or use list.append/extend"));
        }
    }

    private void checkSubDivValidity(TypeKind left, TypeKind right, String op, SourceRange range) {
        boolean strAndStr = left == TypeKind.STR && right == TypeKind.STR;
        boolean strAndInt = (left == TypeKind.STR && right == TypeKind.INT)
                || (left == TypeKind.INT && right == TypeKind.STR);
        boolean strAndFloat = (left == TypeKind.STR && right == TypeKind.FLOAT)
                || (left == TypeKind.FLOAT && right == TypeKind.STR);

        if (strAndStr) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                    "TypeError: unsupported operand type(s) for " + op + ": 'str' and 'str'",
                    "This operation only works with numeric types (int, float)"));
        } else if (strAndInt) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                    "TypeError: unsupported operand type(s) for " + op + ": 'str' and 'int'",
                    "This operation only works with numeric types (int, float)"));
        } else if (strAndFloat) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                    "TypeError: unsupported operand type(s) for " + op + ": 'str' and 'float'",
                    "This operation only works with numeric types (int, float)"));
        }
    }

    private void checkComparison(CompareExpr cmp) {
        TypeKind left = typeEngine.inferType(cmp.getLeft());
        List<String> ops = cmp.getOperators();
        List<Expression> rights = cmp.getRights();

        for (int i = 0; i < ops.size(); i++) {
            TypeKind right = typeEngine.inferType(rights.get(i));
            String op = ops.get(i);
            checkComparisonValidity(left, right, op, cmp.getSourceRange());
            left = right;
        }
    }

    private void checkComparisonValidity(TypeKind left, TypeKind right, String op, SourceRange range) {
        if (range == null) return;

        boolean strAndInt = (left == TypeKind.STR && right == TypeKind.INT)
                || (left == TypeKind.INT && right == TypeKind.STR);
        if (strAndInt) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E103_INCOMPATIBLE_TYPES,
                    "TypeError: '" + op + "' not supported between instances of 'str' and 'int'",
                    "Ensure both operands have compatible types"));
        }

        boolean listAndStr = (left == TypeKind.LIST && right == TypeKind.STR)
                || (left == TypeKind.STR && right == TypeKind.LIST);
        if (listAndStr && !op.equals("==") && !op.equals("!=")) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E103_INCOMPATIBLE_TYPES,
                    "TypeError: '" + op + "' not supported between instances of 'list' and 'str'",
                    "Only == and != work with mixed container and string types"));
        }
    }

    private void checkCallExpression(CallExpr call) {
        Expression func = call.getFunction();
        TypeKind funcType = typeEngine.inferType(func);
        SourceRange range = call.getSourceRange();

        if (range == null) return;

        if (isNonCallableType(funcType)) {
            String varName = extractName(func);
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E104_NOT_CALLABLE,
                    "TypeError: '" + funcType.getDisplayName() + "' object is not callable",
                    varName != null ? "Remove () after " + varName + " or make it a function" : "Remove trailing ()"));
            return;
        }

        for (Argument arg : call.getArguments()) {
            if (arg instanceof PositionalArgument pos) {
                checkExpression(pos.getValue(), pos.getValue().getSourceRange());
            } else if (arg instanceof KeywordArgument key) {
                checkExpression(key.getValue(), key.getValue().getSourceRange());
            }
        }

        if (func instanceof IdentifierExpr id) {
            checkBuiltinFunctionArgs(id.getName(), call);
        }
    }

    private boolean isNonCallableType(TypeKind type) {
        return type == TypeKind.INT || type == TypeKind.STR || type == TypeKind.FLOAT
                || type == TypeKind.LIST || type == TypeKind.DICT || type == TypeKind.BOOL;
    }

    private void checkBuiltinFunctionArgs(String funcName, CallExpr call) {
        SourceRange range = call.getSourceRange();
        if (range == null) return;

        List<Argument> args = call.getArguments();
        if (args.isEmpty()) return;

        Argument firstArg = args.get(0);
        if (!(firstArg instanceof PositionalArgument pos)) return;

        switch (funcName) {
            case "len" -> checkLenArgument(pos, range);
            case "abs" -> checkAbsArgument(pos, range);
            case "sum" -> checkSumArgument(pos, range);
            default -> {}
        }
    }

    private void checkLenArgument(PositionalArgument pos, SourceRange range) {
        TypeKind argType = typeEngine.inferType(pos.getValue());
        boolean validLenType = argType == TypeKind.STR || argType == TypeKind.LIST
                || argType == TypeKind.DICT || argType == TypeKind.TUPLE
                || argType == TypeKind.SET || argType == TypeKind.UNKNOWN;

        if (!validLenType) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE,
                    "TypeError: object of type '" + argType.getDisplayName() + "' has no len()",
                    "len() expects a sequence or collection type (str, list, dict, tuple, set)"));
        }
    }

    private void checkAbsArgument(PositionalArgument pos, SourceRange range) {
        TypeKind argType = typeEngine.inferType(pos.getValue());
        boolean validAbsType = argType == TypeKind.INT || argType == TypeKind.FLOAT
                || argType == TypeKind.UNKNOWN;

        if (!validAbsType) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE,
                    "TypeError: bad operand type for abs(): '" + argType.getDisplayName() + "'",
                    "abs() expects a numeric type (int or float)"));
        }
    }

    private void checkSumArgument(PositionalArgument pos, SourceRange range) {
        Expression value = pos.getValue();
        TypeKind argType = typeEngine.inferType(value);

        if (argType != TypeKind.LIST && argType != TypeKind.UNKNOWN) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE,
                    "TypeError: '" + argType.getDisplayName() + "' object is not iterable",
                    "sum() expects a list of numeric values"));
            return;
        }

        if (value instanceof ListLiteralExpr listLiteral) {
            for (Expression element : listLiteral.getElements()) {
                TypeKind elementType = typeEngine.inferType(element);
                if (elementType != TypeKind.INT && elementType != TypeKind.FLOAT
                        && elementType != TypeKind.UNKNOWN) {
                    diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE,
                            "TypeError: unsupported operand type '" + elementType.getDisplayName() + "' in sum()",
                            "sum() expects all list elements to be numeric (int or float)"));
                    break;
                }
            }
        } else if (value instanceof IdentifierExpr id) {
            TypeKind elemType = typeEngine.lookupListElementType(id.getName());
            if (elemType != TypeKind.UNKNOWN && elemType != TypeKind.INT && elemType != TypeKind.FLOAT) {
                diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE,
                        "TypeError: unsupported operand type '" + elemType.getDisplayName() + "' in sum()",
                        "sum() expects all list elements to be numeric (int or float)"));
            }
        }
    }

    private void checkUnaryExpression(UnaryExpr unary) {
        TypeKind operandType = typeEngine.inferType(unary.getExpression());
        String op = unary.getOperator();
        SourceRange range = unary.getSourceRange();

        if (range == null) return;

        if ((op.equals("-") || op.equals("+")) && operandType != TypeKind.INT
                && operandType != TypeKind.FLOAT && operandType != TypeKind.UNKNOWN) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                    "TypeError: bad operand type for unary " + op + ": '" + operandType.getDisplayName() + "'",
                    "Unary " + op + " only works with numeric types (int, float)"));
        }

        if (op.equals("~") && operandType != TypeKind.INT && operandType != TypeKind.UNKNOWN) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR,
                    "TypeError: bad operand type for unary ~: '" + operandType.getDisplayName() + "'",
                    "Unary ~ only works with int"));
        }
    }

    private void checkForIterableType(ForStmt forStmt) {
        Expression iterable = forStmt.getIterable();
        SourceRange range = forStmt.getSourceRange();

        if (iterable == null || range == null) return;

        TypeKind iterableType = typeEngine.inferType(iterable);

        if (!isIterableType(iterableType)) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E107_NOT_ITERABLE,
                    "TypeError: '" + iterableType.getDisplayName() + "' object is not iterable",
                    "for loop requires an iterable type (list, dict, tuple, set, or str)"));
        }
    }

    /* -------------------- Helpers -------------------- */

    private boolean isIterableType(TypeKind type) {
        if (type == null) return false;

        if (type == TypeKind.LIST || type == TypeKind.DICT || type == TypeKind.TUPLE
                || type == TypeKind.SET || type == TypeKind.STR) {
            return true;
        }

        return type == TypeKind.UNKNOWN || type == TypeKind.ANY;
    }

    private boolean isArithmeticOp(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*")
                || op.equals("/") || op.equals("//") || op.equals("%");
    }

    private String extractName(Expression expr) {
        return expr instanceof IdentifierExpr id ? id.getName() : null;
    }

    private TypeKind inferListElementType(ListLiteralExpr listLiteral) {
        boolean hasInt = false;
        boolean hasFloat = false;
        TypeKind nonNumeric = TypeKind.UNKNOWN;

        for (Expression element : listLiteral.getElements()) {
            TypeKind elementType = typeEngine.inferType(element);
            if (elementType == TypeKind.INT) {
                hasInt = true;
            } else if (elementType == TypeKind.FLOAT) {
                hasFloat = true;
            } else if (elementType != TypeKind.UNKNOWN) {
                return elementType;   // non-numeric found
            }
        }

        if (hasFloat) return TypeKind.FLOAT;
        if (hasInt) return TypeKind.INT;
        return TypeKind.UNKNOWN;
    }

    private void persistTypeToSymbol(String varName, TypeKind type) {
        if (repository == null || varName == null) return;

        if (repository.getFlaskGlobal() instanceof FlaskSymbolTable flaskRoot) {
            Optional<Symbol> deep = flaskRoot.findDeepest(varName);
            if (deep.isPresent()) {
                Symbol symbol = deep.get();
                if (symbol != null) {
                    symbol.setInferredType(type);
                    return;
                }
            }
        }

        Optional<ScopeBinding> binding = NameResolver.resolve(repository.getFlaskGlobal(), varName);
        if (binding.isPresent()) {
            Symbol symbol = binding.get().getSymbol();
            if (symbol != null) {
                symbol.setInferredType(type);
            }
        }
    }
}