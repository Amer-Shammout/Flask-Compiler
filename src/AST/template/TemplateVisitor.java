package AST.template;
import AST.ASTNode;
import AST.template.css.*;
import AST.template.html.*;
import AST.template.jinja.expr.*;

import java.util.ArrayList;
import java.util.List;
import AST.template.jinja.*;
import AST.template.jinja.stmt.*;
import antlr.TemplateParser;
import antlr.TemplateParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;


public class TemplateVisitor extends TemplateParserBaseVisitor<ASTNode> {

    @Override
    public ASTNode visitTemplateRoot(TemplateParser.TemplateRootContext ctx) {
        return visit(ctx.template_content());
    }

    @Override
    public ASTNode visitTemplateContent(TemplateParser.TemplateContentContext ctx) {

        List<ASTNode> children = new ArrayList<>();

        for (var n : ctx.template_node()) {
            ASTNode node = visit(n);   // may be HTML, Jinja, CSS
            if (node != null) children.add(node);
        }

        return new HtmlDocument(children, ctx.start.getLine());
    }



    /* =====================================================
       TEMPLATE NODE DISPATCH (HTML, JINJA, TEXT)
       ===================================================== */

    @Override
    public ASTNode visitHtmlNode(TemplateParser.HtmlNodeContext ctx) {
        return visit(ctx.html_element());
    }

    @Override
    public ASTNode visitTextNode(TemplateParser.TextNodeContext ctx) {
        String raw = ctx.HTML_TEXT().getText();
        return new HtmlText(raw, ctx.start.getLine());
    }

    /* =====================================================
       HTML ELEMENT TYPES
       ===================================================== */

    @Override
    public ASTNode visitStyleHtmlElement(TemplateParser.StyleHtmlElementContext ctx) {
        return visit(ctx.style_element());
    }

    @Override
    public ASTNode visitNormalHtmlElement(TemplateParser.NormalHtmlElementContext ctx) {
        return visit(ctx.normal_element());
    }

    @Override
    public ASTNode visitSelfClosingHtmlElement(TemplateParser.SelfClosingHtmlElementContext ctx) {
        return visit(ctx.self_closing_element());
    }

    @Override
    public ASTNode visitVoidHtmlElement(TemplateParser.VoidHtmlElementContext ctx) {
        return visit(ctx.void_element());
    }


    @Override
    public ASTNode visitNormalElement(TemplateParser.NormalElementContext ctx) {

        TemplateParser.OpenTagContext openCtx =
                (TemplateParser.OpenTagContext) ctx.open_tag();

        String tagName = openCtx.HTML_TAG_NAME().getText();

        List<HtmlAttribute> attrs = new ArrayList<>();
        for (TemplateParser.AttributeContext aCtx : openCtx.attribute()) {
            attrs.add((HtmlAttribute) visit(aCtx));
        }

        HtmlDocument contentDoc = (HtmlDocument) visit(ctx.html_content());
        List<ASTNode> children = contentDoc.getChildrenNodes();

        return new HtmlNormalElement(
                tagName,
                attrs,
                children,
                ctx.start.getLine()
        );
    }

    @Override
    public ASTNode visitSelfClosingElement(TemplateParser.SelfClosingElementContext ctx) {

        String tagName =
                (ctx.HTML_TAG_NAME() != null)
                        ? ctx.HTML_TAG_NAME().getText()
                        : ctx.HTML_VOID_TAG().getText();

        List<HtmlAttribute> attrs = new ArrayList<>();
        for (TemplateParser.AttributeContext aCtx : ctx.attribute()) {
            attrs.add((HtmlAttribute) visit(aCtx));
        }

        return new HtmlSelfClosingElement(
                tagName,
                attrs,
                ctx.start.getLine()
        );
    }

    @Override
    public ASTNode visitVoidElement(TemplateParser.VoidElementContext ctx) {

        String tagName =
                (ctx.HTML_TAG_NAME() != null)
                        ? ctx.HTML_TAG_NAME().getText()
                        : ctx.HTML_VOID_TAG().getText();


        List<HtmlAttribute> attrs = new ArrayList<>();
        for (TemplateParser.AttributeContext aCtx : ctx.attribute()) {
            attrs.add((HtmlAttribute) visit(aCtx));
        }

        return new HtmlVoidElement(
                tagName,
                attrs,
                ctx.start.getLine()
        );
    }

