package SymbolTable;

import AST.ASTNode;
import AST.Program;
import AST.flask.expr.*;
import AST.flask.literal.ListLiteralExpr;
import AST.flask.literal.LiteralExpr;
import AST.flask.literal.SetLiteralExpr;
import AST.flask.stmt.*;
import AST.flask.suite.BlockSuite;
import AST.flask.suite.InlineSuite;
import AST.flask.suite.Suite;
import semantic.diagnostics.ResolutionStatus;

import java.util.Optional;

/**
 * Two-pass Flask symbol table builder:
 * <ol>
 *   <li>Collect definitions into scoped tables</li>
 *   <li>Resolve identifier references through the scope chain</li>
 * </ol>
 */
public class FlaskSymbolTableBuilder extends SymbolTableBuilder {

    private final FlaskReferenceIndex referenceIndex = new FlaskReferenceIndex();
    /**
     * Indexes the next child scope to enter during pass 2 (matches pass-1 enterScope order).
     */
    private int siblingScopeIndex = 0;

    public FlaskSymbolTableBuilder(SymbolTableRepository repository) {
        super(repository);
        this.activeTable = repository.getFlaskGlobal();
    }

    public FlaskReferenceIndex getReferenceIndex() {
        return referenceIndex;
    }

    @Override
    public ISymbolTable build(Program program) {
        referenceIndex.clear();
        activeTable = repository.getFlaskGlobal();

        if (program != null) {
            for (ASTNode child : program.getChildren()) {
                if (child instanceof Statement statement) {
                    collectDefinitions(statement);
                }
            }

            activeTable = repository.getFlaskGlobal();
            siblingScopeIndex = 0;
            for (ASTNode child : program.getChildren()) {
                if (child instanceof Statement statement) {
                    resolveStatement(statement);
                }
            }
        }

        return repository.getFlaskGlobal();
    }

    @Override
    public void visit(ASTNode node) {
        if (node instanceof Program program) {
            build(program);
            return;
        }
        if (node instanceof Statement statement) {
            collectDefinitions(statement);
            resolveStatement(statement);
        }
    }

    // -------------------------------------------------------------------------
    // Pass 1: definitions
    // -------------------------------------------------------------------------

    private void collectDefinitions(Statement statement) {
        if (statement == null) {
            return;
        }

        switch (statement) {
            case FunctionDefStmt functionDef -> collectFunctionDef(functionDef);
            case ClassDefStmt classDef -> collectClassDef(classDef);
            case DecoratedStmt decorated -> collectDefinitions(decorated.getTarget());
            case AssignmentStmt assignment -> defineAssignmentTarget(assignment.getTarget());
            case AssignmentChainStmt chain -> {
                for (Expression target : chain.getTargets()) {
                    defineAssignmentTarget(target);
                }
            }
            case FromImportStmt importStmt -> {
                for (String importedName : importStmt.getNames()) {
                    recordDefinition(importedName, importStmt, activeTable, SymbolKind.IMPORT);
                }
            }
            case ImportStmt importStmt -> {
                // Python binds only the top-level package for dotted imports,
                // e.g. `import os.path` makes `os` (not `os.path`) available in scope.
                for (String moduleName : importStmt.getModules()) {
                    int dot = moduleName.indexOf('.');
                    String boundName = dot >= 0 ? moduleName.substring(0, dot) : moduleName;
                    recordDefinition(boundName, importStmt, activeTable, SymbolKind.IMPORT);
                }
            }
            case IfStmt ifStmt -> {
                collectSuite(ifStmt.getThenSuite());
                for (Suite elifSuite : ifStmt.getElifSuites()) {
                    collectSuite(elifSuite);
                }
                collectSuite(ifStmt.getElseSuite());
            }
            case ForStmt forStmt -> {
                ISymbolTable previous = activeTable;
                activeTable = activeTable.enterScope("for");
                siblingScopeIndex = 0;
                defineAssignmentTarget(forStmt.getIterator());
                collectSuite(forStmt.getBody());
                activeTable = previous;
            }
            case WhileStmt whileStmt -> collectSuite(whileStmt.getBody());
            default -> {
            }
        }
    }

    private void collectSuite(Suite suite) {
        if (suite == null) {
            return;
        }
        if (suite instanceof BlockSuite block) {
            for (Statement stmt : block.getStatements()) {
                collectDefinitions(stmt);
            }
        } else if (suite instanceof InlineSuite inline) {
            collectDefinitions(inline.getStatement());
        }
    }

