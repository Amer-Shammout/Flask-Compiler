package AST.flask.visitor;

import AST.ASTNode;
import AST.Program;
import AST.SourcePosition;
import AST.SourceRange;
import AST.flask.expr.*;
import AST.flask.stmt.Statement;
import AST.flask.literal.*;
import AST.flask.stmt.*;
import AST.flask.suite.*;
import antlr.FlaskParser;
import antlr.FlaskParserBaseVisitor;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class FlaskVisitor extends FlaskParserBaseVisitor<ASTNode> {
    // TODO(George): Build SourceRange from ctx.getStart()/getStop() and pass it into all AST nodes.
    @Override
    public ASTNode visitProg(FlaskParser.ProgContext ctx) {
        List<Statement> list = new ArrayList<>();
        for (var s : ctx.stmt()) {
            Statement st = (Statement) visit(s);
            if (st != null) list.add(st);
        }
        return new Program(list, range(ctx));
    }

    /* =========================
       STATEMENTS
     ========================= */

    @Override
    public ASTNode visitSimpleStmtLine(FlaskParser.SimpleStmtLineContext ctx) {
        ASTNode inner = visit(ctx.simple_stmt());

        if (inner instanceof Expression) {
            Expression expr = (Expression) inner;
            SourceRange r = expr.getSourceRange() != null ? expr.getSourceRange() : range(ctx);
            return new ExpressionStmt(expr, r);
        }

        return inner;
    }


    @Override
    public ASTNode visitCompoundStmt(FlaskParser.CompoundStmtContext ctx) {
        return visit(ctx.getChild(0));  // def, if, while, for...
    }

    @Override
    public ASTNode visitEmptyLine(FlaskParser.EmptyLineContext ctx) {
        return null;
    }

    @Override
    public ASTNode visitAssign_target(FlaskParser.Assign_targetContext ctx) {
        SourceRange sourceRange = range(ctx);

        // Base: the first IDENTIFIER token
        Expression expr = new IdentifierExpr(ctx.IDENTIFIER(0).getText(), sourceRange);

        // Walk the children: IDENTIFIER (DOT IDENTIFIER | LBRACK expr RBRACK)*
        // child(0) = IDENTIFIER, so start from 1
        int i = 1;
        while (i < ctx.getChildCount()) {

            org.antlr.v4.runtime.tree.ParseTree child = ctx.getChild(i);
            String text = child.getText();

            // Case 1: .name
            if (".".equals(text)) {
                org.antlr.v4.runtime.tree.ParseTree nameNode = ctx.getChild(i + 1);
                String attrName = nameNode.getText();
                expr = new AttributeExpr(expr, attrName, sourceRange);
                i += 2; // skip '.' and name
            }
            // Case 2: [ expr ]
            else if ("[".equals(text)) {
                FlaskParser.ExprContext ectx =
                        (FlaskParser.ExprContext) ctx.getChild(i + 1);
                Expression indexExpr = (Expression) visit(ectx);
                expr = new IndexExpr(expr, indexExpr, sourceRange);
                i += 3; // skip '[', expr, ']'
            } else {
                i++;
            }
        }

        return expr;
    }



    @Override
    public ASTNode visitAssignment(FlaskParser.AssignmentContext ctx) {

        // Handle simple x = expr
        if (ctx.assign_target().size() == 1)
        {
            Expression lhs = (Expression) visit(ctx.assign_target(0));
            Expression rhs = (Expression) visit(ctx.expr());
            return new AssignmentStmt(lhs, rhs, range(ctx));
        }

        // Otherwise: chained assignment a = b = c = value
        List<FlaskParser.Assign_targetContext> targets = ctx.assign_target();

        List<Expression> lhsList = new ArrayList<>();
        for (var t : targets)
            lhsList.add((Expression) visit(t));

        Expression rhs = (Expression) visit(ctx.expr());

        return new AssignmentChainStmt(lhsList, rhs, range(ctx));
    }


    @Override
    public ASTNode visitReturn_stmt(FlaskParser.Return_stmtContext ctx) {
        return new ReturnStmt((Expression) visit(ctx.expr()), range(ctx));
    }


    @Override public ASTNode visitPass_stmt(FlaskParser.Pass_stmtContext ctx)
    { return new PassStmt(range(ctx)); }

    @Override public ASTNode visitBreak_stmt(FlaskParser.Break_stmtContext ctx)
    { return new BreakStmt(range(ctx)); }

    @Override public ASTNode visitContinue_stmt(FlaskParser.Continue_stmtContext ctx)
    { return new ContinueStmt(range(ctx)); }

    @Override
    public ASTNode visitDel_stmt(FlaskParser.Del_stmtContext ctx) {

        List<Expression> list = new ArrayList<>();

        for (var e : ctx.expr())
            list.add((Expression) visit(e));

        return new DelStmt(list, range(ctx));
    }

    @Override
    public ASTNode visitGlobal_stmt(FlaskParser.Global_stmtContext ctx) {

        List<String> names =
                ctx.IDENTIFIER().stream().map(t -> t.getText()).toList();

        return new GlobalStmt(names, range(ctx));
    }

    @Override
    public ASTNode visitFromImport(FlaskParser.FromImportContext ctx) {

        String module = ctx.dotted_name().getText();

        List<String> names =
                ctx.IDENTIFIER().stream().map(id -> id.getText()).toList();

        return new FromImportStmt(module, names, range(ctx));
    }

    @Override
    public ASTNode visitIf_stmt(FlaskParser.If_stmtContext ctx) {

        Expression cond = (Expression) visit(ctx.expr(0));
        Suite thenSuite = (Suite) visit(ctx.suite(0));

        List<Expression> elifConds = new ArrayList<>();
        List<Suite> elifSuites = new ArrayList<>();

        int elifCount = ctx.expr().size() - 1;

        for (int i = 1; i <= elifCount; i++) {
            elifConds.add((Expression) visit(ctx.expr(i)));
            elifSuites.add((Suite) visit(ctx.suite(i)));
        }

        Suite elseSuite = null;
        if (ctx.ELSE() != null) {
            elseSuite = (Suite) visit(ctx.suite(ctx.suite().size() - 1));
        }

        return new IfStmt(cond, thenSuite, elifConds, elifSuites, elseSuite,
            range(ctx));
    }

    @Override
    public ASTNode visitWhile_stmt(FlaskParser.While_stmtContext ctx) {
        Expression cond = (Expression) visit(ctx.expr());
        Suite body = (Suite) visit(ctx.suite());
        return new WhileStmt(cond, body, range(ctx));
    }

    @Override
    public ASTNode visitFor_stmt(FlaskParser.For_stmtContext ctx) {

        IdentifierExpr iterator =
                new IdentifierExpr(ctx.IDENTIFIER().getText(), range(ctx));

        Expression iterable = (Expression) visit(ctx.expr());
        Suite body = (Suite) visit(ctx.suite());

        return new ForStmt(iterator, iterable, body, range(ctx));
    }

    @Override
    public ASTNode visitFunc_def(FlaskParser.Func_defContext ctx) {

        String name = ctx.IDENTIFIER().getText();

        List<String> params = new ArrayList<>();
        if (ctx.parameters() != null) {
            ctx.parameters().IDENTIFIER().forEach(id -> params.add(id.getText()));
        }

        Suite body = (Suite) visit(ctx.suite());

        return new FunctionDefStmt(name, params, body, range(ctx));
    }


    @Override
    public ASTNode visitClass_def(FlaskParser.Class_defContext ctx) {

        String name = ctx.IDENTIFIER(0).getText();

        String parent = null;
        if (ctx.IDENTIFIER().size() > 1) {
            parent = ctx.IDENTIFIER(1).getText();
        }

        Suite body = (Suite) visit(ctx.suite());

        return new ClassDefStmt(name, parent, body, range(ctx));
    }

    @Override
    public ASTNode visitDecorator(FlaskParser.DecoratorContext ctx) {

        Expression expr = (Expression) visit(ctx.primary());
        return new Decorator(expr, range(ctx));
    }

    @Override
    public ASTNode visitDecorated(FlaskParser.DecoratedContext ctx) {

        List<Decorator> list = new ArrayList<>();
        for (FlaskParser.DecoratorContext d : ctx.decorator()) {
            list.add((Decorator) visit(d));
        }

        Statement target = (Statement) visit(ctx.func_def() != null
                ? ctx.func_def()
                : ctx.class_def());

        return new DecoratedStmt(list, target, range(ctx));
    }


    /* =========================
       SUITES
     ========================= */

    @Override
    public ASTNode visitInlineSuite(FlaskParser.InlineSuiteContext ctx) {
        Statement st = (Statement) visit(ctx.simple_stmt());
        return new InlineSuite(st, range(ctx));
    }


    @Override
    public ASTNode visitBlockSuite(FlaskParser.BlockSuiteContext ctx) {
        List<Statement> list = new ArrayList<>();
        for (FlaskParser.StmtContext s : ctx.stmt()) {
            list.add((Statement) visit(s));
        }
        return new BlockSuite(list, range(ctx));
    }


    /* =========================
       EXPRESSIONS
     ========================= */

    @Override
    public ASTNode visitExprRoot(FlaskParser.ExprRootContext ctx) {
        return visit(ctx.lambda_expr());
    }

    @Override
    public ASTNode visitLambdaExpr(FlaskParser.LambdaExprContext ctx) {

        List<String> params = new ArrayList<>();

        if (ctx.lambda_params() != null) {
            for (var id : ctx.lambda_params().IDENTIFIER()) {
                params.add(id.getText());
            }
        }

        Expression body = (Expression) visit(ctx.expr());

        return new LambdaExpr(params, body, range(ctx));
    }

    @Override
    public ASTNode visitToOrExpr(FlaskParser.ToOrExprContext ctx) {
        return visit(ctx.or_expr());
    }

    @Override
    public ASTNode visitOrExpr(FlaskParser.OrExprContext ctx) {
        Expression result = (Expression) visit(ctx.and_expr(0));

        for (int i = 1; i < ctx.and_expr().size(); i++) {
            Expression rhs = (Expression) visit(ctx.and_expr(i));
            result = new BinaryExpr(result, "or", rhs, range(ctx));
        }

        return result;
    }

    @Override
    public ASTNode visitAndExpr(FlaskParser.AndExprContext ctx) {
        Expression result = (Expression) visit(ctx.not_expr(0));

        for (int i = 1; i < ctx.not_expr().size(); i++) {
            Expression rhs = (Expression) visit(ctx.not_expr(i));
            result = new BinaryExpr(result, "and", rhs, range(ctx));
        }

        return result;
    }

    @Override
    public ASTNode visitNotExpr(FlaskParser.NotExprContext ctx) {
        Expression sub = (Expression) visit(ctx.not_expr());
        return new NotExpr(sub, range(ctx));
    }

    @Override
    public ASTNode visitCompareExpr(FlaskParser.CompareExprContext ctx) {

        if (ctx.comp_tail().isEmpty()) {
            return visit(ctx.arith_expr());
        }

        Expression left = (Expression) visit(ctx.arith_expr());

        List<String> ops = new ArrayList<>();
        List<Expression> rights = new ArrayList<>();

        for (FlaskParser.Comp_tailContext t : ctx.comp_tail()) {

            if (t instanceof FlaskParser.RelOpContext rel) {
                ops.add(rel.getChild(0).getText());
                rights.add((Expression) visit(rel.arith_expr()));
            }
            else if (t instanceof FlaskParser.IsOpContext isOp) {
                String op = (isOp.NOT() != null) ? "is not" : "is";
                ops.add(op);
                rights.add((Expression) visit(isOp.arith_expr()));
            }
        }

        return new CompareExpr(left, ops, rights, range(ctx));
    }


    @Override
    public ASTNode visitAddSubExpr(FlaskParser.AddSubExprContext ctx) {

        Expression result = (Expression) visit(ctx.term(0));

        for (int i = 1; i < ctx.term().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            Expression rhs = (Expression) visit(ctx.term(i));
            result = new BinaryExpr(result, op, rhs, range(ctx));
        }

        return result;
    }


    @Override
    public ASTNode visitMulDivExpr(FlaskParser.MulDivExprContext ctx) {

        Expression result = (Expression) visit(ctx.power(0));

        for (int i = 1; i < ctx.power().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            Expression rhs = (Expression) visit(ctx.power(i));
            result = new BinaryExpr(result, op, rhs, range(ctx));
        }

        return result;
    }

    @Override
    public ASTNode visitPowExpr(FlaskParser.PowExprContext ctx) {

        Expression base = (Expression) visit(ctx.unary());

        if (ctx.power() != null) {
            Expression exponent = (Expression) visit(ctx.power());
            return new BinaryExpr(base, "**", exponent, range(ctx));
        }

        return base;
    }

    @Override
    public ASTNode visitUnaryPMExpr(FlaskParser.UnaryPMExprContext ctx) {

        String op = ctx.getChild(0).getText();
        Expression expr = (Expression) visit(ctx.getChild(1));

        return new UnaryExpr(op, expr, range(ctx));
    }

    @Override
    public ASTNode visitPrimaryExpr(FlaskParser.PrimaryExprContext ctx) {
        return visit(ctx.primary());
    }


    @Override
    public ASTNode visitPrimaryRoot(FlaskParser.PrimaryRootContext ctx) {

        SourceRange sourceRange = range(ctx);

        // 1. Start with atom (base)
        Expression base = (Expression) visit(ctx.atom());

        // 2. Apply all trailers in sequence
        for (FlaskParser.TrailerContext t : ctx.trailer()) {

            if (t instanceof FlaskParser.CallTrailerContext call) {

                List<Argument> args = new ArrayList<>();

                if (call.arguments() != null) {
                    for (FlaskParser.ArgumentContext argCtx : call.arguments().argument()) {
                        if (argCtx instanceof FlaskParser.KeywordArgumentContext keyword) {
                            String name = keyword.IDENTIFIER().getText();
                            Expression value = (Expression) visit(keyword.expr());
                            args.add(new KeywordArgument(name, value, range(argCtx)));
                        } else if (argCtx instanceof FlaskParser.PositionalArgumentContext positional) {
                            Expression value = (Expression) visit(positional.expr());
                            args.add(new PositionalArgument(value, range(argCtx)));
                        }
                    }
                }

                base = new CallExpr(base, args, sourceRange);
            }
            else if (t instanceof FlaskParser.AttrTrailerContext attr) {

                String name = attr.IDENTIFIER().getText();
                base = new AttributeExpr(base, name, sourceRange);
            }
            else if (t instanceof FlaskParser.IndexTrailerContext idx) {

                Expression indexExpr = (Expression) visit(idx.expr());
                base = new IndexExpr(base, indexExpr, sourceRange);
            }
        }

        return base;
    }

    /* =========================
       ATOMS & LITERALS
     ========================= */

    @Override
    public ASTNode visitLiteralAtom(FlaskParser.LiteralAtomContext ctx) {
        return visit(ctx.literal());
    }


    @Override
    public ASTNode visitIdAtom(FlaskParser.IdAtomContext ctx) {
        return new IdentifierExpr(ctx.IDENTIFIER().getText(), range(ctx));
    }

    @Override
    public ASTNode visitIntNumber(FlaskParser.IntNumberContext ctx) {
        return new NumberLiteralExpr(ctx.getText(), range(ctx));
    }

    @Override
    public ASTNode visitFloatNumber(FlaskParser.FloatNumberContext ctx) {
        return new NumberLiteralExpr(ctx.getText(), range(ctx));
    }

    @Override
    public ASTNode visitSciNumber(FlaskParser.SciNumberContext ctx) {
        return new NumberLiteralExpr(ctx.getText(), range(ctx));
    }

    @Override
    public ASTNode visitTripleString(FlaskParser.TripleStringContext ctx) {
        return new StringLiteralExpr(ctx.getText(), range(ctx));
    }

    @Override
    public ASTNode visitSingleString(FlaskParser.SingleStringContext ctx) {
        return new StringLiteralExpr(ctx.getText(), range(ctx));
    }

    @Override
    public ASTNode visitDoubleString(FlaskParser.DoubleStringContext ctx) {
        return new StringLiteralExpr(ctx.getText(), range(ctx));
    }

    @Override
    public ASTNode visitTrueLiteral(FlaskParser.TrueLiteralContext ctx) {
        return new BooleanLiteralExpr(true, range(ctx));
    }

    @Override
    public ASTNode visitFalseLiteral(FlaskParser.FalseLiteralContext ctx) {
        return new BooleanLiteralExpr(false, range(ctx));
    }

    @Override
    public ASTNode visitNoneLiteral(FlaskParser.NoneLiteralContext ctx) {
        return new NoneLiteralExpr(range(ctx));
    }

    @Override
    public ASTNode visitSet_literal(FlaskParser.Set_literalContext ctx)
    {

        List<Expression> elems = new ArrayList<>();

        for (FlaskParser.ExprContext e : ctx.expr()) {
            elems.add((Expression) visit(e));
        }

        return new SetLiteralExpr(elems, range(ctx));
    }

    @Override
    public ASTNode visitList_literal(FlaskParser.List_literalContext ctx)
    {

        List<Expression> elems = new ArrayList<>();

        for (FlaskParser.ExprContext e : ctx.expr()) {
            elems.add((Expression) visit(e));
        }

        return new ListLiteralExpr(elems, range(ctx));
    }

    private SourceRange range(ParserRuleContext ctx) {
        if (ctx == null) {
            return null;
        }

        Token start = ctx.getStart();
        Token stop = ctx.getStop() != null ? ctx.getStop() : start;

        int startColumn = start.getCharPositionInLine() + 1;
        SourcePosition startPosition = new SourcePosition(start.getLine(), startColumn);

        int endLine = stop.getLine();
        int endColumn = stop.getCharPositionInLine() + 1;
        String stopText = stop.getText();
        if (stopText != null && !stopText.isEmpty()) {
            int lastNewLine = Math.max(stopText.lastIndexOf('\n'), stopText.lastIndexOf('\r'));
            if (lastNewLine >= 0) {
                endLine = stop.getLine();
                endColumn = stopText.length() - lastNewLine;
            } else {
                endColumn += stopText.length() - 1;
            }
        }

        return new SourceRange(startPosition, new SourcePosition(endLine, endColumn));
    }
}
