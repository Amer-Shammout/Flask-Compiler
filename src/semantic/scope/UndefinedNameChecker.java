package semantic.scope;

import AST.Program;
import AST.SourceRange;
import AST.flask.expr.*;
import AST.flask.stmt.*;
import AST.flask.suite.BlockSuite;
import AST.flask.suite.InlineSuite;
import AST.flask.suite.Suite;
import SymbolTable.FlaskSymbolTable;
import SymbolTable.NameResolver;
import SymbolTable.ScopeBinding;
import SymbolTable.Symbol;
import SymbolTable.SymbolTableRepository;
import semantic.diagnostics.DiagnosticCollector;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Utility checker that emits E001 / E002 based on existing symbol tables.
 * <p>
 * Responsibilities:
 * - Walk Flask AST to emit:
 * - E001 when identifier use can't be resolved via symbol tables
 * - E002 when a function call's callee name cannot be found in symbol tables or
 * builtins
 * - Walk Template AST to emit similar checks using repository.resolveAcross
 * (template-first)
 * <p>
 * IMPORTANT: When an identifier is a callee in a CallExpr, emit E002 only (not
 * E001).
 */
public class UndefinedNameChecker {

    private final SymbolTableRepository repository;
    private final DiagnosticCollector diagnostics;
    private Set<Object> calleeIdentifiers = new HashSet<>(); // Changed to Object

    public UndefinedNameChecker(SymbolTableRepository repository, DiagnosticCollector diagnostics) {
        this.repository = repository;
        this.diagnostics = diagnostics;
    }

    // ---------
    // FLASK SIDE
    // ---------
    public void checkFlaskScopes(Program program) {
        if (program == null)
            return;

        calleeIdentifiers.clear();

        // First pass: collect all identifiers that are callees
        for (var child : program.getChildren()) {
            if (child instanceof Statement stmt) {
                collectCalleeIdentifiers(stmt);
            }
        }

        // Second pass: check for undefined names and functions
        for (var child : program.getChildren()) {
            if (child instanceof Statement stmt) {
                checkStatementScopes(stmt);
            }
        }
    }

    private void collectCalleeIdentifiers(Statement stmt) {
        if (stmt == null)
            return;

        if (stmt instanceof DecoratedStmt decoratedStmt) {
            Statement target = decoratedStmt.getTarget();
            if (target != null)
                collectCalleeIdentifiers(target);
            return;
        }

        if (stmt instanceof AssignmentStmt asgnStmt) {
            Expression value = asgnStmt.getValue();
            if (value != null)
                collectCalleeInExpr(value);
            return;
        }

        if (stmt instanceof AssignmentChainStmt chainStmt) {
            Expression value = chainStmt.getValue();
            if (value != null)
                collectCalleeInExpr(value);
            return;
        }

        if (stmt instanceof ExpressionStmt exprStmt) {
            collectCalleeInExpr(exprStmt.getExpression());
            return;
        }

        if (stmt instanceof IfStmt ifStmt) {
            collectCalleeInExpr(ifStmt.getCondition());

            if (ifStmt.getThenSuite() != null)
                collectCalleeInSuite(ifStmt.getThenSuite());

            // [Amer] Handle Elif Conditions & Suits
            for (Expression elifCond : ifStmt.getElifConditions()) {
                collectCalleeInExpr(elifCond);
            }
            for (Suite elifSuite : ifStmt.getElifSuites()) {
                if (elifSuite != null)
                    collectCalleeInSuite(elifSuite);
            }
            // [Amer] Handle Elif Conditions & Suits

            if (ifStmt.getElseSuite() != null)
                collectCalleeInSuite(ifStmt.getElseSuite());
            return;
        }

        if (stmt instanceof WhileStmt whileStmt) {
            collectCalleeInExpr(whileStmt.getCondition());
            if (whileStmt.getBody() != null)
                collectCalleeInSuite(whileStmt.getBody());
            return;
        }

        if (stmt instanceof ForStmt forStmt) {
            collectCalleeInExpr(forStmt.getIterable());
            if (forStmt.getBody() != null)
                collectCalleeInSuite(forStmt.getBody());
            return;
        }

        if (stmt instanceof FunctionDefStmt funcStmt) {
            if (funcStmt.getBody() != null)
                collectCalleeInSuite(funcStmt.getBody());
            return;
        }

        if (stmt instanceof ClassDefStmt classStmt) {
            if (classStmt.getBody() != null)
                collectCalleeInSuite(classStmt.getBody());
            return;
        }

        if (stmt instanceof ReturnStmt retStmt) {
            if (retStmt.getValue() != null)
                collectCalleeInExpr(retStmt.getValue());
        }
    }

    private void collectCalleeInSuite(Suite suite) {
        if (suite == null)
            return;
        if (suite instanceof BlockSuite blockSuite) {
            for (var child : blockSuite.getChildren())
                if (child instanceof Statement s)
                    collectCalleeIdentifiers(s);
        } else if (suite instanceof InlineSuite inlineSuite) {
            for (var child : inlineSuite.getChildren())
                if (child instanceof Statement s)
                    collectCalleeIdentifiers(s);
        }
    }

    private void collectCalleeInExpr(Expression expr) {
        if (expr == null)
            return;
        if (expr instanceof CallExpr call) {
            Expression func = call.getFunction();
            if (func instanceof IdentifierExpr id) {
                calleeIdentifiers.add(id);
            }
        }
        for (var child : expr.getChildren()) {
            if (child instanceof Expression sub)
                collectCalleeInExpr(sub);
        }
    }

