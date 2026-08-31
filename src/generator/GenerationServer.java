package generator;

import AST.ASTNode;
import AST.flask.Program;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import generator.context.ContextData;
import generator.context.PythonContextEvaluator;
import generator.context.RuntimeValue;
import generator.template.TemplateRenderer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The "Java that listens for changes and does the regeneration."
 * <p>
 * Unlike {@link GenerationPipeline} (which renders {@code output/*.html} once as a batch job),
 * this is a live HTTP server: it seeds an in-memory product list once from {@code app.py}
 * (mirroring real Flask's in-memory list — mutations never write back to the source file),
 * then re-renders the already-parsed Jinja ASTs against the *current* in-memory data on every
 * request. Add / Delete / Edit therefore take effect immediately on refresh, with no need to
 * re-run the generator from IntelliJ.
 */
public final class GenerationServer {

    private final Path flaskSource;
    private final Path templateDirectory;
    private final int port;

    private final List<ProductRecord> products = Collections.synchronizedList(new ArrayList<>());

    private ASTNode productsTemplateAst;
    private ASTNode detailsTemplateAst;
    private ASTNode addTemplateAst;
    private ASTNode editTemplateAst;

    public GenerationServer(Path flaskSource, Path templateDirectory, int port) {
        this.flaskSource = flaskSource;
        this.templateDirectory = templateDirectory;
        this.port = port;
    }

    public void start() throws Exception {
        seedProductsFromFlask();
        loadTemplates();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        server.setExecutor(null);
        server.start();

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          Java Regeneration Server (Mode 5)                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        System.out.println("Listening on http://localhost:" + port + "/products");
        System.out.println("Add / Delete / Edit re-render the Jinja AST against updated data — just refresh the page.");
        System.out.println("Seeded " + products.size() + " product(s) from " + flaskSource.getFileName());
        System.out.println("(Ctrl+C to stop)");
    }

    // ── Setup ──────────────────────────────────────────────────────────────

    private void seedProductsFromFlask() throws Exception {
        Program program = GenerationPipeline.parseFlaskHeadless(flaskSource);
        List<RuntimeValue> productValues = PythonContextEvaluator.loadProductObjects(program);
        for (RuntimeValue v : productValues) {
            products.add(new ProductRecord(
                    v.getAttr("id").asInt(),
                    v.getAttr("name").asString(),
                    v.getAttr("price").asFloat(),
                    v.getAttr("in_stock").asBool()
            ));
        }
    }

    private void loadTemplates() throws Exception {
        productsTemplateAst = parseTemplateFile("products.html");
        detailsTemplateAst = parseTemplateFile("product_details.html");
        addTemplateAst = parseTemplateFile("add_product.html");
        editTemplateAst = parseTemplateFile("edit_product.html");
    }

    private ASTNode parseTemplateFile(String name) throws Exception {
        String source = Files.readString(templateDirectory.resolve(name));
        return GenerationPipeline.parseTemplateHeadless(source);
    }

    // ── Routing ────────────────────────────────────────────────────────────

    private void handle(HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();
            switch (path) {
                case "/" -> redirect(exchange, "/products");
                case "/products" -> serveProductsList(exchange);
                case "/products/details" -> serveProductDetails(exchange);
                case "/products/add" -> serveAddForm(exchange);
                case "/products/create" -> handleCreate(exchange);
                case "/products/delete" -> handleDelete(exchange);
                case "/products/edit" -> serveEditForm(exchange);
                case "/products/update" -> handleUpdate(exchange);
                // No /style.css route: the compiled <style> block ships inside each page.
                case "/script.js" -> serveStaticFile(exchange, templateDirectory.resolve("script.js"), "application/javascript");
                default -> respond(exchange, 404, "text/plain", "Not Found: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                respond(exchange, 500, "text/plain", "Server error: " + e);
            } catch (IOException ignored) {
            }
        }
    }

    // ── Read routes (each regenerates HTML from the current in-memory data) ─

    private void serveProductsList(HttpExchange exchange) throws IOException {
        ContextData ctx = new ContextData();
        ctx.put("products", RuntimeValue.ofList(currentProductValues()));
        respond(exchange, 200, "text/html", new TemplateRenderer(ctx, templateDirectory).render(productsTemplateAst));
    }

    private void serveProductDetails(HttpExchange exchange) throws IOException {
        Long id = optionalId(exchange);
        if (id == null) {
            respond(exchange, 200, "text/plain", "Missing product id");
            return;
        }
        Optional<ProductRecord> found = findById(id);
        if (found.isEmpty()) {
            respond(exchange, 200, "text/plain", "Invalid Product");
            return;
        }
        ContextData ctx = new ContextData();
        ctx.put("product", toRuntimeValue(found.get()));
        respond(exchange, 200, "text/html", new TemplateRenderer(ctx, templateDirectory).render(detailsTemplateAst));
    }

    private void serveAddForm(HttpExchange exchange) throws IOException {
        respond(exchange, 200, "text/html", new TemplateRenderer(new ContextData(), templateDirectory).render(addTemplateAst));
    }

    private void serveEditForm(HttpExchange exchange) throws IOException {
        Long id = optionalId(exchange);
        if (id == null) {
            respond(exchange, 200, "text/plain", "Missing product id");
            return;
        }
        Optional<ProductRecord> found = findById(id);
        if (found.isEmpty()) {
            respond(exchange, 200, "text/plain", "Invalid Product");
            return;
        }
        ContextData ctx = new ContextData();
        ctx.put("product", toRuntimeValue(found.get()));
        respond(exchange, 200, "text/html", new TemplateRenderer(ctx, templateDirectory).render(editTemplateAst));
    }

    // ── Mutating routes (the actual "listen for changes" part) ─────────────

    private void handleCreate(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseForm(readBody(exchange));
        String name = form.getOrDefault("name", "");
        double price = parseDoubleSafe(form.get("price"));
        synchronized (products) {
            long maxId = 0;
            for (ProductRecord p : products) {
                maxId = Math.max(maxId, p.id);
            }
            products.add(new ProductRecord(maxId + 1, name, price, true));
        }
        redirect(exchange, "/products");
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        Long id = optionalId(exchange);
        if (id != null) {
            synchronized (products) {
                products.removeIf(p -> p.id == id);
            }
        }
        redirect(exchange, "/products");
    }

    private void handleUpdate(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseForm(readBody(exchange));
        long id = Long.parseLong(form.get("id"));
        synchronized (products) {
            for (ProductRecord p : products) {
                if (p.id == id) {
                    p.name = form.getOrDefault("name", p.name);
                    p.price = parseDoubleSafe(form.get("price"));
                    p.inStock = form.containsKey("in_stock"); // checkbox only sent when checked
                    break;
                }
            }
        }
        redirect(exchange, "/products");
    }

    // ── Static files ─────────────────────────────────────────────────────

    private void serveStaticFile(HttpExchange exchange, Path file, String contentType) throws IOException {
        if (!Files.isRegularFile(file)) {
            respond(exchange, 404, "text/plain", "Not Found");
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ── HTTP helpers ─────────────────────────────────────────────────────

    private void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Long optionalId(HttpExchange exchange) {
        String raw = parseForm(exchange.getRequestURI().getRawQuery()).get("id");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Map<String, String> parseForm(String encoded) {
        Map<String, String> result = new LinkedHashMap<>();
        if (encoded == null || encoded.isBlank()) {
            return result;
        }
        for (String pair : encoded.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            result.put(urlDecode(key), urlDecode(value));
        }
        return result;
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static double parseDoubleSafe(String raw) {
        try {
            return raw == null ? 0.0 : Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // ── In-memory data model ─────────────────────────────────────────────

    private List<RuntimeValue> currentProductValues() {
        List<RuntimeValue> values = new ArrayList<>();
        synchronized (products) {
            for (ProductRecord p : products) {
                values.add(toRuntimeValue(p));
            }
        }
        return values;
    }

    private Optional<ProductRecord> findById(long id) {
        synchronized (products) {
            return products.stream().filter(p -> p.id == id).findFirst();
        }
    }

    private static RuntimeValue toRuntimeValue(ProductRecord p) {
        return RuntimeValue.ofProduct(p.id, p.name, p.price, p.inStock);
    }

    private static final class ProductRecord {
        final long id;
        String name;
        double price;
        boolean inStock;

        ProductRecord(long id, String name, double price, boolean inStock) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.inStock = inStock;
        }
    }
}
