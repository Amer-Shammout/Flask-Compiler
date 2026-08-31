lexer grammar TemplateLexer;

@header { package antlr; }

@members {
    boolean isTagName = false;
    boolean insideStyle = false;

    java.util.LinkedList<Token> pendingTokens = new java.util.LinkedList<>();

           @Override
                public Token nextToken() {
                    if (!pendingTokens.isEmpty()) {
                        return pendingTokens.poll();
                    }
                    Token next = super.nextToken();
                    return next;
                }


        Token commonToken(int type, String text) {
            return _factory.create(
                new Pair<>(this, _input),
                type,
                text,
                DEFAULT_TOKEN_CHANNEL,
                -1, -1,
                getLine(),
                getCharPositionInLine()
            );
        }
}


// ---------- DEFAULT MODE: outside tags (text, comments) ----------
// whitespace outside tags
HTML_WS : [ \t\r\n]+ -> skip ;

DOCTYPE
    : '<!' 'DOCTYPE' (~'>')* '>'
    ;


HTML_COMMENT
    : '<!--' .*? '-->' -> skip
    ;


// ---------------- JINJA2 BLOCKS (Default Mode) ----------------

// Jinja2 Comment
JINJA_COMMENT
    : '{#' .*? '#}' -> skip
    ;

// Jinja2 Expression Start:  {{ ... }}
JINJA_EXPR_START
    : '{{' -> pushMode(JINJA_EXPR_MODE)
    ;

// Jinja2 Statement Start:  {% ... %}
JINJA_STMT_START
    : '{%' -> pushMode(JINJA_STMT_MODE)
    ;

HTML_OPEN_TAG
    : '<' { isTagName = true; } -> pushMode(TAG_MODE)
    ;


// TEXT outside tags
HTML_TEXT
    : (~('<' | '{'))+ {
        String t = getText();
        if (t.trim().isEmpty()) skip();
        else setText(t.trim());
    }
    ;



// ---------------- TAG_MODE: inside < ... > ----------------
mode TAG_MODE;

JINJA_EXPR_START_IN_TAG
    : '{{' -> pushMode(JINJA_EXPR_MODE)
    ;

JINJA_STMT_START_IN_TAG
    : '{%' -> pushMode(JINJA_STMT_MODE)
    ;


HTML_CLOSE_TAG
    : '>' {
          if (insideStyle) {
              insideStyle = false;
              pushMode(CSS_MODE);
          } else {
              popMode();
          }
      } ;


// self-closing "/>"
HTML_SLASH_CLOSE
    : '/>' -> popMode
    ;

// slash alone </element>
HTML_SLASH
    : '/'
    ;

// hay ana ghalia hateta
 HTML_VOID_TAG
        : {isTagName}? (
             'meta'
           | 'br'
           | 'hr'
           | 'link'
           | 'input'
           | 'img'
          ) { isTagName = false; }
        ;


// tag name
HTML_TAG_NAME
    : {isTagName}? (
          'html'
        | 'head'
        | 'title'
        | 'meta'
        | 'link'
        | 'style'
        | 'script'
        | 'body'
        | 'header'
        | 'main'
        | 'footer'
        | 'section'
        | 'article'
        | 'nav'
        | 'aside'
        | 'div'
        | 'span'
        | 'p'
        | 'h1'
        | 'h2'
        | 'h3'
        | 'h4'
        | 'h5'
        | 'h6'
        | 'a'
        | 'img'
        | 'video'
        | 'audio'
        | 'form'
        | 'input'
        | 'button'
        | 'label'
        | 'textarea'
        | 'select'
        | 'option'
        | 'table'
        | 'thead'
        | 'tbody'
        | 'tr'
        | 'td'
        | 'th'
        | 'ul'
        | 'ol'
        | 'li'
        | 'br'
        | 'hr'
        | 'canvas'
        | 'svg'
        | 'iframe'

        // fallback
        | [a-zA-Z][a-zA-Z0-9\\-]*
      )
      {
          isTagName = false;

          if (getText().equalsIgnoreCase("style")) {
              insideStyle = true;
          }
      }
    ;

