package SymbolTable;

import AST.ASTNode;
import AST.flask.Program;
import AST.SourceRange;
import AST.template.css.*;
import AST.template.html.*;
import AST.template.jinja.JinjaBody;
import AST.template.jinja.expr.*;
import AST.template.jinja.stmt.*;
import semantic.diagnostics.ResolutionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Two-pass template/Jinja symbol table builder (definitions, then reference resolution).
 */
public class TemplateSymbolTableBuilder extends SymbolTableBuilder {

    private final TemplateReferenceIndex referenceIndex = new TemplateReferenceIndex();
    private int siblingScopeIndex = 0;

    public TemplateSymbolTableBuilder(SymbolTableRepository repository) {
        super(repository);
        this.activeTable = repository.getTemplateGlobal();
    }

    private static String normalizeTemplateName(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }

    public TemplateReferenceIndex getReferenceIndex() {
        return referenceIndex;
    }

    /**
     * Build symbol tables and resolve references for a template AST root (e.g. {@link HtmlDocument}).
     */
    public ISymbolTable buildTemplate(ASTNode root) {
        referenceIndex.clear();
        activeTable = repository.getTemplateGlobal();
        siblingScopeIndex = 0;

        if (root != null) {
            collectDefinitions(root);
            activeTable = repository.getTemplateGlobal();
            siblingScopeIndex = 0;
            resolveReferences(root);
        }

        return repository.getTemplateGlobal();
    }

    @Override
    public ISymbolTable build(Program program) {
        throw new UnsupportedOperationException("Use buildTemplate(ASTNode) for template AST roots.");
    }

    // -------------------------------------------------------------------------
    // Pass 1
    // -------------------------------------------------------------------------

    @Override
    public void visit(ASTNode node) {
        if (node != null) {
            collectDefinitions(node);
            resolveReferences(node);
        }
    }

    private void collectDefinitions(ASTNode node) {
        if (node == null) {
            return;
        }

        switch (node) {
            case HtmlDocument document -> collectNodes(document.getChildrenNodes());
            case HtmlNormalElement element -> collectNodes(element.getChildNodes());
            case HtmlSelfClosingElement ignored -> {
            }
            case HtmlVoidElement ignored -> {
            }
            case HtmlStyleElement style -> {
                for (ASTNode child : style.getChildren()) {
                    if (child instanceof CssStylesheet stylesheet) {
                        collectCssStylesheet(stylesheet);
                    }
                }
            }
            case JinjaBody body -> collectNodes(body.getBodyChildren());
            case JinjaExpr ignored -> {
            }
            case JinjaStmt stmt -> collectJinjaStmtDefinitions(stmt);
            case CssStylesheet stylesheet -> collectCssStylesheet(stylesheet);
            case CssRule rule -> collectNodes(rule.getBlockContents());
            case CssDeclaration declaration -> collectCssValue(declaration.getValue());
            case CssJinjaExpressionValue ignored -> {
            }
            case CssJinjaValueIf jinjaIf -> {
                collectCssValue(jinjaIf.getThenValue());
                collectCssValue(jinjaIf.getElseValue());
            }
            default -> {
                if (node instanceof CssNode cssNode) {
                    collectNodes(cssNode.getChildren());
                } else if (node instanceof HtmlElement htmlElement) {
                    collectNodes(htmlElement.getChildren());
                }
            }
        }
    }

    private void collectCssStylesheet(CssStylesheet stylesheet) {
        if (stylesheet != null) {
            collectNodes(stylesheet.getContents());
        }
    }

    private void collectCssValue(CssValue value) {
        if (value == null) {
            return;
        }
        for (CssValuePart part : value.getParts()) {
            if (part instanceof CssJinjaValueIf jinjaIf) {
                collectCssValue(jinjaIf.getThenValue());
                collectCssValue(jinjaIf.getElseValue());
            }
        }
    }

