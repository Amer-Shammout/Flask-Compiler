parser grammar TemplateParser;

@header {
    // Parser package
    package antlr;
}

options {
    // Lexer tokens
    tokenVocab = TemplateLexer;
}

// ======================================================
// ROOT
// ======================================================

// Template entry
template
    : doctype? template_content EOF                 # TemplateRoot
    ;

// Doctype
doctype
    : DOCTYPE                                      # DoctypeDecl
    ;

// ======================================================
// TEMPLATE CONTENT
// ======================================================

// Template body
template_content
    : template_node*                               # TemplateContent
    ;

// Template node
template_node
    : html_element                                 # HtmlNode
    | jinja_statement                              # JinjaStmtNode
    | jinja_expression                             # JinjaExprNode
    | HTML_TEXT                                    # TextNode
    ;

// ======================================================
// HTML
// ======================================================

// Html element
html_element
    : style_element                                # StyleHtmlElement
    | normal_element                               # NormalHtmlElement
    | self_closing_element                         # SelfClosingHtmlElement
    | void_element                                 # VoidHtmlElement
    ;

// <tag>...</tag>
normal_element
    : open_tag html_content close_tag              # NormalElement
    ;

// <tag /> // hay ana ghalia ghayyarta
self_closing_element
    : HTML_OPEN_TAG (HTML_TAG_NAME | HTML_VOID_TAG) attribute* HTML_SLASH_CLOSE
                                                   # SelfClosingElement
    ;

// <tag> // hay ana ghalia ghayyarta
void_element
     : HTML_OPEN_TAG (HTML_TAG_NAME | HTML_VOID_TAG) attribute* HTML_CLOSE_TAG
                                                   # VoidElement
    ;

// Inner html
html_content
    : template_node*                               # HtmlContent
    ;

// Opening tag
open_tag
    : HTML_OPEN_TAG HTML_TAG_NAME attribute* HTML_CLOSE_TAG
                                                   # OpenTag
    ;

// Closing tag
close_tag
    : HTML_OPEN_TAG HTML_SLASH HTML_TAG_NAME HTML_CLOSE_TAG
                                                   # CloseTag
    ;

// Html attribute
attribute
    : HTML_ATTR_NAME HTML_EQUALS HTML_STRING        # HtmlAttribute
    ;

// ======================================================
// CSS
// ======================================================

// <style> element
style_element
    : style_open css_stylesheet CSS_STYLE_END       # StyleElement
    ;

// <style> open
style_open
    : HTML_OPEN_TAG t=HTML_TAG_NAME attribute* HTML_CLOSE_TAG
      { $t.getText().equalsIgnoreCase("style") }?
                                                   # StyleOpenTag
    ;

// Css root
css_stylesheet
    : css_content*                                 # CssStylesheet
    ;

// Css content
css_content
    : css_rule                                     # CssRuleContent
    | jinja_statement                              # CssJinjaStmtContent
    ;

// Css rule
css_rule
    : css_selector CSS_LBRACE css_block_content* CSS_RBRACE
                                                   # CssRule
    ;

// Css selector
css_selector
    : css_selector_part+                           # CssRawSelector
    ;

// Selector part
css_selector_part
    : CSS_IDENT                                    # CssIdentPart
    | CSS_DOT                                      # CssDotPart
    | CSS_HASH                                     # CssHashPart
    | CSS_COLON                                    # CssColonPart
    | CSS_GT                                       # CssGtPart
    | CSS_PLUS                                     # CssPlusPart
    | CSS_TILDE                                    # CssTildePart
    | CSS_COMMA                                    # CssCommaPart
    | CSS_UNIVERSAL                                # CssUniversalPart
    | jinja_expression                             # CssJinjaSelectorExpr
    ;

// Css block content
css_block_content
    : css_declaration                              # CssDeclarationBlockContent
    | jinja_statement                              # CssJinjaBlockContent
    ;

// Css declaration
css_declaration
    : CSS_PROPERTY CSS_COLON_IN_BLOCK css_value CSS_SEMICOLON?
                                                   # CssDeclaration
    ;

// Css value
css_value
    : css_space_value (CSS_COMMA_IN_BLOCK css_space_value)*
                                                   # CssValue
    ;

// Space separated value
css_space_value
    : css_value_part+                              # CssSpaceValue
    ;

