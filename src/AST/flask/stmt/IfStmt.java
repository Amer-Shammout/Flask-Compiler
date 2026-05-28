package AST.flask.stmt;

import AST.ASTNode;
import AST.SourceRange;
import AST.flask.expr.Expression;
import AST.flask.suite.Suite;

import java.util.ArrayList;
import java.util.List;

public class IfStmt extends Statement {

    private Expression condition;
    private Suite thenSuite;
    private List<Expression> elifConditions;
    private List<Suite> elifSuites;
    private Suite elseSuite;



    // TODO(George): Add SourceRange to constructor and store it via ASTNode.
    public IfStmt(Expression condition, Suite thenSuite,
                  List<Expression> elifConditions,
                  List<Suite> elifSuites,
                  Suite elseSuite,
                  SourceRange sourceRange) {

        super("IfStmt", sourceRange);
        this.condition = condition;
        this.thenSuite = thenSuite;
        this.elifConditions = elifConditions;
        this.elifSuites = elifSuites;
        this.elseSuite = elseSuite;
    }

    // TODO(George): Ensure every AST node exposes getters for its fields.
    public Expression getCondition() {
        return condition;
    }

    public Suite getThenSuite() {
        return thenSuite;
    }

    public List<Expression> getElifConditions() {
        return elifConditions;
    }

    public List<Suite> getElifSuites() {
        return elifSuites;
    }

    public Suite getElseSuite() {
        return elseSuite;
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();

        children.add(condition);
        children.add(thenSuite);

        for (int i = 0; i < elifConditions.size(); i++) {
            children.add(elifConditions.get(i));
            children.add(elifSuites.get(i));
        }

        if (elseSuite != null) {
            children.add(elseSuite);
        }

        return children;
    }

    // TODO(George): Modify toString for all AST nodes.
    @Override
    public String toString() {
        return "IfStmt " + formatLocation();
    }

}