    private void collectJinjaStmtDefinitions(JinjaStmt stmt) {
        switch (stmt) {
            case JinjaForStmt forStmt -> {
                ISymbolTable previous = activeTable;
                int previousSiblingIndex = siblingScopeIndex;
                activeTable = activeTable.enterScope("for");
                siblingScopeIndex = 0;
                for (String variable : forStmt.getVariables()) {
                    recordDefinition(variable, forStmt, activeTable, SymbolKind.VARIABLE);
                }
                collectNodes(forStmt.getBody().getBodyChildren());
                activeTable = previous;
                siblingScopeIndex = previousSiblingIndex;
            }
            case JinjaBlockStmt blockStmt -> {
                recordDefinition(blockStmt.getName(), blockStmt, activeTable, SymbolKind.BLOCK);
                ISymbolTable previous = activeTable;
                int previousSiblingIndex = siblingScopeIndex;
                activeTable = activeTable.enterScope("block:" + blockStmt.getName());
                siblingScopeIndex = 0;
                collectNodes(blockStmt.getBody().getBodyChildren());
                activeTable = previous;
                siblingScopeIndex = previousSiblingIndex;
            }
            case JinjaIfStmt ifStmt -> {
                collectNodes(ifStmt.getThenBody().getBodyChildren());
                for (JinjaElifClause elif : ifStmt.getElifClauses()) {
                    collectNodes(elif.getBody().getBodyChildren());
                }
                if (ifStmt.getElseBody() != null) {
                    collectNodes(ifStmt.getElseBody().getBodyChildren());
                }
            }
            case JinjaExtendsStmt extendsStmt -> recordTemplateDependency(
                    "extends", extendsStmt.getTemplateName(), extendsStmt.getSourceRange());
            case JinjaIncludeStmt includeStmt -> recordTemplateDependency(
                    "include", normalizeTemplateName(includeStmt.getTemplateName()),
                    includeStmt.getSourceRange());
            case JinjaSetStmt setStmt -> {
                recordDefinition(setStmt.getName(), setStmt, activeTable, SymbolKind.VARIABLE);
            }
            default -> {
            }
        }
    }

    // -------------------------------------------------------------------------
    // Pass 2
    // -------------------------------------------------------------------------

    private void collectNodes(List<? extends ASTNode> nodes) {
        if (nodes == null) {
            return;
        }
        for (ASTNode child : nodes) {
            collectDefinitions(child);
        }
    }

    private void resolveReferences(ASTNode node) {
        if (node == null) {
            return;
        }

        switch (node) {
            case HtmlDocument document -> resolveNodes(document.getChildrenNodes());
            case HtmlNormalElement element -> resolveNodes(element.getChildNodes());
            case HtmlSelfClosingElement ignored -> {
            }
            case HtmlVoidElement ignored -> {
            }
            case HtmlStyleElement style -> {
                for (ASTNode child : style.getChildren()) {
                    if (child instanceof CssStylesheet stylesheet) {
                        resolveReferences(stylesheet);
                    }
                }
            }
            case JinjaBody body -> resolveNodes(body.getBodyChildren());
            case JinjaExpr expr -> resolveJinjaExpr(expr);
            case JinjaStmt stmt -> resolveJinjaStmt(stmt);
            case CssStylesheet stylesheet -> resolveNodes(stylesheet.getContents());
            case CssRule rule -> resolveNodes(rule.getBlockContents());
            case CssDeclaration declaration -> resolveCssValue(declaration.getValue());
            case CssJinjaExpressionValue jinjaValue -> resolveJinjaExpr(jinjaValue.getExpr());
            case CssJinjaValueIf jinjaIf -> {
                resolveJinjaExpr(jinjaIf.getCondition());
                resolveCssValue(jinjaIf.getThenValue());
                resolveCssValue(jinjaIf.getElseValue());
            }
            default -> {
                if (node instanceof CssNode cssNode) {
                    resolveNodes(cssNode.getChildren());
                } else if (node instanceof HtmlElement htmlElement) {
                    resolveNodes(htmlElement.getChildren());
                }
            }
        }
    }

    private void resolveCssValue(CssValue value) {
        if (value == null) {
            return;
        }
        for (CssValuePart part : value.getParts()) {
            if (part instanceof CssJinjaExpressionValue jinjaPart) {
                resolveJinjaExpr(jinjaPart.getExpr());
            } else if (part instanceof CssJinjaValueIf jinjaIf) {
                resolveJinjaExpr(jinjaIf.getCondition());
                resolveCssValue(jinjaIf.getThenValue());
                resolveCssValue(jinjaIf.getElseValue());
            }
        }
    }

    private void resolveJinjaStmt(JinjaStmt stmt) {
        switch (stmt) {
            case JinjaForStmt forStmt -> {
                resolveJinjaExpr(forStmt.getIterable());
                ISymbolTable previous = activeTable;
                int previousSiblingIndex = siblingScopeIndex;
                activeTable = enterSiblingScope();
                int nextSiblingIndex = siblingScopeIndex;
                siblingScopeIndex = 0;
                resolveNodes(forStmt.getBody().getBodyChildren());
                activeTable = previous;
                siblingScopeIndex = nextSiblingIndex;
            }
            case JinjaBlockStmt blockStmt -> {
                ISymbolTable blockScope = findNamedChildScope("block:" + blockStmt.getName());
                if (blockScope == null) {
                    return;
                }
                ISymbolTable previous = activeTable;
                int previousSiblingIndex = siblingScopeIndex;
                activeTable = blockScope;
                siblingScopeIndex = 0;
                resolveNodes(blockStmt.getBody().getBodyChildren());
                activeTable = previous;
                siblingScopeIndex = previousSiblingIndex;
            }
            case JinjaIfStmt ifStmt -> {
                resolveJinjaExpr(ifStmt.getCondition());
                resolveNodes(ifStmt.getThenBody().getBodyChildren());
                for (JinjaElifClause elif : ifStmt.getElifClauses()) {
                    resolveJinjaExpr(elif.getCondition());
                    resolveNodes(elif.getBody().getBodyChildren());
                }
                if (ifStmt.getElseBody() != null) {
                    resolveNodes(ifStmt.getElseBody().getBodyChildren());
                }
            }
            case JinjaSetStmt setStmt -> resolveJinjaExpr(setStmt.getValue());
            case JinjaExtendsStmt ignored -> {
            }
            case JinjaIncludeStmt ignored -> {
            }
            default -> {
            }
        }
    }