    private void checkStatementScopes(Statement stmt) {
        if (stmt == null)
            return;

        if (stmt instanceof DecoratedStmt decoratedStmt) {
            Statement target = decoratedStmt.getTarget();
            if (target != null)
                checkStatementScopes(target);
            return;
        }

        if (stmt instanceof AssignmentStmt asgnStmt) {
            Expression value = asgnStmt.getValue();
            if (value != null)
                checkExpressionScopes(value);
            return;
        }

        if (stmt instanceof AssignmentChainStmt chainStmt) {
            Expression value = chainStmt.getValue();
            if (value != null)
                checkExpressionScopes(value);
            return;
        }

        if (stmt instanceof ExpressionStmt exprStmt) {
            checkExpressionScopes(exprStmt.getExpression());
            return;
        }

        if (stmt instanceof IfStmt ifStmt) {
            checkExpressionScopes(ifStmt.getCondition());
            if (ifStmt.getThenSuite() != null)
                checkSuiteScopes(ifStmt.getThenSuite());

            // [Amer] Handle Elif Conditions & Suits
            for (Expression elifCond : ifStmt.getElifConditions()) {
                checkExpressionScopes(elifCond);
            }
            for (Suite elifSuite : ifStmt.getElifSuites()) {
                if (elifSuite != null)
                    checkSuiteScopes(elifSuite);
            }
            // [Amer] Handle Elif Conditions & Suits

            if (ifStmt.getElseSuite() != null)
                checkSuiteScopes(ifStmt.getElseSuite());
            return;
        }

        if (stmt instanceof WhileStmt whileStmt) {
            checkExpressionScopes(whileStmt.getCondition());
            if (whileStmt.getBody() != null)
                checkSuiteScopes(whileStmt.getBody());
            return;
        }

        if (stmt instanceof ForStmt forStmt) {
            checkExpressionScopes(forStmt.getIterable());
            if (forStmt.getBody() != null)
                checkSuiteScopes(forStmt.getBody());
            return;
        }

        if (stmt instanceof FunctionDefStmt funcStmt) {
            if (funcStmt.getBody() != null)
                checkSuiteScopes(funcStmt.getBody());
            return;
        }

        if (stmt instanceof ClassDefStmt classStmt) {
            if (classStmt.getBody() != null)
                checkSuiteScopes(classStmt.getBody());
            return;
        }

        if (stmt instanceof ReturnStmt retStmt) {
            if (retStmt.getValue() != null)
                checkExpressionScopes(retStmt.getValue());
        }
    }

    private void checkSuiteScopes(Suite suite) {
        if (suite == null)
            return;
        if (suite instanceof BlockSuite blockSuite) {
            for (var child : blockSuite.getChildren())
                if (child instanceof Statement s)
                    checkStatementScopes(s);
        } else if (suite instanceof InlineSuite inlineSuite) {
            for (var child : inlineSuite.getChildren())
                if (child instanceof Statement s)
                    checkStatementScopes(s);
        }
    }

    private void checkExpressionScopes(Expression expr) {
        if (expr == null)
            return;

        if (expr instanceof IdentifierExpr idExpr) {
            // Skip if this identifier is a callee (will be checked as E002, not E001)
            if (!calleeIdentifiers.contains(idExpr)) {
                checkIdentifierScopeUse(idExpr);
            }
            return;
        }

        if (expr instanceof CallExpr call) {
            checkCallScopeUse(call);
        }

        for (var child : expr.getChildren()) {
            if (child instanceof Expression sub)
                checkExpressionScopes(sub);
        }
    }

    private void checkCallScopeUse(CallExpr call) {
        if (call == null)
            return;
        Expression func = call.getFunction();
        if (func instanceof IdentifierExpr id) {
            String fname = id.getName();
            if (fname == null || fname.isBlank())
                return;

            boolean exists = false;

            // Prefer deep lookup inside FlaskSymbolTable (nested/local defs)
            if (repository != null && repository.getFlaskGlobal() instanceof FlaskSymbolTable flaskRoot) {
                Optional<Symbol> deep = flaskRoot.findDeepest(fname);
                if (deep.isPresent())
                    exists = true;
            }

            // Fallback: NameResolver.resolve starting from Flask global
            if (!exists && repository != null) {
                Optional<ScopeBinding> binding = NameResolver.resolve(repository.getFlaskGlobal(), fname);
                if (binding.isPresent())
                    exists = true;
            }

            if (!exists) {
                SourceRange range = call.getSourceRange();
                String hint = "Ensure the function is defined or properly imported before calling.";
                diagnostics.reportUndefinedFunction(range, fname, hint);
            }
        }

        // Check arguments
        List<Argument> args = call.getArguments();
        if (args != null) {
            for (Argument a : args) {
                if (a instanceof PositionalArgument pos && pos.getValue() != null) {
                    checkExpressionScopes(pos.getValue());
                }
            }
        }
    }

    private void checkIdentifierScopeUse(IdentifierExpr id) {
        if (id == null)
            return;
        String name = id.getName();
        if (name == null || name.isBlank())
            return;

        boolean found = false;

        if (repository != null && repository.getFlaskGlobal() instanceof FlaskSymbolTable flaskRoot) {
            Optional<Symbol> deep = flaskRoot.findDeepest(name);
            if (deep.isPresent())
                found = true;
        }

        if (!found && repository != null) {
            Optional<ScopeBinding> binding = NameResolver.resolve(repository.getFlaskGlobal(), name);
            if (binding.isPresent())
                found = true;
        }

        if (!found) {
            String hint = "Ensure the variable is defined in the current or parent scope before use.";
            diagnostics.reportUndefinedVariable(id.getSourceRange(), name, hint);
        }
    }
}