// Value part
css_value_part
    : CSS_VALUE                                    # CssPrimitiveValue
    | jinja_expression                             # CssJinjaExpressionValue
    | css_function_call                            # CssFunctionValue
    | jinja_css_value_statement                    # CssJinjaStatementValue
    ;

// Jinja css value
jinja_css_value_statement
    : jinja_css_if_block                           # JinjaCssIfValue
    | jinja_css_for_block                          # JinjaCssForValue
    ;

// Jinja css if
jinja_css_if_block
    : jinja_stmt_start JINJA_IF_STMT jinja_stmt_expr JINJA_STMT_END
      css_value
      jinja_css_else_clause?
      jinja_stmt_start JINJA_ENDIF JINJA_STMT_END
                                                   # JinjaCssIfBlock
    ;

// Jinja css else
jinja_css_else_clause
    : jinja_stmt_start JINJA_ELSE_STMT JINJA_STMT_END
      css_value                                   # JinjaCssElseClause
    ;

// Jinja css for
jinja_css_for_block
    : jinja_stmt_start JINJA_FOR
      jinja_for_vars
      JINJA_IN jinja_stmt_expr
      JINJA_STMT_END
      css_content*
      jinja_stmt_start JINJA_ENDFOR JINJA_STMT_END
                                                   # JinjaCssForBlock
    ;

// Css function
css_function_call
    : CSS_FUNCTION css_value? CSS_RPAREN           # CssFunctionCall
    ;

// ======================================================
// JINJA EXPRESSIONS
// ======================================================

// Expr start
jinja_expr_start
    : JINJA_EXPR_START                             # JinjaExprStart
    | JINJA_EXPR_START_IN_TAG                      # JinjaExprStartInTag
    | JINJA_EXPR_START_IN_CSS                      # JinjaExprStartInCss
    | JINJA_EXPR_START_IN_CSS_BLOCK                # JinjaExprStartInCssBlock
    ;

// Jinja expression
jinja_expression
    : jinja_expr_start jinja_expr JINJA_EXPR_END   # JinjaExpression
    ;

// Binary expr
jinja_expr
    : jinja_term (JINJA_OP jinja_term)*            # JinjaBinaryExpr
    ;

// Expr term
jinja_term
    : jinja_atom jinja_postfix*                    # JinjaTerm
    ;

// Postfix
jinja_postfix
    : JINJA_EXPR_DOT JINJA_ID                      # JinjaMemberAccess
    | jinja_call                                   # JinjaCallPostfix
    | jinja_filter                                 # JinjaFilterPostfix
    ;

// Function call
jinja_call
    : JINJA_EXPR_LPAREN jinja_arg_list? JINJA_EXPR_RPAREN
                                                   # JinjaCall
    ;

// Filter
jinja_filter
    : JINJA_EXPR_PIPE JINJA_ID
      (JINJA_EXPR_LPAREN jinja_arg_list? JINJA_EXPR_RPAREN)?
                                                   # JinjaFilter
    ;

// Arg list
jinja_arg_list
    : jinja_expr (JINJA_EXPR_COMMA jinja_expr)*    # JinjaArgList
    ;

// Atom
jinja_atom
    : JINJA_ID                                     # JinjaIdAtom
    | JINJA_EXPR_STRING                            # JinjaStringAtom
    | JINJA_NUMBER                                 # JinjaNumberAtom
    | JINJA_EXPR_LPAREN jinja_expr JINJA_EXPR_RPAREN
                                                   # JinjaParenAtom
    ;

// ======================================================
// JINJA STATEMENTS
// ======================================================

// Jinja statement
jinja_statement
    : jinja_if_block                               # JinjaIfStatement
    | jinja_for_block                              # JinjaForStatement
    | jinja_include_statement                      # JinjaIncludeStatement
    | jinja_block_statement                        # JinjaBlockStatement
    | jinja_extends_statement                      # JinjaExtendsStatement
    ;

// Stmt start
jinja_stmt_start
    : JINJA_STMT_START                             # JinjaStmtStart
    | JINJA_STMT_START_IN_TAG                      # JinjaStmtStartInTag
    | JINJA_STMT_START_IN_CSS                      # JinjaStmtStartInCss
    | JINJA_STMT_START_IN_CSS_BLOCK                # JinjaStmtStartInCssBlock
    ;

// If block
jinja_if_block
    : jinja_if_clause
      jinja_elif_clause*
      jinja_else_clause?
      jinja_endif_clause                           # JinjaIfBlock
    ;