    private void resolveJinjaExpr(JinjaExpr expr) {
        if (expr == null) {
            return;
        }

        switch (expr) {
            case JinjaIdentifierExpr identifier -> resolveIdentifierReference(identifier);
            case JinjaBinaryExpr binary -> {
                resolveJinjaExpr(binary.getLeft());
                resolveJinjaExpr(binary.getRight());
            }
            case JinjaUnaryExpr unary -> resolveJinjaExpr(unary.getExpr());
            case JinjaCallExpr call -> {
                resolveJinjaExpr(call.getCallee());
                for (JinjaExpr arg : call.getArgs()) {
                    resolveJinjaExpr(arg);
                }
            }
            case JinjaFilterExpr filter -> {
                resolveJinjaExpr(filter.getBase());
                for (JinjaExpr arg : filter.getArgs()) {
                    resolveJinjaExpr(arg);
                }
            }
            case JinjaAttrExpr attribute -> resolveJinjaExpr(attribute.getTarget());
            default -> {
                for (ASTNode child : expr.getChildren()) {
                    if (child instanceof JinjaExpr childExpr) {
                        resolveJinjaExpr(childExpr);
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private void resolveNodes(List<? extends ASTNode> nodes) {
        if (nodes == null) {
            return;
        }
        for (ASTNode child : nodes) {
            resolveReferences(child);
        }
    }

    private void recordDefinition(
            String name,
            ASTNode source,
            ISymbolTable table,
            SymbolKind kind) {
        if (name == null || name.isBlank()) {
            return;
        }

        Symbol symbol = new Symbol(name, kind, originFor(table));
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
                table,                // useScope
                table                 // definingScope
        ));
    }

    private void resolveIdentifierReference(JinjaIdentifierExpr identifier) {
        String name = identifier.getName();
        Optional<ScopeBinding> binding = NameResolver.resolve(activeTable, name);
        ResolutionStatus status = NameResolver.toStatus(binding);

        ISymbolTable definingScope = null;
        if (binding.isPresent()) {
            definingScope = binding.get().getDefiningScope();
        } else {
            for (SymbolReference def : referenceIndex.getDefinitions()) {
                if (name.equals(def.getName())) {
                    definingScope = def.getDefiningScope().orElse(null);
                    break;
                }
            }
        }

        referenceIndex.record(new SymbolReference(
                name,
                SymbolUseKind.REFERENCE,
                identifier.getSourceRange(),
                identifier,
                NameResolver.scopeName(activeTable),
                status,
                binding.map(ScopeBinding::getSymbol).orElse(null),
                binding.map(b -> NameResolver.scopeName(b.getDefiningScope())).orElse(null),
                activeTable,          // useScope
                definingScope  // definingScope
        ));
    }

    private void recordTemplateDependency(String linkKind, String templateName, SourceRange location) {
        referenceIndex.recordTemplateLink(linkKind, normalizeTemplateName(templateName), location);
    }

    private Optional<ScopeBinding> findOuterBinding(ISymbolTable table, String name) {
        ISymbolTable outer = NameResolver.parentOf(table);
        if (outer == null) {
            return Optional.empty();
        }
        return NameResolver.resolve(outer, name);
    }

    private ISymbolTable enterSiblingScope() {
        if (!(activeTable instanceof AbstractSymbolTable parent)) {
            return activeTable;
        }

        while (siblingScopeIndex < parent.children.size()) {
            ISymbolTable child = parent.children.get(siblingScopeIndex);
            String scopeName = NameResolver.scopeName(child);

            siblingScopeIndex++;

            if (scopeName != null && scopeName.startsWith("block:")) {
                continue;
            }

            return child;
        }

        return activeTable;
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

    private String originFor(ISymbolTable table) {
        if (table instanceof TemplateSymbolTable templateTable) {
            return templateTable.getTemplateName();
        }
        if (repository.getTemplateGlobal() instanceof TemplateSymbolTable templateTable) {
            return templateTable.getTemplateName();
        }
        return null;
    }
}