    private void collectFunctionDef(FunctionDefStmt functionDef) {
        recordDefinition(functionDef.getName(), functionDef, activeTable, SymbolKind.FUNCTION);

        ISymbolTable previous = activeTable;
        activeTable = activeTable.enterScope("function:" + functionDef.getName());
        siblingScopeIndex = 0;

        for (String parameter : functionDef.getParameters()) {
            // Parameters are bound at function entry: use the def header as their position.
            recordDefinition(parameter, functionDef, activeTable, SymbolKind.PARAMETER);
        }

        collectSuite(functionDef.getBody());
        activeTable = previous;
    }

    private void collectClassDef(ClassDefStmt classDef) {
        recordDefinition(classDef.getName(), classDef, activeTable, SymbolKind.CLASS);

        ISymbolTable previous = activeTable;
        activeTable = activeTable.enterScope("class:" + classDef.getName());
        siblingScopeIndex = 0;

        collectSuite(classDef.getBody());
        activeTable = previous;
    }

    private void defineAssignmentTarget(Expression target) {
        if (target instanceof IdentifierExpr identifier) {
            recordDefinition(identifier.getName(), identifier, activeTable, SymbolKind.VARIABLE);
        }
    }

    /**
     * Record a definition site.
     *
     * @param source AST node the definition is anchored to. For assignments this is the
     *               {@link IdentifierExpr} target; for defs/classes/imports it is the
     *               statement node, whose range starts at the keyword. Used for the
     *               ordering that {@code ScopeCheckAnalyzer} relies on to emit E202.
     */
    private void recordDefinition(String name, ASTNode source, ISymbolTable table, SymbolKind kind) {
        if (name == null || name.isBlank()) {
            return;
        }

        String origin = originFor(table);
        Symbol symbol = new Symbol(name, kind, origin);
        table.define(symbol);

        Optional<ScopeBinding> outer = findOuterBinding(table, name);
        ResolutionStatus status = outer.isPresent() ? ResolutionStatus.SHADOWED : ResolutionStatus.RESOLVED;

        referenceIndex.record(new SymbolReference(
                name,
                SymbolUseKind.DEFINITION,
                source != null ? source.getSourceRange() : null,
                source,
                NameResolver.scopeName(table),
                status,
                symbol,
                NameResolver.scopeName(table),
                table
        ));
    }

    private Optional<ScopeBinding> findOuterBinding(ISymbolTable table, String name) {
        ISymbolTable outer = NameResolver.parentOf(table);
        if (outer == null) {
            return Optional.empty();
        }
        return NameResolver.resolve(outer, name);
    }

    // -------------------------------------------------------------------------
    // Pass 2: references
    // -------------------------------------------------------------------------

    private void resolveStatement(Statement statement) {
        if (statement == null) {
            return;
        }

        switch (statement) {
            case FunctionDefStmt functionDef -> resolveFunctionDef(functionDef);
            case ClassDefStmt classDef -> resolveClassDef(classDef);
            case DecoratedStmt decorated -> resolveStatement(decorated.getTarget());
            case AssignmentStmt assignment -> resolveExpression(assignment.getValue());
            case AssignmentChainStmt chain -> resolveExpression(chain.getValue());
            case ExpressionStmt expressionStmt -> resolveExpression(expressionStmt.getExpression());
            case ReturnStmt returnStmt -> resolveExpression(returnStmt.getValue());
            case DelStmt delStmt -> {
                for (Expression target : delStmt.getTargets()) {
                    resolveExpression(target);
                }
            }
            case IfStmt ifStmt -> {
                resolveExpression(ifStmt.getCondition());
                for (Expression elifCondition : ifStmt.getElifConditions()) {
                    resolveExpression(elifCondition);
                }
                resolveSuite(ifStmt.getThenSuite());
                for (Suite elifSuite : ifStmt.getElifSuites()) {
                    resolveSuite(elifSuite);
                }
                resolveSuite(ifStmt.getElseSuite());
            }
            case ForStmt forStmt -> {
                resolveExpression(forStmt.getIterable());
                ISymbolTable previous = activeTable;
                activeTable = enterSiblingScope();
                siblingScopeIndex = 0;
                resolveSuite(forStmt.getBody());
                activeTable = previous;
            }
            case WhileStmt whileStmt -> {
                resolveExpression(whileStmt.getCondition());
                resolveSuite(whileStmt.getBody());
            }
            default -> {
            }
        }
    }

    private void resolveSuite(Suite suite) {
        if (suite == null) {
            return;
        }
        if (suite instanceof BlockSuite block) {
            for (Statement stmt : block.getStatements()) {
                resolveStatement(stmt);
            }
        } else if (suite instanceof InlineSuite inline) {
            resolveStatement(inline.getStatement());
        }
    }

