import AST.ASTGraphvizPrinter;
import AST.ASTNode;
import AST.Program;
import AST.template.TemplateNode;
import AST.template.TemplateVisitor;
import AST.flask.visitor.ProgramVisitor;
import SymbolTable.FlaskSymbolTable;
import SymbolTable.FlaskSymbolTableBuilder;
import SymbolTable.ISymbolTable;
import SymbolTable.SymbolTableRepository;
import SymbolTable.TemplateSymbolTable;
import SymbolTable.TemplateSymbolTableBuilder;
import semantic.bridge.TemplateContextBridge;
import semantic.diagnostics.DiagnosticCollector;
import antlr.FlaskLexer;
import antlr.FlaskParser;
import antlr.TemplateLexer;
import antlr.TemplateParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;


public class Main {

    // Default input locations. Change these in one place when switching test data.
    private static final Path DEFAULT_FLASK_SOURCE = Paths.get("src/Tests/Flask/keyword-args-test.py").toAbsolutePath().normalize();
    private static final Path DEFAULT_TEMPLATE_SOURCE = Paths.get("src/Tests/Template/template1.html").toAbsolutePath().normalize();
    private static final Path DEFAULT_TEMPLATE_DIRECTORY = Paths.get("src/Tests/FinalTests").toAbsolutePath().normalize();
    private static final String FLASK_AST_OUTPUT = "ast-flask.dot";
    private static final String TEMPLATE_AST_OUTPUT = "ast-template.dot";

    private enum Mode {
        FLASK_ONLY,
        TEMPLATE_ONLY,
        FLASK_AND_TEMPLATES
    }

    public static void main(String[] args) throws Exception {
        Mode mode = args.length > 0 ? parseMode(args[0]) : askMode();

        switch (mode) {
            case FLASK_ONLY -> runFlaskOnly(args);
            case TEMPLATE_ONLY -> runTemplateOnly(args);
            case FLASK_AND_TEMPLATES -> runCombined(args);
        }
    }

