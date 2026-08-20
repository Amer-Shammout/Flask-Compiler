package generator.context;

import AST.ASTNode;
import AST.Program;
import AST.flask.expr.Expression;
import AST.flask.expr.IdentifierExpr;
import AST.flask.stmt.AssignmentStmt;

/**
 * Walks a Flask {@link Program} AST and extracts global variable assignments
 * into {@link ContextData} by evaluating their right-hand sides.
 * <p>
 * Example from {@code app.py}:
 * <pre>
 *   products = [
 *       [1, "Laptop", 1200.0, True],
 *       [2, "Headphones", 250.0, False]
 *   ]
 * </pre>
 * becomes {@code ContextData} with key {@code "products"} → nested LIST values.
 * <p>
 * Does NOT use the Symbol Table (generation rule from the course announcement).
 */
public final class FlaskDataExtractor {

    private FlaskDataExtractor() {
    }

    /**
     * Collect all top-level {@code name = literal...} assignments.
     */
    public static ContextData extractGlobals(Program program) {
        ContextData globals = new ContextData();
        if (program == null) {
            return globals;
        }

        for (ASTNode child : program.getChildren()) {
            if (!(child instanceof AssignmentStmt assignment)) {
                continue;
            }
            Expression target = assignment.getTarget();
            if (!(target instanceof IdentifierExpr id)) {
                continue;
            }
            // Step 2: only keep assignments whose RHS is literals/lists
            // (skip things like app = Flask(__name__))
            if (!ExpressionEvaluator.isSupported(assignment.getValue())) {
                continue;
            }
            RuntimeValue value = ExpressionEvaluator.evaluate(assignment.getValue());
            globals.put(id.getName(), value);
        }

        return globals;
    }

    /**
     * Convenience: extract one named global (e.g. {@code "products"}).
     * Returns {@link RuntimeValue#none()} if missing.
     */
    public static RuntimeValue extractGlobal(Program program, String name) {
        return extractGlobals(program).get(name);
    }
}