// If clause
jinja_if_clause
    : jinja_stmt_start JINJA_IF_STMT jinja_stmt_expr JINJA_STMT_END
      jinja_body                                   # JinjaIfClause
    ;

// Elif clause
jinja_elif_clause
    : jinja_stmt_start JINJA_ELIF_STMT jinja_stmt_expr JINJA_STMT_END
      jinja_body                                   # JinjaElifClause
    ;

// Else clause
jinja_else_clause
    : jinja_stmt_start JINJA_ELSE_STMT JINJA_STMT_END
      jinja_body                                   # JinjaElseClause
    ;

// Endif
jinja_endif_clause
    : jinja_stmt_start JINJA_ENDIF JINJA_STMT_END  # JinjaEndIf
    ;

// For block
jinja_for_block
    : jinja_for_clause jinja_endfor_clause         # JinjaForBlock
    ;

// For clause
jinja_for_clause
    : jinja_stmt_start JINJA_FOR
      jinja_for_vars
      JINJA_IN jinja_stmt_expr
      JINJA_STMT_END
      jinja_body                                   # JinjaForClause
    ;

// For vars
jinja_for_vars
    : JINJA_STMT_ID (JINJA_STMT_COMMA JINJA_STMT_ID)*
                                                   # JinjaForVars
    ;

// Endfor
jinja_endfor_clause
    : jinja_stmt_start JINJA_ENDFOR JINJA_STMT_END # JinjaEndFor
    ;

// Include
jinja_include_statement
    : jinja_stmt_start JINJA_STMT_ID JINJA_STMT_STRING JINJA_STMT_END
                                                   # JinjaInclude
    ;

// Block
jinja_block_statement
    : jinja_block_open jinja_body jinja_block_close
                                                   # JinjaBlock
    ;

// Block open
jinja_block_open
    : jinja_stmt_start JINJA_BLOCK JINJA_STMT_ID JINJA_STMT_END
                                                   # JinjaBlockOpen
    ;

// Block close
jinja_block_close
    : jinja_stmt_start JINJA_ENDBLOCK JINJA_STMT_END
                                                   # JinjaBlockClose
    ;

jinja_extends_statement
    : jinja_stmt_start JINJA_EXTENDS JINJA_STMT_STRING JINJA_STMT_END
                                                   # JinjaExtends
    ;

// Statement expr
jinja_stmt_expr
    : jinja_stmt_term (jinja_stmt_op jinja_stmt_term)*
                                                   # JinjaStmtExpr
    ;

// Statement op
jinja_stmt_op
    : JINJA_STMT_OP                                # JinjaStmtBinaryOp
    | JINJA_AND                                    # JinjaAnd
    | JINJA_OR                                     # JinjaOr
    ;

// Statement term
jinja_stmt_term
    : JINJA_NOT? jinja_stmt_atom jinja_stmt_postfix*
                                                   # JinjaStmtTerm
    ;

// Statement postfix
jinja_stmt_postfix
    : JINJA_STMT_DOT JINJA_STMT_ID                 # JinjaStmtMemberAccess
    | jinja_stmt_call                              # JinjaStmtCall
    | jinja_stmt_filter                            # JinjaStmtFilter
    ;

// Statement call
jinja_stmt_call
    : JINJA_STMT_LPAREN jinja_stmt_arg_list? JINJA_STMT_RPAREN
                                                   # JinjaStmtCallExpr
    ;

// Statement filter
jinja_stmt_filter
    : JINJA_STMT_PIPE JINJA_STMT_ID
      (JINJA_STMT_LPAREN jinja_stmt_arg_list? JINJA_STMT_RPAREN)?
                                                   # JinjaStmtFilterExpr
    ;

// Statement args
jinja_stmt_arg_list
    : jinja_stmt_expr (JINJA_STMT_COMMA jinja_stmt_expr)*
                                                   # JinjaStmtArgList
    ;

// Statement atom
jinja_stmt_atom
    : JINJA_STMT_ID                                # JinjaStmtIdAtom
    | JINJA_STMT_STRING                            # JinjaStmtStringAtom
    | JINJA_STMT_NUMBER                            # JinjaStmtNumberAtom
    | JINJA_STMT_LPAREN jinja_stmt_expr JINJA_STMT_RPAREN
                                                   # JinjaStmtParenAtom
    ;

// Jinja body
jinja_body
    : template_node*                               # JinjaHtmlBody
    | css_content*                                 # JinjaCssBody
    ;