    private static Mode parseMode(String rawMode) {
        String normalized = rawMode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "1", "flask", "flask-only" -> Mode.FLASK_ONLY;
            case "2", "template", "template-only" -> Mode.TEMPLATE_ONLY;
            case "3", "both", "combined", "flask+template" -> Mode.FLASK_AND_TEMPLATES;
            default -> throw new IllegalArgumentException("Unknown mode: " + rawMode);
        };
    }

    private static Mode askMode() {
        System.out.println("Choose execution mode:");
        System.out.println("1) Flask only");
        System.out.println("2) Template only");
        System.out.println("3) Flask + templates");
        System.out.print("Enter choice: ");

        try (Scanner scanner = new Scanner(System.in)) {
            String choice = scanner.nextLine().trim();
            return parseMode(choice);
        }
    }

    private static void runFlaskOnly(String[] args) throws Exception {
        Path flaskPath = resolvePath(args, 1, DEFAULT_FLASK_SOURCE);
        Program program = parseFlask(flaskPath);

        ASTGraphvizPrinter.print(program, FLASK_AST_OUTPUT);
        processFlaskSymbolTable(program, flaskPath);
    }

    private static void runTemplateOnly(String[] args) throws Exception {
        Path templatePath = resolvePath(args, 1, DEFAULT_TEMPLATE_SOURCE);
        ASTNode root = parseTemplate(templatePath);

        ASTGraphvizPrinter.print(root, TEMPLATE_AST_OUTPUT);
        processTemplateSymbolTable(root, templatePath);
    }

    private static void runCombined(String[] args) throws Exception {
        Path flaskPath = resolvePath(args, 1, DEFAULT_FLASK_SOURCE);
        List<Path> templatePaths = resolveTemplatePaths(args);

        Program flaskProgram = parseFlask(flaskPath);
        ASTGraphvizPrinter.print(flaskProgram, FLASK_AST_OUTPUT);

        FlaskSymbolTable flaskTable = new FlaskSymbolTable("flask-global", flaskPath.toString());
        SymbolTableRepository flaskOnlyRepo = new SymbolTableRepository(
                flaskTable,
                new TemplateSymbolTable("template-global", null)
        );
        FlaskSymbolTableBuilder flaskBuilder = new FlaskSymbolTableBuilder(flaskOnlyRepo);
        flaskBuilder.build(flaskProgram);
        System.out.println(flaskTable);
        System.out.println(flaskBuilder.getReferenceIndex().formatReport());

        for (Path templatePath : templatePaths) {
            ASTNode root = parseTemplate(templatePath);
            String outputName = "ast-" + safeFileStem(templatePath) + ".dot";
            ASTGraphvizPrinter.print(root, outputName);

            String templateName = templatePath.getFileName().toString();
            SymbolTableRepository repo = new SymbolTableRepository(
                    flaskTable,
                    new TemplateSymbolTable("template-global", templateName)
            );
            TemplateSymbolTableBuilder templateBuilder = new TemplateSymbolTableBuilder(repo);
            templateBuilder.buildTemplate(root);
            System.out.println(repo.getTemplateGlobal());
            System.out.println(templateBuilder.getReferenceIndex().formatReport());

            TemplateContextBridge bridge = new TemplateContextBridge(repo, new DiagnosticCollector());
            bridge.bridge(
                    flaskProgram,
                    (TemplateNode) root,
                    flaskBuilder.getReferenceIndex(),
                    templateBuilder.getReferenceIndex());
            System.out.println(bridge.formatReport());
        }

        System.out.println("Combined mode finished: Flask file + " + templatePaths.size() + " template file(s).");
    }

    private static Program parseFlask(Path path) throws Exception {
        CharStream input = CharStreams.fromPath(path);
        FlaskLexer lexer = new FlaskLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FlaskParser parser = new FlaskParser(tokens);
        ProgramVisitor visitor = new ProgramVisitor();
        return visitor.visitProg(parser.prog());
    }

    private static ASTNode parseTemplate(Path path) throws Exception {
        CharStream input = CharStreams.fromPath(path);
        TemplateLexer lexer = new TemplateLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TemplateParser parser = new TemplateParser(tokens);
        TemplateVisitor visitor = new TemplateVisitor();
        return visitor.visitTemplateRoot((TemplateParser.TemplateRootContext) parser.template());
    }

    private static Path resolvePath(String[] args, int index, Path fallback) {
        if (args.length > index && !args[index].isBlank()) {
            return Paths.get(args[index]).toAbsolutePath().normalize();
        }
        return fallback;
    }

    private static List<Path> resolveTemplatePaths(String[] args) {
        List<Path> paths = new ArrayList<>();

        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                paths.add(Paths.get(args[i]).toAbsolutePath().normalize());
            }
            return paths;
        }

        if (Files.isDirectory(DEFAULT_TEMPLATE_DIRECTORY)) {
            try (var stream = Files.list(DEFAULT_TEMPLATE_DIRECTORY)) {
                stream.filter(path -> path.toString().endsWith(".html"))
                        .forEach(paths::add);
            } catch (Exception ignored) {
                // Fall back to an empty list if the directory cannot be scanned.
            }
        }

        if (paths.isEmpty()) {
            paths.add(DEFAULT_TEMPLATE_SOURCE);
        }

        return paths;
    }

    private static String safeFileStem(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private static void processFlaskSymbolTable(Program program, Path sourcePath) {
        SymbolTableRepository repository = new SymbolTableRepository(
                new FlaskSymbolTable("flask-global", sourcePath.toString()),
                new TemplateSymbolTable("template-global", null)
        );
        FlaskSymbolTableBuilder builder = new FlaskSymbolTableBuilder(repository);
        ISymbolTable symbolTable = builder.build(program);
        System.out.println(symbolTable);
        System.out.println(builder.getReferenceIndex().formatReport());
    }

    private static void processTemplateSymbolTable(ASTNode root, Path sourcePath) {
        String templateName = sourcePath.getFileName().toString();
        SymbolTableRepository repository = new SymbolTableRepository(
                new FlaskSymbolTable("flask-global", null),
                new TemplateSymbolTable("template-global", templateName)
        );
        TemplateSymbolTableBuilder builder = new TemplateSymbolTableBuilder(repository);
        ISymbolTable symbolTable = builder.buildTemplate(root);
        System.out.println(symbolTable);
        System.out.println(builder.getReferenceIndex().formatReport());
    }

    private static void printSymbolTablePlaceholder(String kind, Path sourcePath) {
        System.out.println("[TODO] " + kind + " symbol table for " + sourcePath.getFileName()
                + " will be built and printed after Laila implements the builders.");
    }
}