// attribute name
HTML_ATTR_NAME
    : [a-zA-Z_:][a-zA-Z0-9_:.-]*
    ;

// equals
HTML_EQUALS : '=' ;

// attribute value
HTML_STRING
    : '"' (~["\r\n])* '"'
    | '\'' (~['\r\n])* '\''
    ;

// whitespace inside tag (skip)
TAG_WS : [ \t\r\n]+ -> skip ;

// ---------------- JINJA_EXPR_MODE ----------------
mode JINJA_EXPR_MODE;

JINJA_EXPR_END
    : '}}' -> popMode
    ;

JINJA_EXPR_DOT : '.';


// identifiers
JINJA_ID
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

// Pipe for filters
JINJA_EXPR_PIPE : '|';

JINJA_EXPR_LPAREN : '(' ;
JINJA_EXPR_RPAREN : ')' ;
JINJA_EXPR_COMMA  : ',' ;
JINJA_EXPR_COLON  : ':' ;


JINJA_EXPR_STRING
    : '"' (~["\r\n])* '"'
    | '\'' (~['\r\n])* '\''
    ;

// numbers
JINJA_NUMBER
    : '0' ('.' [0-9]+)?
    | [1-9] [0-9]* ('.' [0-9]+)?
    ;

// operators
JINJA_OP
    : '==' | '!=' | '>=' | '<=' | '>' | '<' | '+' | '-' | '*' | '/' | '%' | '~'
    ;

// whitespace inside jinja expression
JINJA_EXPR_WS : [ \t\r\n]+ -> skip;



// ---------------- JINJA_STMT_MODE ----------------
mode JINJA_STMT_MODE;

JINJA_STMT_END
    : '%}' -> popMode
    ;

JINJA_STMT_DOT : '.';

// keywords for statements
JINJA_FOR       : 'for';
JINJA_ENDFOR    : 'endfor';
JINJA_IF_STMT   : 'if';
JINJA_ELIF_STMT : 'elif';
JINJA_ELSE_STMT : 'else';
JINJA_ENDIF     : 'endif';
JINJA_IN        : 'in';
JINJA_BLOCK     : 'block';
JINJA_ENDBLOCK  : 'endblock';
JINJA_EXTENDS  : 'extends';
JINJA_SET : 'set' ;

// logical operators
JINJA_AND       : 'and';
JINJA_OR        : 'or';
JINJA_NOT       : 'not';
JINJA_STMT_ASSIGN : '=' ;

// identifiers
JINJA_STMT_ID
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

JINJA_STMT_LPAREN : '(' ;
JINJA_STMT_RPAREN : ')' ;
JINJA_STMT_COMMA  : ',' ;
JINJA_STMT_COLON  : ':' ;


// numbers
JINJA_STMT_NUMBER
    : '0' ('.' [0-9]+)?
    | [1-9] [0-9]* ('.' [0-9]+)?
    ;

// Pipe for filters
JINJA_STMT_PIPE : '|';

JINJA_STMT_STRING
    : '"' (~["\r\n])* '"'
    | '\'' (~['\r\n])* '\''
    ;

// operators
JINJA_STMT_OP
    : '==' | '!=' | '>=' | '<=' | '>' | '<' | '+' | '-' | '*' | '/' | '%' | '~'
    ;

// whitespace inside jinja stmt
JINJA_STMT_WS : [ \t\r\n]+ -> skip;

// fallback
//JINJA_STMT_TEXT
//    : ~[%]+
//    ;

// ---------- CSS_MODE ----------
mode CSS_MODE;

JINJA_COMMENT_IN_CSS
    : '{#' .*? '#}' -> skip;

JINJA_EXPR_START_IN_CSS
    : '{{' -> pushMode(JINJA_EXPR_MODE);

JINJA_STMT_START_IN_CSS
    : '{%' -> pushMode(JINJA_STMT_MODE);

// any whitespace
CSS_WS : [ \t\r\n]+ -> skip ;

// </style>
CSS_STYLE_END
    : '</style>' -> popMode, mode(DEFAULT_MODE)
    ;

CSS_UNIVERSAL : '*' ;

// Comments
CSS_COMMENT
    : '/*' .*? '*/' -> skip
    ;


// Open Block {
CSS_LBRACE
    : '{' -> pushMode(CSS_BLOCK_MODE)
    ;

// Connector
CSS_COMMA : ',' ;

// combinators
CSS_GT : '>';
CSS_PLUS : '+';
CSS_TILDE : '~';

// selector tokens
CSS_HASH : '#' ;
CSS_DOT : '.' ;
CSS_COLON : ':' ;


CSS_IDENT
    : [a-zA-Z_][a-zA-Z0-9_-]*
    ;

//CSS_AT_RULE
//    : '@' [a-zA-Z_-]+
//    ;



// ---------- CSS_BLOCK_MODE ----------
mode CSS_BLOCK_MODE;

JINJA_COMMENT_IN_CSS_BLOCK
    : '{#' .*? '#}' -> skip;

JINJA_EXPR_START_IN_CSS_BLOCK
    : '{{' -> pushMode(JINJA_EXPR_MODE);

JINJA_STMT_START_IN_CSS_BLOCK
    : '{%' -> pushMode(JINJA_STMT_MODE);

// End Of Block }
CSS_RBRACE : '}' -> popMode ;

// property name
CSS_PROPERTY
    : (
          'color'
        | 'background'
        | 'background-color'
        | 'width'
        | 'height'
        | 'margin'
        | 'margin-top'
        | 'margin-right'
        | 'margin-bottom'
        | 'margin-left'
        | 'padding'
        | 'padding-top'
        | 'padding-right'
        | 'padding-bottom'
        | 'padding-left'
        | 'display'
        | 'position'
        | 'top'
        | 'right'
        | 'bottom'
        | 'left'
        | 'font-size'
        | 'font-family'
        | 'font-weight'
        | 'line-height'
        | 'text-align'
        | 'text-decoration'
        | 'border'
        | 'border-radius'
        | 'border-color'
        | 'border-width'
        | 'flex'
        | 'flex-direction'
        | 'justify-content'
        | 'align-items'
        | 'grid-template-columns'
        | 'grid-template-rows'
        | 'gap'
        | 'overflow'
        | 'opacity'
        | 'visibility'
        | 'z-index'
        | 'cursor'
        | 'transition'
        | 'transform'
        | 'animation'

        // fallback
        | [a-zA-Z-]+
      )
      ':'
      {
         String text = getText();
         setText(text.substring(0, text.length() - 1));
         setType(CSS_PROPERTY);
         pendingTokens.add(commonToken(CSS_COLON_IN_BLOCK, ":"));
      }
    ;


CSS_COLON_IN_BLOCK : ':' ;
CSS_COMMA_IN_BLOCK : ',' ;


// ;
CSS_SEMICOLON : ';' ;


// function
CSS_FUNCTION
    : [a-zA-Z-]+ '('
    ;

CSS_RPAREN : ')' ;

fragment CSS_STRING
    : '"' (~["\r\n])* '"'
    | '\'' (~['\r\n])* '\''
    ;

fragment CSS_HASH_COLOR
    : '#' HEX_DIGIT HEX_DIGIT HEX_DIGIT (HEX_DIGIT HEX_DIGIT HEX_DIGIT)?
        ;

fragment HEX_DIGIT : [0-9a-fA-F] ;

fragment CSS_NUMBER
    : [0-9]+ ('.' [0-9]+)? NUMBER_UNIT?
    ;

NUMBER_UNIT
    : 'px' |
    'vw' |
    'fr' |
    '%'  ;

CSS_VALUE
    : CSS_STRING
    | CSS_HASH_COLOR
    | CSS_NUMBER
    | [a-zA-Z_][a-zA-Z0-9_-]*
    ;



// spaces
CSS_BLOCK_WS : [ \t\r\n]+ -> skip ;