    @Override
    public ASTNode visitHtmlContent(TemplateParser.HtmlContentContext ctx) {

        List<ASTNode> children = new ArrayList<>();

        for (TemplateParser.Template_nodeContext tn : ctx.template_node()) {
            ASTNode node = visit(tn);   // HtmlNode OR JinjaNode
            if (node != null) children.add(node);
        }

        return new HtmlDocument(children, ctx.start.getLine());
    }

    @Override
    public ASTNode visitHtmlAttribute(TemplateParser.HtmlAttributeContext ctx) {

        String name = ctx.HTML_ATTR_NAME().getText();
        String value = ctx.HTML_STRING().getText();

        return new HtmlAttribute(name, value, ctx.start.getLine());
    }


    /* =====================================================
       CSS VISIT METHODS
       ===================================================== */

    @Override
    public ASTNode visitStyleElement(TemplateParser.StyleElementContext ctx) {

        TemplateParser.StyleOpenTagContext openCtx =
                (TemplateParser.StyleOpenTagContext) ctx.style_open();

        List<HtmlAttribute> attrs = new ArrayList<>();
        for (TemplateParser.AttributeContext aCtx : openCtx.attribute()) {
            attrs.add((HtmlAttribute) visit(aCtx));
        }

        CssStylesheet sheet = (CssStylesheet) visit(ctx.css_stylesheet());

        return new HtmlStyleElement(
                attrs,
                sheet,
                ctx.start.getLine()
        );
    }

    @Override
    public ASTNode visitCssStylesheet(TemplateParser.CssStylesheetContext ctx) {

        List<ASTNode> items = new ArrayList<>();

        for (var c : ctx.css_content()) {
            ASTNode node = visit(c);
            if (node != null) items.add(node);
        }

        return new CssStylesheet(items, ctx.start.getLine());
    }


    @Override
    public ASTNode visitCssRuleContent(TemplateParser.CssRuleContentContext ctx) {
        return visit(ctx.css_rule());
    }

    @Override
    public ASTNode visitCssRule(TemplateParser.CssRuleContext ctx) {

        CssSelector selector = (CssSelector) visit(ctx.css_selector());

        List<CssNode> blockContents = new ArrayList<>();
        for (TemplateParser.Css_block_contentContext b : ctx.css_block_content()) {
            CssNode n = (CssNode) visit(b);
            if (n != null) blockContents.add(n);
        }

        return new CssRule(selector, blockContents, ctx.start.getLine());
    }

    @Override
    public ASTNode visitCssRawSelector(TemplateParser.CssRawSelectorContext ctx) {

        int line = ctx.start.getLine();

        String selectorText = ctx.getText();

        List<JinjaExpr> jinjaExpressions = new ArrayList<>();

        for (ParseTree child : ctx.children) {
            ASTNode node = visit(child);
            if (node instanceof JinjaExpr expr) {
                jinjaExpressions.add(expr);
            }
        }

        return new CssSelector(selectorText, jinjaExpressions, line);
    }

    @Override
    public ASTNode visitCssDeclarationBlockContent(TemplateParser.CssDeclarationBlockContentContext ctx) {
        return visit(ctx.css_declaration());
    }

    @Override
    public ASTNode visitCssDeclaration(TemplateParser.CssDeclarationContext ctx) {
        String prop = ctx.CSS_PROPERTY().getText();
        CssValue val = (CssValue) visit(ctx.css_value());
        return new CssDeclaration(prop, val, ctx.start.getLine());
    }

    @Override
    public ASTNode visitCssValue(TemplateParser.CssValueContext ctx) {

        List<CssValuePart> parts = new ArrayList<>();

        // css_value : css_space_value+ ;
        for (TemplateParser.Css_space_valueContext sv : ctx.css_space_value()) {

            // css_space_value : css_value_part+ ;
            for (ParseTree child : sv.children) {
                ASTNode n = visit(child);

                if (n instanceof CssValuePart p) {
                    // normal CSS token (ident, number, function, etc.)
                    parts.add(p);
                }
                else if (n instanceof JinjaExpr expr) {
                    // {{ ... }} inside a value
                    parts.add(new CssJinjaExpressionValue(expr, getLine(child)));
                }
            }
        }

        return new CssValue(parts, ctx.start.getLine());
    }


    private int getLine(ParseTree t) {
        if (t instanceof ParserRuleContext prc)
            return prc.getStart().getLine();
        if (t instanceof TerminalNode tn)
            return tn.getSymbol().getLine();
        return -1;
    }


