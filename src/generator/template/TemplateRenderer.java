package generator.template;

import AST.ASTNode;
import AST.template.css.CssDeclaration;
import AST.template.css.CssFunctionCall;
import AST.template.css.CssJinjaExpressionValue;
import AST.template.css.CssNode;
import AST.template.css.CssPrimitiveValue;
import AST.template.css.CssRule;
import AST.template.css.CssSelector;
import AST.template.css.CssStylesheet;
import AST.template.css.CssValue;
import AST.template.css.CssValuePart;
import AST.template.html.HtmlAttribute;
import AST.template.html.HtmlDocument;
import AST.template.html.HtmlNormalElement;
import AST.template.html.HtmlSelfClosingElement;
import AST.template.html.HtmlStyleElement;
import AST.template.html.HtmlText;
import AST.template.html.HtmlVoidElement;
import AST.template.jinja.JinjaBody;
import AST.template.jinja.expr.JinjaExpr;
import AST.template.jinja.stmt.JinjaBlockStmt;
import AST.template.jinja.stmt.JinjaElifClause;
import AST.template.jinja.stmt.JinjaExtendsStmt;
import AST.template.jinja.stmt.JinjaForStmt;
import AST.template.jinja.stmt.JinjaIfStmt;
import AST.template.jinja.stmt.JinjaIncludeStmt;
import AST.template.jinja.stmt.JinjaStmt;
import generator.GenerationPipeline;
import generator.context.ContextData;
import generator.context.RuntimeValue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Walks a Template AST and emits HTML, substituting Jinja {@code {{ }}} / {@code {% %}}.
 * <p>
 * Step 6: supports {@code {% if %}} / {@code {% elif %}} / {@code {% else %}} and {@code {% for %}}.
 */
public final class TemplateRenderer {

    private static final Pattern ATTR_JINJA = Pattern.compile("\\{\\{\\s*(.+?)\\s*}}");

    private ContextData context;
    private final StringBuilder out = new StringBuilder();
    private final Path templateDirectory;
    private Map<String, JinjaBody> blockOverrides = Map.of();

    /** Convenience constructor for templates that don't use {@code extends}/{@code include}. */
    public TemplateRenderer(ContextData context) {
        this(context, null);
    }

    /**
     * @param templateDirectory where {@code {% extends "x.html" %}} / {@code {% include "x.html" %}}
     *                          resolve their target file from. May be {@code null} if the template
     *                          doesn't use either (resolving one without it throws).
     */
    public TemplateRenderer(ContextData context, Path templateDirectory) {
        this.context = context == null ? new ContextData() : context;
        this.templateDirectory = templateDirectory;
    }

    public String render(ASTNode root) {
        out.setLength(0);

        JinjaExtendsStmt extendsStmt = findTopLevelExtends(root);
        if (extendsStmt != null) {
            Map<String, JinjaBody> overrides = new LinkedHashMap<>();
            collectBlocks(root, overrides);

            Map<String, JinjaBody> previous = this.blockOverrides;
            this.blockOverrides = overrides;
            renderNode(loadTemplate(extendsStmt.getTemplateName()));
            this.blockOverrides = previous;
            return out.toString();
        }

        renderNode(root);
        return out.toString();
    }

    /** {@code {% extends %}} only takes effect when it's a top-level statement in the child template. */
    private JinjaExtendsStmt findTopLevelExtends(ASTNode root) {
        if (root instanceof HtmlDocument doc) {
            for (ASTNode child : doc.getChildrenNodes()) {
                if (child instanceof JinjaExtendsStmt extendsStmt) {
                    return extendsStmt;
                }
            }
        }
        return null;
    }

    /** Collects every {@code {% block name %}...{% endblock %}} in the child template, by name. */
    private void collectBlocks(ASTNode node, Map<String, JinjaBody> into) {
        if (node == null) {
            return;
        }
        if (node instanceof JinjaBlockStmt block) {
            into.put(block.getName(), block.getBody());
        }
        for (ASTNode child : node.getChildren()) {
            collectBlocks(child, into);
        }
    }

