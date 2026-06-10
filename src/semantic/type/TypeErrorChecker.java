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
 * TypeErrorChecker flask in scopes (global + local)
 */
public class TypeErrorChecker {

    private final TypeInferenceEngine typeEngine;
    private final DiagnosticCollector diagnostics;
    private final SymbolTableRepository repository; // optional, used to persist inferred types to Symbol objects

    public TypeErrorChecker(DiagnosticCollector diagnostics, SymbolTableRepository repository) {
        this.diagnostics = diagnostics;
        this.repository = repository;
        this.typeEngine = new TypeInferenceEngine();
    }

    public void checkProgram(AST.Program program) {
        if (program == null) return;
        for (var child : program.getChildren()) {
            if (child instanceof Statement stmt) checkStatement(stmt);
        }
    }

    private void checkStatement(Statement stmt) {
        if (stmt == null) return;

        if (stmt instanceof DecoratedStmt decoratedStmt) {
            Statement target = decoratedStmt.getTarget();
            if (target != null) {
                checkStatement(target);
            }
            return;
        }

        if (stmt instanceof AssignmentStmt asgnStmt) {
            checkAssignment(asgnStmt);
            return;
        }
        if (stmt instanceof AssignmentChainStmt chainStmt) {
            checkAssignmentChain(chainStmt);
            return;
        }
        if (stmt instanceof ExpressionStmt exprStmt) {
            checkExpression(exprStmt.getExpression(), exprStmt.getSourceRange());
            return;
        }
        if (stmt instanceof IfStmt ifStmt) {
            checkExpression(ifStmt.getCondition(), ifStmt.getSourceRange());
            if (ifStmt.getThenSuite() != null) checkSuite(ifStmt.getThenSuite());
            if (ifStmt.getElseSuite() != null) checkSuite(ifStmt.getElseSuite());
            return;
        }
        if (stmt instanceof WhileStmt whileStmt) {
            checkExpression(whileStmt.getCondition(), whileStmt.getSourceRange());
            if (whileStmt.getBody() != null) checkSuite(whileStmt.getBody());
            return;
        }
        if (stmt instanceof ForStmt forStmt) {
            checkExpression(forStmt.getIterable(), forStmt.getSourceRange());
            checkForStatement(forStmt);
            if (forStmt.getIterator() instanceof IdentifierExpr idExpr) {
                typeEngine.recordVariableType(idExpr.getName(), TypeKind.UNKNOWN);
            }
            if (forStmt.getBody() != null) checkSuite(forStmt.getBody());
            return;
        }
        if (stmt instanceof FunctionDefStmt funcStmt) {
            checkFunctionDef(funcStmt);
            return;
        }
        if (stmt instanceof ClassDefStmt classStmt) {
            checkClassDef(classStmt);
            return;
        }
        if (stmt instanceof ReturnStmt retStmt) {
            if (retStmt.getValue() != null) checkExpression(retStmt.getValue(), retStmt.getSourceRange());
        }
    }

    private void checkSuite(Suite suite) {
        if (suite == null) return;
        if (suite instanceof BlockSuite blockSuite) {
            for (var child : blockSuite.getChildren()) if (child instanceof Statement s) checkStatement(s);
        } else if (suite instanceof InlineSuite inlineSuite) {
            for (var child : inlineSuite.getChildren()) if (child instanceof Statement s) checkStatement(s);
        }
    }


    private void checkFunctionDef(FunctionDefStmt funcStmt) {
        typeEngine.enterScope();
        if (funcStmt.getParameters() != null) {
            for (String p : funcStmt.getParameters()) typeEngine.recordVariableType(p, TypeKind.UNKNOWN);
        }
        if (funcStmt.getBody() != null) checkSuite(funcStmt.getBody());
        typeEngine.exitScope();
    }


    private void checkClassDef(ClassDefStmt classStmt) {
        typeEngine.enterScope();
        if (classStmt.getBody() != null) checkSuite(classStmt.getBody());
        typeEngine.exitScope();
    }