    @Override
    public ASTNode visitCssPrimitiveValue(TemplateParser.CssPrimitiveValueContext ctx) {

        String raw = ctx.CSS_VALUE().getText();
        return new CssPrimitiveValue(raw, ctx.start.getLine());
    }


    @Override
    public ASTNode visitCssJinjaExpressionValue(TemplateParser.CssJinjaExpressionValueContext ctx) {

        JinjaExpr expr = (JinjaExpr) visit(ctx.jinja_expression());
        return new CssJinjaExpressionValue(expr, ctx.start.getLine());
    }


    @Override
    public ASTNode visitCssFunctionValue(TemplateParser.CssFunctionValueContext ctx) {

        CssFunctionCall func = (CssFunctionCall) visit(ctx.css_function_call());
        return func;
    }

    @Override
    public ASTNode visitCssJinjaStatementValue(TemplateParser.CssJinjaStatementValueContext ctx) {
        return visit(ctx.jinja_css_value_statement());
    }

    @Override
    public ASTNode visitCssFunctionCall(TemplateParser.CssFunctionCallContext ctx) {

        String raw = ctx.CSS_FUNCTION().getText();
        String name = raw.endsWith("(") ? raw.substring(0, raw.length() - 1) : raw;

        CssValue args = null;
        if (ctx.css_value() != null) {
            args = (CssValue) visit(ctx.css_value());
        }


        return new CssFunctionCall(name, args, ctx.start.getLine());
    }

    @Override
    public ASTNode visitJinjaCssIfValue(TemplateParser.JinjaCssIfValueContext ctx) {
        return visit(ctx.jinja_css_if_block());
    }


    @Override
    public ASTNode visitJinjaCssIfBlock(TemplateParser.JinjaCssIfBlockContext ctx) {

        JinjaExpr cond = (JinjaExpr) visit(ctx.jinja_stmt_expr());

        CssValue thenVal = (CssValue) visit(ctx.css_value());

        CssValue elseVal = null;
        if (ctx.jinja_css_else_clause() != null) {
            TemplateParser.Jinja_css_else_clauseContext ectx = ctx.jinja_css_else_clause();
            ParseTree lastChild = ectx.getChild(ectx.getChildCount() - 1);
            elseVal = (CssValue) visit(lastChild);
        }

        return new CssJinjaValueIf(cond, thenVal, elseVal, ctx.start.getLine());
    }

    @Override
    public ASTNode visitJinjaCssElseClause(TemplateParser.JinjaCssElseClauseContext ctx) {
        return visit(ctx.css_value());
    }

//    @Override
//    public ASTNode visitJinjaCssForBlock(TemplateParser.JinjaCssForBlockContext ctx) {
//
//        ASTNode vars = visit(ctx.jinja_for_vars());
//        ASTNode iterable = visit(ctx.jinja_stmt_expr());
//
//        List<CssNode> body = new ArrayList<>();
//        for (TemplateParser.Css_contentContext c : ctx.css_content()) {
//            CssNode n = (CssNode) visit(c);
//            if (n != null) body.add(n);
//        }
//
//        return new JinjaCssForBlock(vars, iterable, body, ctx.start.getLine());
//    }




    /* =====================================================
       Jinja VISIT METHODS
       ===================================================== */

    @Override
    public ASTNode visitJinjaExprNode(TemplateParser.JinjaExprNodeContext ctx) {
        return visit(ctx.jinja_expression());
    }

    @Override
    public ASTNode visitJinjaStmtNode(TemplateParser.JinjaStmtNodeContext ctx) {
        return visit(ctx.jinja_statement());
    }

    @Override
    public ASTNode visitJinjaExpression(TemplateParser.JinjaExpressionContext ctx) {
        return visit(ctx.jinja_expr());
    }

    @Override
    public ASTNode visitJinjaBinaryExpr(TemplateParser.JinjaBinaryExprContext ctx) {

        JinjaExpr result = (JinjaExpr) visit(ctx.jinja_term(0));
        int line = ctx.start.getLine();

        for (int i = 1; i < ctx.jinja_term().size(); i++) {
            String op = ctx.JINJA_OP(i - 1).getText();
            JinjaExpr right = (JinjaExpr) visit(ctx.jinja_term(i));
            result = new JinjaBinaryExpr(result, op, right, line);
        }

        return result;
    }

