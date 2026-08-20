package generator;

import AST.ASTNode;
import generator.context.ContextData;
import generator.context.RuntimeValue;
import generator.template.TemplateRenderer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Standalone smoke test for {@code {% extends %}} / {@code {% include %}} support.
 * <p>
 * Not part of the required {@code output/}/{@code compiler_output/} deliverable — these fixture
 * templates ({@code base_layout.html}, {@code extends_test.html}, {@code nav_partial.html},
 * {@code include_test.html}) exist purely to exercise template inheritance and inclusion in
 * isolation from the product-catalog templates.
 * <p>
 * In IntelliJ: right-click → Run 'ExtendsIncludeSmokeTest.main()'.
 */
public final class ExtendsIncludeSmokeTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== extends / include smoke test ===\n");

        Path templateDir = Paths.get("src/Tests/FinalTests").toAbsolutePath().normalize();

        // ── extends: child overrides base's blocks ──────────────────────────
        ContextData extendsCtx = new ContextData();
        extendsCtx.put("name", RuntimeValue.ofString("World"));

        ASTNode extendsAst = GenerationPipeline.parseTemplateHeadless(
                Files.readString(templateDir.resolve("extends_test.html")));
        System.out.println("--- extends_test.html parsed AST ---");
        System.out.println(AstJsonExporter.toJson(extendsAst));
        String extendsHtml = new TemplateRenderer(extendsCtx, templateDir).render(extendsAst);
        System.out.println("--- extends_test.html rendered output ---");
        System.out.println(extendsHtml);
        System.out.println();

        require(extendsHtml.contains("Extends Test"), "extends: child's <title> block override missing");
        require(extendsHtml.contains("Hello") && extendsHtml.contains("World"),
                "extends: child's content block override missing");
        require(!extendsHtml.contains("Untitled"), "extends: base's default title leaked through (override didn't apply)");
        require(!extendsHtml.contains("Default content"), "extends: base's default content leaked through (override didn't apply)");
        require(extendsHtml.contains("Products</a>"), "extends: base layout's own markup (nav) is missing");
        System.out.println("extends OK");
        System.out.println();

        // ── include: partial renders inline against the including page's context ──
        ContextData includeCtx = new ContextData();
        includeCtx.put("name", RuntimeValue.ofString("Laila"));

        ASTNode includeAst = GenerationPipeline.parseTemplateHeadless(
                Files.readString(templateDir.resolve("include_test.html")));
        String includeHtml = new TemplateRenderer(includeCtx, templateDir).render(includeAst);

        require(includeHtml.contains("Products</a>"), "include: nav_partial.html content is missing");
        require(includeHtml.contains("Laila"), "include: including page's own context variable didn't render");
        System.out.println("include OK:");
        System.out.println(includeHtml);
        System.out.println();

        System.out.println("=== ALL CHECKS PASSED ===");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