    /** Parses {@code name} from {@link #templateDirectory} (used by both {@code extends} and {@code include}). */
    private ASTNode loadTemplate(String name) {
        if (templateDirectory == null) {
            throw new UnsupportedOperationException(
                    "Template uses extends/include (\"" + name + "\") but this TemplateRenderer "
                            + "was not given a template directory to resolve it from.");
        }
        try {
            String source = Files.readString(templateDirectory.resolve(name));
            return GenerationPipeline.parseTemplateHeadless(source);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load template \"" + name + "\" for extends/include", e);
        }
    }

    private void renderNode(ASTNode node) {
        if (node == null) {
            return;
        }

        if (node instanceof HtmlDocument doc) {
            for (ASTNode child : doc.getChildrenNodes()) {
                renderNode(child);
            }
            return;
        }

        if (node instanceof HtmlText text) {
            out.append(text.getText());
            return;
        }

        if (node instanceof HtmlStyleElement style) {
            renderOpenTag("style", style.getAttributes(), false);
            for (ASTNode child : style.getChildren()) {
                if (child instanceof CssStylesheet sheet) {
                    renderCssStylesheet(sheet);
                }
            }
            out.append("</style>");
            return;
        }

        if (node instanceof HtmlNormalElement el) {
            renderOpenTag(el.getTagName(), el.getAttributes(), false);
            for (ASTNode child : el.getChildNodes()) {
                renderNode(child);
            }
            out.append("</").append(el.getTagName()).append(">");
            return;
        }

        if (node instanceof HtmlVoidElement el) {
            renderOpenTag(el.getTagName(), el.getAttributes(), true);
            return;
        }

        if (node instanceof HtmlSelfClosingElement el) {
            renderOpenTag(el.getTagName(), el.getAttributes(), true);
            return;
        }

        if (node instanceof JinjaExpr expr) {
            RuntimeValue value = JinjaExpressionEvaluator.evaluate(expr, context);
            out.append(escapeHtml(value.toString()));
            return;
        }

        if (node instanceof JinjaBody body) {
            for (ASTNode child : body.getBodyChildren()) {
                renderNode(child);
            }
            return;
        }

        if (node instanceof JinjaIfStmt ifStmt) {
            renderIf(ifStmt);
            return;
        }

        if (node instanceof JinjaForStmt forStmt) {
            renderFor(forStmt);
            return;
        }

        if (node instanceof JinjaBlockStmt block) {
            // When rendering via extends(), a child override for this block name wins;
            // otherwise render the block's own (parent/default) body.
            JinjaBody override = blockOverrides.get(block.getName());
            renderNode(override != null ? override : block.getBody());
            return;
        }

        if (node instanceof JinjaIncludeStmt include) {
            // Included content renders inline against the *current* context, matching Jinja defaults.
            renderNode(loadTemplate(include.getTemplateName()));
            return;
        }

        if (node instanceof JinjaExtendsStmt) {
            // Already handled by render()'s pre-scan; a bare walk just skips the declaration itself.
            return;
        }

        if (node instanceof JinjaStmt stmt) {
            throw new UnsupportedOperationException(
                    "Unsupported Jinja statement: " + stmt.getClass().getSimpleName()
            );
        }

        for (ASTNode child : node.getChildren()) {
            renderNode(child);
        }
    }

    private void renderIf(JinjaIfStmt ifStmt) {
        if (JinjaExpressionEvaluator.evaluate(ifStmt.getCondition(), context).isTruthy()) {
            renderNode(ifStmt.getThenBody());
            return;
        }

        if (ifStmt.getElifClauses() != null) {
            for (JinjaElifClause elif : ifStmt.getElifClauses()) {
                if (JinjaExpressionEvaluator.evaluate(elif.getCondition(), context).isTruthy()) {
                    renderNode(elif.getBody());
                    return;
                }
            }
        }

        if (ifStmt.getElseBody() != null) {
            renderNode(ifStmt.getElseBody());
        }
    }

    private void renderFor(JinjaForStmt forStmt) {
        RuntimeValue iterable = JinjaExpressionEvaluator.evaluate(forStmt.getIterable(), context);
        if (iterable.getKind() != RuntimeValue.Kind.LIST) {
            return;
        }

        List<String> vars = forStmt.getVariables();
        String loopVar = (vars == null || vars.isEmpty()) ? "item" : vars.get(0);

        ContextData parent = this.context;
        for (RuntimeValue item : iterable.asList()) {
            this.context = parent.withLocal(loopVar, item);
            renderNode(forStmt.getBody());
        }
        this.context = parent;
    }

    private void renderOpenTag(String tagName, List<HtmlAttribute> attributes, boolean selfClosing) {
        out.append("<").append(tagName);
        if (attributes != null) {
            for (HtmlAttribute attr : attributes) {
                out.append(" ").append(attr.getName());
                String value = attr.getValue();
                if (value != null && !value.isEmpty()) {
                    String interpolated = interpolateAttributeValue(value);
                    if (isQuoted(interpolated)) {
                        out.append("=").append(interpolated);
                    } else if (isQuoted(value)) {
                        char q = value.charAt(0);
                        out.append("=").append(q).append(stripOuterQuotes(interpolated)).append(q);
                    } else {
                        out.append("=\"").append(interpolated).append("\"");
                    }
                }
            }
        }
        if (selfClosing) {
            out.append(" />");
        } else {
            out.append(">");
        }
    }

    /**
     * Grammar stores attribute values as plain strings, so {@code href="...?id={{ product.id }}"}
     * is not a Jinja AST node — interpolate dotted paths here.
     */
    private String interpolateAttributeValue(String raw) {
        Matcher matcher = ATTR_JINJA.matcher(raw);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String path = matcher.group(1).trim();
            String replacement = Matcher.quoteReplacement(
                    escapeHtml(JinjaExpressionEvaluator.evaluatePath(path, context).toString())
            );
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * HTML-escapes a value before it is written into text content or an attribute value,
     * matching Jinja's default autoescape behavior. Applied to every {@code {{ }}} substitution
     * so untrusted/unexpected data (e.g. a product name containing {@code <} or {@code &})
     * cannot corrupt the surrounding markup.
     */
    private static String escapeHtml(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw == null ? "" : raw;
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private void renderCssStylesheet(CssStylesheet sheet) {
        if (sheet == null) {
            return;
        }
        for (ASTNode content : sheet.getContents()) {
            if (content instanceof CssRule rule) {
                renderCssRule(rule);
            } else {
                renderNode(content);
            }
        }
    }

    private void renderCssRule(CssRule rule) {
        CssSelector selector = rule.getSelector();
        out.append(selector.getSelectorText()).append(" {");
        for (CssNode part : rule.getBlockContents()) {
            if (part instanceof CssDeclaration decl) {
                out.append(decl.getProperty()).append(": ");
                renderCssValue(decl.getValue());
                out.append("; ");
            }
        }
        out.append("} ");
    }

    private void renderCssValue(CssValue value) {
        if (value == null) {
            return;
        }
        boolean first = true;
        for (CssValuePart part : value.getParts()) {
            if (!first) {
                out.append(" ");
            }
            first = false;
            if (part instanceof CssPrimitiveValue prim) {
                out.append(prim.getText());
            } else if (part instanceof CssFunctionCall call) {
                out.append(call.getName()).append("(");
                renderCssValue(call.getArgs());
                out.append(")");
            } else if (part instanceof CssJinjaExpressionValue jinja) {
                RuntimeValue v = JinjaExpressionEvaluator.evaluate(jinja.getExpr(), context);
                out.append(v.toString());
            }
        }
    }

    private static boolean isQuoted(String value) {
        if (value == null || value.length() < 2) {
            return false;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        return (first == '"' && last == '"') || (first == '\'' && last == '\'');
    }

    private static String stripOuterQuotes(String value) {
        if (isQuoted(value)) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