    @Override
    public ASTNode visitJinjaTerm(TemplateParser.JinjaTermContext ctx) {

        JinjaExpr expr = (JinjaExpr) visit(ctx.jinja_atom());

        for (TemplateParser.Jinja_postfixContext p : ctx.jinja_postfix()) {

            if (p instanceof TemplateParser.JinjaMemberAccessContext mem) {

                String name = mem.JINJA_ID().getText();
                expr = new JinjaAttrExpr(expr, name, mem.start.getLine());
            }

            else if (p instanceof TemplateParser.JinjaCallPostfixContext callPost) {

                TemplateParser.Jinja_callContext baseCall = callPost.jinja_call();
                TemplateParser.JinjaCallContext cctx =
                        (TemplateParser.JinjaCallContext) baseCall;

                List<JinjaExpr> args = new ArrayList<>();

                if (cctx.jinja_arg_list() != null) {

                    TemplateParser.Jinja_arg_listContext baseArgs = cctx.jinja_arg_list();
                    TemplateParser.JinjaArgListContext argList =
                            (TemplateParser.JinjaArgListContext) baseArgs;

                    for (TemplateParser.Jinja_exprContext ectx : argList.jinja_expr()) {
                        args.add((JinjaExpr) visit(ectx));
                    }
                }

                expr = new JinjaCallExpr(expr, args, cctx.start.getLine());
            }

            else if (p instanceof TemplateParser.JinjaFilterPostfixContext filPost) {

                TemplateParser.Jinja_filterContext baseFilter = filPost.jinja_filter();
                TemplateParser.JinjaFilterContext fctx =
                        (TemplateParser.JinjaFilterContext) baseFilter;

                String filterName = fctx.JINJA_ID().getText();

                List<JinjaExpr> args = new ArrayList<>();

                if (fctx.jinja_arg_list() != null) {

                    TemplateParser.Jinja_arg_listContext baseArgs = fctx.jinja_arg_list();
                    TemplateParser.JinjaArgListContext argList =
                            (TemplateParser.JinjaArgListContext) baseArgs;

                    for (TemplateParser.Jinja_exprContext ectx : argList.jinja_expr()) {
                        args.add((JinjaExpr) visit(ectx));
                    }
                }

                expr = new JinjaFilterExpr(expr, filterName, args, fctx.start.getLine());
            }
        }

        return expr;
    }




    @Override
    public ASTNode visitJinjaIdAtom(TemplateParser.JinjaIdAtomContext ctx) {
        return new JinjaIdentifierExpr(ctx.JINJA_ID().getText(), ctx.start.getLine());
    }

    @Override
    public ASTNode visitJinjaStringAtom(TemplateParser.JinjaStringAtomContext ctx) {
        return new JinjaStringLiteralExpr(ctx.JINJA_EXPR_STRING().getText(), ctx.start.getLine());
    }

    @Override
    public ASTNode visitJinjaNumberAtom(TemplateParser.JinjaNumberAtomContext ctx) {
        return new JinjaNumberLiteralExpr(ctx.JINJA_NUMBER().getText(), ctx.start.getLine());
    }

    @Override
    public ASTNode visitJinjaParenAtom(TemplateParser.JinjaParenAtomContext ctx) {
        return visit(ctx.jinja_expr());
    }

    @Override
    public ASTNode visitJinjaStmtExpr(TemplateParser.JinjaStmtExprContext ctx) {

        JinjaExpr result = (JinjaExpr) visit(ctx.jinja_stmt_term(0));
        int line = ctx.start.getLine();

        for (int i = 1; i < ctx.jinja_stmt_term().size(); i++) {
            TemplateParser.Jinja_stmt_opContext opCtx = ctx.jinja_stmt_op(i - 1);

            String op;
            if (opCtx instanceof TemplateParser.JinjaStmtBinaryOpContext bop) {
                op = bop.JINJA_STMT_OP().getText();
            } else if (opCtx instanceof TemplateParser.JinjaAndContext) {
                op = "and";
            } else {
                op = "or";
            }

            JinjaExpr right = (JinjaExpr) visit(ctx.jinja_stmt_term(i));
            result = new JinjaBinaryExpr(result, op, right, line);
        }

        return result;
    }