    private void resolveFunctionDef(FunctionDefStmt functionDef) {
        ISymbolTable functionScope = findNamedChildScope("function:" + functionDef.getName());
        if (functionScope == null) {
            return;
        }
        ISymbolTable previous = activeTable;
        activeTable = functionScope;
        siblingScopeIndex = 0;
        resolveSuite(functionDef.getBody());
        activeTable = previous;
    }

    private void resolveClassDef(ClassDefStmt classDef) {
        ISymbolTable classScope = findNamedChildScope("class:" + classDef.getName());
        if (classScope == null) {
            return;
        }
        ISymbolTable previous = activeTable;
        activeTable = classScope;
        siblingScopeIndex = 0;
        resolveSuite(classDef.getBody());
        activeTable = previous;
    }

    private ISymbolTable findNamedChildScope(String scopeName) {
        if (!(activeTable instanceof AbstractSymbolTable parent)) {
            return null;
        }
        for (ISymbolTable child : parent.children) {
            if (scopeName.equals(NameResolver.scopeName(child))) {
                return child;
            }
        }
        return null;
    }

    /**
     * Re-enters the next child scope created during pass 1 at this level.
     */
    private ISymbolTable enterSiblingScope() {
        if (!(activeTable instanceof AbstractSymbolTable parent)) {
            return activeTable;
        }
        if (siblingScopeIndex >= parent.children.size()) {
            return activeTable;
        }
        return parent.children.get(siblingScopeIndex++);
    }

    /**
     * Walk an expression tree and resolve every {@link IdentifierExpr} as a reference.
     */
    private void resolveExpression(Expression expression) {
        if (expression == null || expression instanceof LiteralExpr) {
            return;
        }

        switch (expression) {
            case IdentifierExpr identifier -> resolveIdentifierReference(identifier);
            case BinaryExpr binary -> {
                resolveExpression(binary.getLeft());
                resolveExpression(binary.getRight());
            }
            case UnaryExpr unary -> resolveExpression(unary.getExpression());
            case NotExpr notExpr -> resolveExpression(notExpr.getExpr());
            case CompareExpr compare -> {
                resolveExpression(compare.getLeft());
                for (Expression right : compare.getRights()) {
                    resolveExpression(right);
                }
            }
            case CallExpr call -> {
                resolveExpression(call.getFunction());
                for (Argument argument : call.getArguments()) {
                    if (argument instanceof KeywordArgument keyword) {
                        resolveExpression(keyword.getValue());
                    } else if (argument instanceof PositionalArgument positional) {
                        resolveExpression(positional.getValue());
                    }
                }
            }
            case AttributeExpr attribute -> resolveExpression(attribute.getBase());
            case IndexExpr index -> {
                resolveExpression(index.getBase());
                resolveExpression(index.getIndex());
            }
            case ListLiteralExpr list -> {
                for (Expression element : list.getElements()) {
                    resolveExpression(element);
                }
            }
            case SetLiteralExpr set -> {
                for (Expression element : set.getElements()) {
                    resolveExpression(element);
                }
            }
            case LambdaExpr lambda -> resolveLambda(lambda);
            default -> {
                for (ASTNode child : expression.getChildren()) {
                    if (child instanceof Expression childExpr) {
                        resolveExpression(childExpr);
                    }
                }
            }
        }
    }

    private void resolveLambda(LambdaExpr lambda) {
        ISymbolTable previous = activeTable;
        activeTable = activeTable.enterScope("lambda");

        for (String parameter : lambda.getParams()) {
            activeTable.define(new Symbol(parameter, SymbolKind.PARAMETER, originFor(activeTable)));
        }

        resolveExpression(lambda.getBody());
        activeTable = previous;
    }

    private void resolveIdentifierReference(IdentifierExpr identifier) {
        String name = identifier.getName();
        Optional<ScopeBinding> binding = NameResolver.resolve(activeTable, name);
        ResolutionStatus status = NameResolver.toStatus(binding);

        referenceIndex.record(new SymbolReference(
                name,
                SymbolUseKind.REFERENCE,
                identifier.getSourceRange(),
                identifier,
                NameResolver.scopeName(activeTable),
                status,
                binding.map(ScopeBinding::getSymbol).orElse(null),
                binding.map(b -> NameResolver.scopeName(b.getDefiningScope())).orElse(null),
                activeTable
        ));
    }

    private String originFor(ISymbolTable table) {
        if (table instanceof FlaskSymbolTable flaskTable) {
            return flaskTable.getSourceFile();
        }
        if (repository.getFlaskGlobal() instanceof FlaskSymbolTable flaskTable) {
            return flaskTable.getSourceFile();
        }
        return null;
    }
}
