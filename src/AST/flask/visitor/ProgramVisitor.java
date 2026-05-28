package AST.flask.visitor;

import AST.ASTNode;
import AST.Program;
import AST.SourcePosition;
import AST.SourceRange;
import AST.flask.stmt.Statement;
import antlr.FlaskParser;
import antlr.FlaskParserBaseVisitor;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class ProgramVisitor extends FlaskParserBaseVisitor<Program> {
// TODO(George): Build SourceRange from ctx.getStart()/getStop() and pass it into Program.

    @Override
    public Program visitProg(FlaskParser.ProgContext ctx) {

        FlaskVisitor flaskVisitor = new FlaskVisitor();
        List<Statement> statements = new ArrayList<>();

        for (FlaskParser.StmtContext stmtCtx : ctx.stmt()) {
            ASTNode node = flaskVisitor.visit(stmtCtx);

            if (node instanceof Statement) {
                statements.add((Statement) node);
            }
        }
        System.out.println("Number of statements = " + statements.size());

        return new Program(statements, range(ctx));
    }

    private SourceRange range(ParserRuleContext ctx) {
        if (ctx == null) {
            return null;
        }

        Token start = ctx.getStart();
        Token stop = ctx.getStop() != null ? ctx.getStop() : start;

        int startColumn = start.getCharPositionInLine() + 1;
        SourcePosition startPosition = new SourcePosition(start.getLine(), startColumn);

        int endColumn = stop.getCharPositionInLine() + 1;
        String stopText = stop.getText();
        if (stopText != null && !stopText.isEmpty() && !stopText.contains("\n") && !stopText.contains("\r")) {
            endColumn += stopText.length() - 1;
        }

        SourcePosition endPosition = new SourcePosition(stop.getLine(), endColumn);
        return new SourceRange(startPosition, endPosition);
    }
}