    @Override
    public ASTNode visitJinjaStmtTerm(TemplateParser.JinjaStmtTermContext ctx) {

        JinjaExpr expr = (JinjaExpr) visit(ctx.jinja_stmt_atom());

        for (TemplateParser.Jinja_stmt_postfixContext p : ctx.jinja_stmt_postfix()) {

            if (p instanceof TemplateParser.JinjaStmtMemberAccessContext mem) {
                String name = mem.JINJA_STMT_ID().getText();
                expr = new JinjaAttrExpr(expr, name, mem.start.getLine());
            }

            else if (p instanceof TemplateParser.JinjaStmtCallContext callPost) {

                TemplateParser.Jinja_stmt_callContext baseCall = callPost.jinja_stmt_call();
                TemplateParser.JinjaStmtCallExprContext cctx =
                        (TemplateParser.JinjaStmtCallExprContext) baseCall;

                List<JinjaExpr> args = new ArrayList<>();

                if (cctx.jinja_stmt_arg_list() != null) {

                    TemplateParser.Jinja_stmt_arg_listContext baseArgs =
                            cctx.jinja_stmt_arg_list();

                    TemplateParser.JinjaStmtArgListContext argList =
                            (TemplateParser.JinjaStmtArgListContext) baseArgs;

                    for (TemplateParser.Jinja_stmt_exprContext ectx : argList.jinja_stmt_expr()) {
                        args.add((JinjaExpr) visit(ectx));
                    }
                }

                expr = new JinjaCallExpr(expr, args, cctx.start.getLine());
            }

            else if (p instanceof TemplateParser.JinjaStmtFilterContext filPost) {

                TemplateParser.Jinja_stmt_filterContext baseFilter =
                        filPost.jinja_stmt_filter();

                TemplateParser.JinjaStmtFilterExprContext fctx =
                        (TemplateParser.JinjaStmtFilterExprContext) baseFilter;

                String filterName = fctx.JINJA_STMT_ID().getText();

                List<JinjaExpr> args = new ArrayList<>();

                if (fctx.jinja_stmt_arg_list() != null) {

                    TemplateParser.Jinja_stmt_arg_listContext baseArgs =
                            fctx.jinja_stmt_arg_list();

                    TemplateParser.JinjaStmtArgListContext argList =
                            (TemplateParser.JinjaStmtArgListContext) baseArgs;

                    for (TemplateParser.Jinja_stmt_exprContext ectx : argList.jinja_stmt_expr()) {
                        args.add((JinjaExpr) visit(ectx));
                    }
                }

                expr = new JinjaFilterExpr(expr, filterName, args, fctx.start.getLine());
            }
        }

        if (ctx.JINJA_NOT() != null) {
            expr = new JinjaUnaryExpr("not", expr, ctx.start.getLine());
        }

        return expr;
    }



    @Override
    public ASTNode visitJinjaStmtIdAtom(TemplateParser.JinjaStmtIdAtomContext ctx) {
        return new JinjaIdentifierExpr(ctx.JINJA_STMT_ID().getText(), ctx.start.getLine());
    }

    @Override
    public ASTNode visitJinjaStmtStringAtom(TemplateParser.JinjaStmtStringAtomContext ctx) {
        return new JinjaStringLiteralExpr(ctx.JINJA_STMT_STRING().getText(), ctx.start.getLine());
    }

    @Override
    public ASTNode visitJinjaStmtNumberAtom(TemplateParser.JinjaStmtNumberAtomContext ctx) {
        return new JinjaNumberLiteralExpr(ctx.JINJA_STMT_NUMBER().getText(), ctx.start.getLine());
    }

    @Override
    public ASTNode visitJinjaStmtParenAtom(TemplateParser.JinjaStmtParenAtomContext ctx) {
        return visit(ctx.jinja_stmt_expr());
    }

    @Override
    public ASTNode visitJinjaHtmlBody(TemplateParser.JinjaHtmlBodyContext ctx) {
        List<ASTNode> children = new ArrayList<>();
        for (TemplateParser.Template_nodeContext t : ctx.template_node()) {
            ASTNode node = visit(t);
            if (node != null) children.add(node);
        }
        return new JinjaBody(children, ctx.start.getLine());
    }

    @Override
    public ASTNode visitJinjaCssBody(TemplateParser.JinjaCssBodyContext ctx) {
        List<ASTNode> children = new ArrayList<>();
        for (TemplateParser.Css_contentContext c : ctx.css_content()) {
            ASTNode node = visit(c);
            if (node != null) children.add(node);
        }
        return new JinjaBody(children, ctx.start.getLine());
    }


    @Override
    public ASTNode visitJinjaIfStatement(TemplateParser.JinjaIfStatementContext ctx) {
        return visit(ctx.jinja_if_block());
    }

    @Override
    public ASTNode visitJinjaForStatement(TemplateParser.JinjaForStatementContext ctx) {
        return visit(ctx.jinja_for_block());
    }