    private void checkAssignment(AssignmentStmt asgnStmt) {
        Expression target = asgnStmt.getTarget();
        Expression value = asgnStmt.getValue();
        checkExpression(value, asgnStmt.getSourceRange());
        TypeKind valueType = typeEngine.inferType(value);
        if (target instanceof IdentifierExpr id) {
            typeEngine.recordVariableType(id.getName(), valueType);

            // persist inferred type into the Flask symbol table if available
            persistTypeToSymbol(id.getName(), valueType);

            // new: if assigned from a list literal, infer and record element type for this variable
            if (value instanceof ListLiteralExpr listLiteral) {
                boolean hasInt = false;
                boolean hasFloat = false;
                TypeKind nonNumeric = TypeKind.UNKNOWN;
                for (Expression element : listLiteral.getElements()) {
                    TypeKind elType = typeEngine.inferType(element);
                    if (elType == TypeKind.INT) hasInt = true;
                    else if (elType == TypeKind.FLOAT) hasFloat = true;
                    else if (elType != TypeKind.UNKNOWN) {
                        nonNumeric = elType;
                        break;
                    }
                }
                TypeKind elementType = TypeKind.UNKNOWN;
                if (nonNumeric != TypeKind.UNKNOWN) {
                    elementType = nonNumeric;
                } else if (hasFloat && hasInt) {
                    elementType = TypeKind.FLOAT;
                } else if (hasFloat) {
                    elementType = TypeKind.FLOAT;
                } else if (hasInt) {
                    elementType = TypeKind.INT;
                } else {
                    elementType = TypeKind.UNKNOWN;
                }
                typeEngine.recordListElementType(id.getName(), elementType);
            }
        }
    }

    private void checkAssignmentChain(AssignmentChainStmt chainStmt) {
        Expression value = chainStmt.getValue();
        checkExpression(value, chainStmt.getSourceRange());
        TypeKind t = typeEngine.inferType(value);
        for (Expression target : chainStmt.getTargets()) {
            if (target instanceof IdentifierExpr id) {
                typeEngine.recordVariableType(id.getName(), t);

                // persist inferred type into the Flask symbol table if available
                persistTypeToSymbol(id.getName(), t);


                // new: handle chain assignment from list literal similarly
                if (value instanceof ListLiteralExpr listLiteral) {
                    boolean hasInt = false;
                    boolean hasFloat = false;
                    TypeKind nonNumeric = TypeKind.UNKNOWN;
                    for (Expression element : listLiteral.getElements()) {
                        TypeKind elType = typeEngine.inferType(element);
                        if (elType == TypeKind.INT) hasInt = true;
                        else if (elType == TypeKind.FLOAT) hasFloat = true;
                        else if (elType != TypeKind.UNKNOWN) {
                            nonNumeric = elType;
                            break;
                        }
                    }
                    TypeKind elementType = TypeKind.UNKNOWN;
                    if (nonNumeric != TypeKind.UNKNOWN) {
                        elementType = nonNumeric;
                    } else if (hasFloat && hasInt) {
                        elementType = TypeKind.FLOAT;
                    } else if (hasFloat) {
                        elementType = TypeKind.FLOAT;
                    } else if (hasInt) {
                        elementType = TypeKind.INT;
                    } else {
                        elementType = TypeKind.UNKNOWN;
                    }
                    typeEngine.recordListElementType(id.getName(), elementType);
                }
            }
        }
    }

    private void checkExpression(Expression expr, SourceRange sr) {
        if (expr == null) return;

        if (expr instanceof BinaryExpr b) checkBinaryExpression(b);
        else if (expr instanceof CompareExpr c) checkComparison(c);
        else if (expr instanceof CallExpr call) checkCallExpression(call);
        else if (expr instanceof UnaryExpr u) checkUnaryExpression(u);

        for (var child : expr.getChildren())
            if (child instanceof Expression sub) checkExpression(sub, sub.getSourceRange());
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
        if (isArithmeticOp(op)) checkArithmeticValidity(left, right, op, range);
    }


