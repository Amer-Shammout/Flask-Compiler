package generator.context;

import AST.Program;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds Jinja {@link ContextData} by simulating Flask route logic from the AST.
 * <p>
 * Covers the three {@code render_template} call sites in {@code app.py}:
 * <ul>
 *   <li>{@code list_products} → products.html ({@code products=...})</li>
 *   <li>{@code product_details} → product_details.html ({@code product=...})</li>
 *   <li>{@code add_product} → add_product.html (empty context)</li>
 * </ul>
 * Does NOT use the Symbol Table.
 */
public final class PythonContextEvaluator {

    private PythonContextEvaluator() {
    }

    /**
     * Context for {@code products.html}:
     * {@code render_template("products.html", products=view_products)}
     */
    public static ContextData forProductsList(Program program) {
        ContextData ctx = new ContextData();
        ctx.put("products", RuntimeValue.ofList(loadProductObjects(program)));
        return ctx;
    }

    /**
     * Context for {@code product_details.html}:
     * {@code render_template("product_details.html", product=product)}
     * <p>
     * If {@code productId} is empty, uses the first product (static generation default).
     */
    public static ContextData forProductDetails(Program program, Optional<Long> productId) {
        List<RuntimeValue> products = loadProductObjects(program);
        RuntimeValue chosen = RuntimeValue.none();

        if (!products.isEmpty()) {
            if (productId.isPresent()) {
                long wanted = productId.get();
                for (RuntimeValue p : products) {
                    if (p.getAttr("id").asInt() == wanted) {
                        chosen = p;
                        break;
                    }
                }
            } else {
                chosen = products.get(0);
            }
        }

        ContextData ctx = new ContextData();
        ctx.put("product", chosen);
        return ctx;
    }

    /**
     * Context for {@code add_product.html}:
     * {@code return render_template("add_product.html")} — no variables passed.
     */
    public static ContextData forAddProduct(Program program) {
        // program kept for a uniform API; page has no Jinja context vars
        return new ContextData();
    }

    /** All Product objects derived from the global {@code products} list in app.py. */
    public static List<RuntimeValue> loadProductObjects(Program program) {
        ContextData globals = FlaskDataExtractor.extractGlobals(program);
        RuntimeValue rawProducts = globals.get("products");
        List<RuntimeValue> viewProducts = new ArrayList<>();
        if (rawProducts.getKind() == RuntimeValue.Kind.LIST) {
            for (RuntimeValue row : rawProducts.asList()) {
                viewProducts.add(rowToProduct(row));
            }
        }
        return viewProducts;
    }

    /**
     * Mirrors {@code Product(p[0], p[1], p[2], p[3])} in app.py.
     */
    public static RuntimeValue rowToProduct(RuntimeValue row) {
        if (row.getKind() != RuntimeValue.Kind.LIST) {
            throw new IllegalArgumentException("Product row must be a LIST, got " + row.getKind());
        }
        List<RuntimeValue> cells = row.asList();
        if (cells.size() < 4) {
            throw new IllegalArgumentException(
                    "Product row needs 4 values [id, name, price, in_stock], got " + cells.size());
        }

        long id = cells.get(0).asInt();
        String name = cells.get(1).asString();
        double price = cells.get(2).asFloat();
        boolean inStock = cells.get(3).asBool();
        return RuntimeValue.ofProduct(id, name, price, inStock);
    }
}