    @Override
    public ASTNode visitJinjaIncludeStatement(TemplateParser.JinjaIncludeStatementContext ctx) {
        return visit(ctx.jinja_include_statement());
    }

    @Override
    public ASTNode visitJinjaBlockStatement(TemplateParser.JinjaBlockStatementContext ctx) {
        return visit(ctx.jinja_block_statement());
    }

    @Override
    public ASTNode visitJinjaIfBlock(TemplateParser.JinjaIfBlockContext ctx) {

        TemplateParser.Jinja_if_clauseContext baseIf = ctx.jinja_if_clause();
        TemplateParser.JinjaIfClauseContext ifc =
                (TemplateParser.JinjaIfClauseContext) baseIf;

        JinjaExpr cond = (JinjaExpr) visit(ifc.jinja_stmt_expr());
        JinjaBody thenBody = (JinjaBody) visit(ifc.jinja_body());

        List<JinjaElifClause> elifs = new ArrayList<>();

        for (TemplateParser.Jinja_elif_clauseContext baseElif : ctx.jinja_elif_clause()) {

            TemplateParser.JinjaElifClauseContext ectx =
                    (TemplateParser.JinjaElifClauseContext) baseElif;

            JinjaExpr eCond = (JinjaExpr) visit(ectx.jinja_stmt_expr());
            JinjaBody eBody = (JinjaBody) visit(ectx.jinja_body());

            elifs.add(new JinjaElifClause(eCond, eBody, ectx.start.getLine()));
        }

        JinjaBody elseBody = null;

        if (ctx.jinja_else_clause() != null) {

            TemplateParser.Jinja_else_clauseContext baseElse = ctx.jinja_else_clause();
            TemplateParser.JinjaElseClauseContext elctx =
                    (TemplateParser.JinjaElseClauseContext) baseElse;

            elseBody = (JinjaBody) visit(elctx.jinja_body());
        }

        return new JinjaIfStmt(cond, thenBody, elifs, elseBody, ctx.start.getLine());
    }


    @Override
    public ASTNode visitJinjaEndIf(TemplateParser.JinjaEndIfContext ctx) {
        return null;
    }

    @Override
    public ASTNode visitJinjaForBlock(TemplateParser.JinjaForBlockContext ctx) {

        TemplateParser.Jinja_for_clauseContext baseFor = ctx.jinja_for_clause();
        TemplateParser.JinjaForClauseContext fctx =
                (TemplateParser.JinjaForClauseContext) baseFor;

        TemplateParser.Jinja_for_varsContext baseVars = fctx.jinja_for_vars();
        TemplateParser.JinjaForVarsContext vctx =
                (TemplateParser.JinjaForVarsContext) baseVars;

        List<String> vars = new ArrayList<>();
        vctx.JINJA_STMT_ID().forEach(id -> vars.add(id.getText()));

        JinjaExpr iterable = (JinjaExpr) visit(fctx.jinja_stmt_expr());

        JinjaBody body = (JinjaBody) visit(fctx.jinja_body());

        return new JinjaForStmt(vars, iterable, body, ctx.start.getLine());
    }


    @Override
    public ASTNode visitJinjaEndFor(TemplateParser.JinjaEndForContext ctx) {
        return null;
    }

    @Override
    public ASTNode visitJinjaInclude(TemplateParser.JinjaIncludeContext ctx) {

        String templateName = ctx.JINJA_STMT_STRING().getText();
        return new JinjaIncludeStmt(templateName, ctx.start.getLine());
    }


    @Override
    public ASTNode visitJinjaBlock(TemplateParser.JinjaBlockContext ctx) {

        TemplateParser.Jinja_block_openContext baseOpen = ctx.jinja_block_open();

        TemplateParser.JinjaBlockOpenContext open =
                (TemplateParser.JinjaBlockOpenContext) baseOpen;

        String name = open.JINJA_STMT_ID().getText();

        JinjaBody body = (JinjaBody) visit(ctx.jinja_body());

        return new JinjaBlockStmt(name, body, ctx.start.getLine());
    }

    @Override
    public ASTNode visitJinjaBlockClose(TemplateParser.JinjaBlockCloseContext ctx) {
        return null;
    }

    @Override
    public ASTNode visitJinjaExtends(TemplateParser.JinjaExtendsContext ctx) {

        String raw = ctx.JINJA_STMT_STRING().getText();
        String templateName = raw.substring(1, raw.length() - 1);

        return new JinjaExtendsStmt(templateName, ctx.start.getLine());
    }


}