    private void checkArithmeticValidity(TypeKind left, TypeKind right, String op, SourceRange range) {
        if (op.equals("+")) {
            // string + int
            if ((left == TypeKind.STR && right == TypeKind.INT) || (left == TypeKind.INT && right == TypeKind.STR)) {
                diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR, "TypeError: can only concatenate str (not \"int\") to str", "Use str() to convert int to string"));
                return;
            }
            // list + int => E105
            if ((left == TypeKind.LIST && right == TypeKind.INT) || (left == TypeKind.INT && right == TypeKind.LIST)) {
                diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E105_INVALID_LIST_OPERATION, "TypeError: can only concatenate list (not \"int\") to list", "Use list concatenation with another list or use list.append/extend"));
                return;
            }
        }
        if (op.equals("-") || op.equals("/") || op.equals("//")) {
            if (left == TypeKind.STR && right == TypeKind.STR) {
                diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR, "TypeError: unsupported operand type(s) for " + op + ": 'str' and 'str'", "This operation only works with numeric types (int, float)"));
            } else if ((left == TypeKind.STR && right == TypeKind.INT) || (right == TypeKind.STR && left == TypeKind.INT)) {
                diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR, "TypeError: unsupported operand type(s) for " + op + ": 'str' and 'int'", "This operation only works with numeric types (int, float)"));
            } else if ((left == TypeKind.STR && right == TypeKind.FLOAT) || (right == TypeKind.STR && left == TypeKind.FLOAT)) {
                diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR, "TypeError: unsupported operand type(s) for " + op + ": 'str' and 'float'", "This operation only works with numeric types (int, float)"));
            }
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
        if ((left == TypeKind.STR && right == TypeKind.INT) || (left == TypeKind.INT && right == TypeKind.STR)) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E103_INCOMPATIBLE_TYPES, "TypeError: '" + op + "' not supported between instances of 'str' and 'int'", "Ensure both operands have compatible types"));
        }
        if ((left == TypeKind.LIST && right == TypeKind.STR) || (left == TypeKind.STR && right == TypeKind.LIST)) {
            if (!op.equals("==") && !op.equals("!=")) {
                diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E103_INCOMPATIBLE_TYPES, "TypeError: '" + op + "' not supported between instances of 'list' and 'str'", "Only == and != work with mixed container and string types"));
            }
        }
    }

    private void checkCallExpression(CallExpr call) {
        Expression func = call.getFunction();
        TypeKind funcType = typeEngine.inferType(func);
        SourceRange range = call.getSourceRange();
        if (range == null) return;


        if (funcType == TypeKind.INT || funcType == TypeKind.STR || funcType == TypeKind.FLOAT || funcType == TypeKind.LIST || funcType == TypeKind.DICT || funcType == TypeKind.BOOL) {
            String varName = extractName(func);
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E104_NOT_CALLABLE, "TypeError: '" + funcType.getDisplayName() + "' object is not callable", varName != null ? "Remove () after " + varName + " or make it a function" : "Remove trailing ()"));
            return;
        }

        if (func instanceof IdentifierExpr id) checkBuiltinFunctionArgs(id.getName(), call);
    }


    private void checkBuiltinFunctionArgs(String funcName, CallExpr call) {
        SourceRange range = call.getSourceRange();
        if (range == null) return;
        List<Argument> args = call.getArguments();
        if ("len".equals(funcName) && !args.isEmpty()) {
            if (args.get(0) instanceof PositionalArgument pos) {
                TypeKind t = typeEngine.inferType(pos.getValue());
                if (t != TypeKind.STR && t != TypeKind.LIST && t != TypeKind.DICT && t != TypeKind.TUPLE && t != TypeKind.SET && t != TypeKind.UNKNOWN) {
                    diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE, "TypeError: object of type '" + t.getDisplayName() + "' has no len()", "len() expects a sequence or collection type (str, list, dict, tuple, set)"));
                }
            }
        } else if ("abs".equals(funcName) && !args.isEmpty()) {
            if (args.get(0) instanceof PositionalArgument pos) {
                TypeKind t = typeEngine.inferType(pos.getValue());
                if (t != TypeKind.INT && t != TypeKind.FLOAT && t != TypeKind.UNKNOWN) {
                    diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE, "TypeError: bad operand type for abs(): '" + t.getDisplayName() + "'", "abs() expects a numeric type (int or float)"));
                }
            }
        } else if ("sum".equals(funcName) && !args.isEmpty()) {
            if (args.get(0) instanceof PositionalArgument pos) {
                Expression value = pos.getValue();
                TypeKind t = typeEngine.inferType(value);
                // sum() requires an iterable
                if (t != TypeKind.LIST && t != TypeKind.UNKNOWN) {
                    diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE, "TypeError: '" + t.getDisplayName() + "' object is not iterable", "sum() expects a list of numeric values"));
                }
                // If the argument is a list literal, validate element types
                else if (value instanceof ListLiteralExpr listLiteral) {
                    for (Expression element : listLiteral.getElements()) {
                        TypeKind elementType = typeEngine.inferType(element);
                        if (elementType != TypeKind.INT && elementType != TypeKind.FLOAT && elementType != TypeKind.UNKNOWN) {
                            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE, "TypeError: unsupported operand type '" + elementType.getDisplayName() + "' in sum()", "sum() expects all list elements to be numeric (int or float)"));
                            break;
                        }
                    }
                }
                // new: if argument is an identifier referencing a list variable, check stored element type
                else if (value instanceof IdentifierExpr id) {
                    TypeKind elemType = typeEngine.lookupListElementType(id.getName());
                    if (elemType != TypeKind.UNKNOWN && elemType != TypeKind.INT && elemType != TypeKind.FLOAT) {
                        diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E106_INVALID_BUILTIN_USAGE, "TypeError: unsupported operand type '" + elemType.getDisplayName() + "' in sum()", "sum() expects all list elements to be numeric (int or float)"));
                    }
                }
            }
        }
    }


    private void checkUnaryExpression(UnaryExpr unary) {
        TypeKind opType = typeEngine.inferType(unary.getExpression());
        String op = unary.getOperator();
        SourceRange range = unary.getSourceRange();
        if (range == null) return;
        if ((op.equals("-") || op.equals("+")) && opType != TypeKind.INT && opType != TypeKind.FLOAT && opType != TypeKind.UNKNOWN) {
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E102_TYPE_ERROR, "TypeError: bad operand type for unary " + op + ": '" + opType.getDisplayName() + "'", "Unary " + op + " only works with numeric types (int, float)"));
        }
    }

    /**
     * Check for statement: verify iterable is actually iterable.
     */
    private void checkForStatement(ForStmt forStmt) {
        Expression iterable = forStmt.getIterable();
        SourceRange range = forStmt.getSourceRange();

        if (iterable == null || range == null) return;

        TypeKind iterableType = typeEngine.inferType(iterable);

        // E107: Check if type is iterable
        if (!isIterableType(iterableType)) {
            String iterableName = extractName(iterable);
            diagnostics.addDiagnostic(new Diagnostic(range, ErrorCode.E107_NOT_ITERABLE, "TypeError: '" + iterableType.getDisplayName() + "' object is not iterable", "for loop requires an iterable type (list, dict, tuple, set, or str)"));
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

    private boolean isArithmeticOp(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("//") || op.equals("%");
    }

    private String extractName(Expression expr) {
        if (expr instanceof IdentifierExpr id) return id.getName();
        return null;
    }

    /**
     * Persist inferred type into SymbolTable (Flask global) if symbol exists.
     * <p>
     * Improved behavior: try to find the deepest (most-nested) Flask symbol with the given name
     * so we update local/nested scope symbols (not only global).
     */
    private void persistTypeToSymbol(String varName, TypeKind type) {
        if (repository == null || varName == null) return;
        if (repository.getFlaskGlobal() instanceof FlaskSymbolTable flaskRoot) {
            Optional<Symbol> deep = flaskRoot.findDeepest(varName);
            if (deep.isPresent()) {
                Symbol sym = deep.get();
                if (sym != null) {
                    sym.setInferredType(type);
                    //System.out.println(sym.getName()+" ----------------------------------- " +sym.getInferredType());
                    return;
                }
            }
        }

        // fallback: resolve starting from flask global (previous behavior)
        Optional<ScopeBinding> binding = NameResolver.resolve(repository.getFlaskGlobal(), varName);
        if (binding.isPresent()) {
            Symbol sym = binding.get().getSymbol();
            if (sym != null) {
                sym.setInferredType(type);
            }
        }
    }
}