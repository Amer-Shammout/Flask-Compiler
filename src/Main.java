import AST.ASTGraphvizPrinter;
import AST.ASTNode;
import AST.flask.Program;
import AST.template.TemplateNode;
import AST.template.TemplateVisitor;
import AST.flask.visitor.ProgramVisitor;
import SymbolTable.FlaskSymbolTable;
import SymbolTable.FlaskSymbolTableBuilder;
import SymbolTable.ISymbolTable;
import SymbolTable.SymbolTableRepository;
import SymbolTable.TemplateSymbolTable;
import SymbolTable.TemplateSymbolTableBuilder;
import org.antlr.v4.runtime.tree.ParseTree;
import semantic.analyzers.SemanticAnalysisPipeline;
import semantic.diagnostics.ColoredLogger;
import semantic.diagnostics.DiagnosticCollector;
import semantic.diagnostics.Diagnostic;
import semantic.diagnostics.ErrorCode;
import generator.GenerationPipeline;
import antlr.FlaskLexer;
import antlr.FlaskParser;
import antlr.TemplateLexer;
import antlr.TemplateParser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;


public class Main {

    private static final Path DEFAULT_FLASK_SOURCE = Paths.get("src/Tests/FinalTests/app.py").toAbsolutePath().normalize();
    private static final Path DEFAULT_TEMPLATE_SOURCE = Paths.get("src/Tests/FinalTests/products.html").toAbsolutePath().normalize();
    private static final Path DEFAULT_TEMPLATE_DIRECTORY = Paths.get("src/Tests/FinalTests").toAbsolutePath().normalize();
    private static final String FLASK_AST_OUTPUT = "ast-flask.dot";
    private static final String TEMPLATE_AST_OUTPUT = "ast-template.dot";

    private enum Mode {
        FLASK_ONLY, TEMPLATE_ONLY, FLASK_AND_TEMPLATES, GENERATE, SERVE
    }

    static class NodeData {
        String label;
        ParseTree tree;

        NodeData(String label, ParseTree tree) {
            this.label = label;
            this.tree = tree;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static void main(String[] args) throws Exception {
        Mode mode = args.length > 0 ? parseMode(args[0]) : askMode();
        switch (mode) {
            case FLASK_ONLY -> runFlaskOnly(args);
            case TEMPLATE_ONLY -> runTemplateOnly(args);
            case FLASK_AND_TEMPLATES -> runCombined(args);
            case GENERATE -> runGenerate(args);
            case SERVE -> runServer(args);
        }
    }

    private static Mode parseMode(String rawMode) {
        String normalized = rawMode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "1", "flask", "flask-only" -> Mode.FLASK_ONLY;
            case "2", "template", "template-only" -> Mode.TEMPLATE_ONLY;
            case "3", "both", "combined", "flask+template" -> Mode.FLASK_AND_TEMPLATES;
            case "4", "generate", "codegen", "generation" -> Mode.GENERATE;
            case "5", "server", "serve" -> Mode.SERVE;
            default -> throw new IllegalArgumentException("Unknown mode: " + rawMode);
        };
    }

    private static Mode askMode() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                Flask Compiler - Execution Mode             ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        System.out.println("Choose execution mode:");
        System.out.println("  1) Flask only (with TypeError detection)");
        System.out.println("  2) Flask + single template (with cross-context type checking)");
        System.out.println("  3) Flask + all templates (with cross-context type checking)");
        System.out.println("  4) Generate HTML (Code Generation → output/)");
        System.out.println("  5) Start Interactive Server (Java regenerates on Add/Delete/Edit)");
        System.out.print("\nEnter choice (1-5): ");

        try (Scanner scanner = new Scanner(System.in)) {
            String choice = scanner.nextLine().trim();
            return parseMode(choice);
        }
    }

    /**
     * MODE 5: Interactive server — Java listens for Add/Delete/Edit requests and
     * regenerates the Jinja AST against the updated in-memory product data on every request.
     * <p>
     * ✅ VALIDATION: Before starting the server, perform complete semantic analysis.
     * If any [E...] errors found → STOP and don't start server.
     */
    private static void runServer(String[] args) throws Exception {
        Path flaskPath = resolvePath(args, 1, DEFAULT_FLASK_SOURCE);
        Path templateDir = resolvePath(args, 2, DEFAULT_TEMPLATE_DIRECTORY);
        int port = 5001;

        // ════════════════════════════════════════════════════════════════════════════════
        // Semantic Validation (BEFORE starting server)
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║       Pre-validation: Semantic Analysis (Mode 5)          ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // Step 1: Parse Flask
        Program flaskProgram = GenerationPipeline.parseFlaskHeadless(flaskPath);
        System.out.println("✓ Flask parsed successfully");

        // Step 2: Build Flask Symbol Table
        FlaskSymbolTable flaskTable = new FlaskSymbolTable("flask-global", flaskPath.toString());
        SymbolTableRepository repository = new SymbolTableRepository(flaskTable, new TemplateSymbolTable("template-global", null));
        FlaskSymbolTableBuilder flaskBuilder = new FlaskSymbolTableBuilder(repository);
        flaskBuilder.build(flaskProgram);
        System.out.println("✓ Flask symbol table built\n");

        // Step 3: Run Flask Semantic Analysis
        DiagnosticCollector flaskDiagnostics = new DiagnosticCollector();
        SemanticAnalysisPipeline flaskPipeline = new SemanticAnalysisPipeline(repository, flaskDiagnostics, flaskBuilder);
        flaskPipeline.analyzeFlaskOnly(flaskProgram);
        System.out.println("📋 Flask analysis complete\n");

        // Step 4: Check for Flask errors
        if (flaskDiagnostics.getErrorCount() > 0) {
            System.out.println("❌ SEMANTIC ERRORS IN FLASK:\n");
            flaskDiagnostics.reportAll();
            System.out.println("\n❌ Server startup BLOCKED. " + "Fix Flask errors before running Mode 5.\n");
            return;
        }

        // Step 5: Analyze all Templates
        System.out.println("═".repeat(70));
        System.out.println("Checking Templates...\n");
        System.out.println("═".repeat(70) + "\n");

        List<DiagnosticCollector> templateDiagnosticsList = new ArrayList<>();

        List<String> templateNamesList = new ArrayList<>();

        boolean hasTemplateErrors = false;

        try (var stream = Files.list(templateDir)) {

            List<Path> templateFiles = stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".html")).sorted().toList();

            for (Path templatePath : templateFiles) {

                String templateName = templatePath.getFileName().toString();

                System.out.println("→ Analyzing: " + templateName);

                // Parse template
                String source = Files.readString(templatePath);

                ASTNode templateRoot = GenerationPipeline.parseTemplateHeadless(source);

                if (!(templateRoot instanceof TemplateNode templateNode)) {
                    System.out.println("  ⚠ Template parse failed: not a TemplateNode\n");
                    continue;
                }

                // Build template symbol table
                TemplateSymbolTable templateTable = new TemplateSymbolTable("template-global", templateName);

                SymbolTableRepository templateRepo = new SymbolTableRepository(flaskTable, templateTable);

                TemplateSymbolTableBuilder templateBuilder = new TemplateSymbolTableBuilder(templateRepo);

                templateBuilder.buildTemplate(templateNode);

                // Semantic analysis
                DiagnosticCollector templateDiag = new DiagnosticCollector();

                SemanticAnalysisPipeline templatePipeline = new SemanticAnalysisPipeline(templateRepo, templateDiag);

                templatePipeline.analyzeTemplateOnly(templateNode, templateBuilder.getReferenceIndex());

                templatePipeline.bridgeTemplateWithFlask(flaskProgram, templateNode, templateBuilder.getReferenceIndex());

                templateDiagnosticsList.add(templateDiag);
                templateNamesList.add(templateName);

                // Check for errors
                if (templateDiag.getErrorCount() > 0) {

                    System.out.println("  ❌ " + templateName + " has " + templateDiag.getErrorCount() + " error(s)");

                    hasTemplateErrors = true;

                } else {

                    System.out.println("  ✓ " + templateName + " OK\n");
                }
            }
        }

        // Step 6: Report all template diagnostics if any errors found
        if (hasTemplateErrors) {

            System.out.println("\n❌ SEMANTIC ERRORS IN TEMPLATES:\n");

            System.out.println("═".repeat(70) + "\n");

            for (int i = 0; i < templateDiagnosticsList.size(); i++) {

                if (templateDiagnosticsList.get(i).getErrorCount() > 0) {

                    System.out.println("📄 " + templateNamesList.get(i) + ":");

                    System.out.println("─".repeat(70));

                    templateDiagnosticsList.get(i).reportAll();

                    System.out.println();
                }
            }

            System.out.println("❌ Server startup BLOCKED. " + "Fix template errors before running Mode 5.\n");

            return;
        }

        // ════════════════════════════════════════════════════════════════════════════════
        // All validations passed → start server
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("\n" + "═".repeat(70));

        System.out.println("✅ All semantic validations PASSED");

        System.out.println("═".repeat(70));

        System.out.println("\nStarting Interactive Server...\n");

        new generator.GenerationServer(flaskPath, templateDir, port).start();
    }

    /**
     * MODE 4: Code Generation — Flask data + Jinja templates → output/*.html
     */
    private static void runGenerate(String[] args) throws Exception {
        Path flaskPath = resolvePath(args, 1, DEFAULT_FLASK_SOURCE);
        Path templateDir = resolvePath(args, 2, DEFAULT_TEMPLATE_DIRECTORY);
        Path outputDir = Paths.get("output").toAbsolutePath().normalize();
        Path compilerOutputDir = Paths.get("compiler_output").toAbsolutePath().normalize();

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                   Code Generation Mode                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        System.out.println("Flask:     " + flaskPath);
        System.out.println("Templates: " + templateDir);
        System.out.println("Output:    " + outputDir);

        GenerationPipeline.Result result = new GenerationPipeline(flaskPath, templateDir, outputDir, compilerOutputDir).run();

        System.out.println("\nGenerated files:");
        result.generatedHtml().forEach((name, path) -> System.out.println("  ✓ " + name + " → " + path));
        System.out.println("\nAlso copied support files into output/ (e.g. app.py).");
        System.out.println("See compiler_output/generation_log.txt for details.");
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════════
     * MODE 1: Flask Only
     * ════════════════════════════════════════════════════════════════════════════════
     * <p>
     * Flow:
     * 1. Parse Flask
     * 2. Build Flask Symbol Table          ← ONCE PER PIPELINE
     * 3. Run Flask Semantic Pipeline       ← Phase 1 only
     * <p>
     * NOTE: Symbol table is built exactly ONCE, then passed to analyzer.
     * Analyzer does NOT build again.
     */
    private static void runFlaskOnly(String[] args) throws Exception {
        Path flaskPath = resolvePath(args, 1, DEFAULT_FLASK_SOURCE);

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                      Flask Compilation                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        System.out.println("📂 Source File: " + flaskPath.getFileName());
        System.out.println("📍 Path: " + flaskPath);

        // ════════════════════════════════════════════════════════════════════════════════
        // Step 1: Parse Flask
        // ════════════════════════════════════════════════════════════════════════════════
        Program program = parseFlask(flaskPath);

        System.out.println("\n" + "═".repeat(70));
        System.out.println("🌿 Step 1: AST Generation");
        System.out.println("═".repeat(70));
        System.out.println("🚀 AST Parsing Status: Completed Successfully.");

        ASTGraphvizPrinter.print(program, FLASK_AST_OUTPUT);
        System.out.println("📝 Visualization saved to: " + FLASK_AST_OUTPUT);

        // ════════════════════════════════════════════════════════════════════════════════
        // Step 2: Build Flask Symbol Table (ONCE - BEFORE semantic pipeline)
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("\n" + "═".repeat(70));
        System.out.println("📊 Step 2: Flask Symbol Table Building");
        System.out.println("═".repeat(70) + "\n");

        SymbolTableRepository repository = new SymbolTableRepository(new FlaskSymbolTable("flask-global", flaskPath.toString()), new TemplateSymbolTable("template-global", null));

        FlaskSymbolTableBuilder flaskBuilder = new FlaskSymbolTableBuilder(repository);
        flaskBuilder.build(program);

        System.out.println("📌 [Flask Global Table Summary]:");
        System.out.println(repository.getFlaskGlobal());
        System.out.println("\n🔍 [Reference Index Report]:");
        System.out.println(flaskBuilder.getReferenceIndex().formatReport());

        // ════════════════════════════════════════════════════════════════════════════════
        // Step 3: Run Flask Semantic Analysis Pipeline (Phase 1 only)
        // ═════════════════════════════════  ══════════════════════════════════════════════
        System.out.println("\n" + "═".repeat(70));
        System.out.println("🛡️  Step 3: Flask Semantic Analysis");
        System.out.println("═".repeat(70) + "\n");

        DiagnosticCollector diagnostics = new DiagnosticCollector();
        SemanticAnalysisPipeline pipeline = new SemanticAnalysisPipeline(repository, diagnostics, flaskBuilder);

        // Phase 1: Flask analysis (NO symbol table building here!)
        pipeline.analyzeFlaskOnly(program);

        System.out.println("\n📋 [Diagnostic Summary Report]:");
        // Use the collector's reportAll() which delegates to ColoredLogger (centralized formatting)
        diagnostics.reportAll();
        System.out.println("\n" + "═".repeat(70) + "\n");
    }

    /**
     * ═══════════════════════════════════════════════════   ════════════════════════════
     * MODE 2: Flask + Single Template with Bridge
     * ════════════════════════════════════════════════════════════════════════════════
     * <p>
     * Flow:
     * Phase 1: Flask analysis
     * 1.1. Parse Flask
     * 1.2. Build Flask Symbol Table            ← ONCE, BEFORE pipeline
     * 1.3. Run Flask semantic checks
     * <p>
     * Phase 2: Template analysis
     * 2.1. Parse Template
     * 2.2. Build Template Symbol Table         ← ONCE, BEFORE pipeline
     * 2.3. Run Template semantic checks
     * <p>
     * Phase 3: Cross-context bridging
     * 3.1. Run Flask ↔ Template bridge
     * <p>
     * CRITICAL: Symbol tables are built in Main, NOT inside analyzers!
     */
    private static void runTemplateOnly(String[] args) throws Exception {
        Path templatePath = resolvePath(args, 1, DEFAULT_TEMPLATE_SOURCE);

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          Flask + Template Analysis Mode (Case 2)           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        System.out.println("📂 Template File: " + templatePath.getFileName());
        System.out.println("📂 Flask File: " + DEFAULT_FLASK_SOURCE.getFileName());
        System.out.println("📍 Template Path: " + templatePath);
        System.out.println("📍 Flask Path: " + DEFAULT_FLASK_SOURCE);

        // ════════════════════════════════════════════════════════════════════════════════
        // PHASE 1: Parse Flask & Build Symbol Table
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("\n" + "═".repeat(70));
        System.out.println("⚙️  Phase 1: Flask Parsing & Symbol Table Building");
        System.out.println("═".repeat(70));

        Program flaskProgram = parseFlask(DEFAULT_FLASK_SOURCE);
        System.out.println("✓ Flask parsed successfully");

        FlaskSymbolTable flaskTable = new FlaskSymbolTable("flask-global", DEFAULT_FLASK_SOURCE.toString());
        SymbolTableRepository repository = new SymbolTableRepository(flaskTable, new TemplateSymbolTable("template-global", null));

        System.out.println("🔨 Building Flask symbol table...");
        FlaskSymbolTableBuilder flaskBuilder = new FlaskSymbolTableBuilder(repository);
        flaskBuilder.build(flaskProgram);

        System.out.println("✓ Flask symbol table built\n");
        System.out.println("📌 [Flask Table Summary]:");
        System.out.println(flaskTable);
        System.out.println("\n🔍 [Flask Reference Index Report]:");
        System.out.println(flaskBuilder.getReferenceIndex().formatReport());

        // ════════════════════════════════════════════════════════════════════════════════
        // PHASE 1b: Run Flask Semantic Analysis (populate types)
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("\n" + "─".repeat(70));
        System.out.println("→ Running Flask Semantic Analysis (populates Symbol.inferredType)");
        System.out.println("─".repeat(70));

        DiagnosticCollector flaskDiagnostics = new DiagnosticCollector();
        SemanticAnalysisPipeline flaskPipeline = new SemanticAnalysisPipeline(repository, flaskDiagnostics, flaskBuilder);
        flaskPipeline.analyzeFlaskOnly(flaskProgram);

        System.out.println("✓ Flask semantic analysis complete - types now in Symbol Table\n");

        // ════════════════════════════════════════════════════════════════════════════════
        // PHASE 2: Parse Template & Build Symbol Table
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("═".repeat(70));
        System.out.println("⚙️  Phase 2: Template Parsing & Symbol Table Building");
        System.out.println("═".repeat(70));

        ASTNode root = parseTemplate(templatePath);

        System.out.println("\n" + "═".repeat(70));
        System.out.println("🌿 Template AST Generation");
        System.out.println("═".repeat(70));
        ASTGraphvizPrinter.print(root, TEMPLATE_AST_OUTPUT);
        System.out.println("📝 Template AST saved to: " + TEMPLATE_AST_OUTPUT);

        System.out.println("\n" + "═".repeat(70));
        System.out.println("📊 Template Symbol Table Processing");
        System.out.println("═".repeat(70) + "\n");

        String templateName = templatePath.getFileName().toString();
        // CRITICAL: Create new repository with Flask table (already typed) + new Template table
        repository = new SymbolTableRepository(flaskTable,  // Reuse Flask table with populated types
                new TemplateSymbolTable("template-global", templateName));

        System.out.println("🔨 Building template symbol table...");
        TemplateSymbolTableBuilder templateBuilder = new TemplateSymbolTableBuilder(repository);
        ISymbolTable symbolTable = templateBuilder.buildTemplate(root);

        System.out.println("✓ Template symbol table built\n");
        System.out.println("📌 [Template Symbol Table Summary]:");
        System.out.println(symbolTable);
        System.out.println("\n🔍 [Template Reference Index]:");
        System.out.println(templateBuilder.getReferenceIndex().formatReport());

        // ════════════════════════════════════════════════════════════════════════════════
        // PHASE 2b: Run Template Semantic Analysis (uses Flask types)
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("\n" + "─".repeat(70));
        System.out.println("→ Running Template Semantic Analysis");
        System.out.println("─".repeat(70));

        DiagnosticCollector templateDiagnostics = new DiagnosticCollector();
        SemanticAnalysisPipeline templatePipeline = new SemanticAnalysisPipeline(repository, templateDiagnostics);

        if (root instanceof TemplateNode templateRoot) {
            templatePipeline.analyzeTemplateOnly(templateRoot, templateBuilder.getReferenceIndex());
            System.out.println("✓ Template semantic analysis complete\n");

            // ════════════════════════════════════════════════════════════════════════════════
            // PHASE 3: Cross-Context Bridge Analysis
            // ════════════════════════════════════════════════════  ═══════════════════════════
            System.out.println("═".repeat(70));
            System.out.println("🛡️  Phase 3: Cross-Context Bridge Analysis");
            System.out.println("═".repeat(70));

            System.out.println("🔗 Running Flask ↔ Template bridge (cross-context type checking)...\n");
            templatePipeline.bridgeTemplateWithFlask(flaskProgram, templateRoot, templateBuilder.getReferenceIndex());

            System.out.println("✓ Cross-context type checking complete");
            System.out.println("\n🔗 [Context Bridge Integration]:");
            System.out.println(templatePipeline.getContextBridge().formatReport());
        } else {
            templateDiagnostics.addDiagnostic(new Diagnostic(null, ErrorCode.H001_SUGGESTION, "Template semantic analysis skipped: Template root is not a TemplateNode.", "Ensure parser/visitor produces a TemplateNode."));
        }

        // ════════════════════════════════════════════════════════════════════════════════
        // Display all diagnostics
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("\n📋 [Diagnostic Summary Report]:");
        System.out.println("\n" + "═".repeat(70));
        System.out.println(ColoredLogger.color("semantic.diagnostics.ColoredLogger", "36;1", "📊 Flask Diagnostics"));
        System.out.println("═".repeat(70));
        // Use collector's centralized reporting (which uses ColoredLogger internally)
        flaskDiagnostics.reportAll();

        System.out.println("\n" + "═".repeat(70));
        System.out.println(ColoredLogger.color("semantic.diagnostics.ColoredLogger", "36;1", "📊 Template: " + templatePath.getFileName() + " Diagnostics"));
        System.out.println("═".repeat(70));
        templateDiagnostics.reportAll();

        System.out.println("\n" + "═".repeat(70) + "\n");
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════════
     * MODE 3: Flask + All Templates with Bridge
     * ════════════════════════════════════════════════════════════════════════════════
     * <p>
     * Flow:
     * PHASE 1: Flask analysis (runs ONCE)
     * 1.1. Parse Flask
     * 1.2. Build Flask Symbol Table            ← ONCE, shared across all templates
     * 1.3. Run Flask semantic checks
     * <p>
     * FOR EACH TEMPLATE:
     * PHASE 2: Template analysis
     * 2.1. Parse Template
     * 2.2. Build Template Symbol Table (fresh)  ← Per template
     * 2.3. Run Template semantic checks
     * <p>
     * PHASE 3: Cross-context bridging
     * 3.1. Run Flask ↔ This Template bridge
     * <p>
     * CRITICAL: Flask analysis happens ONCE, templates reuse Flask types!
     */
    private static void runCombined(String[] args) throws Exception {
        Path flaskPath = resolvePath(args, 1, DEFAULT_FLASK_SOURCE);
        List<Path> templatePaths = resolveTemplatePaths(args);

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║             Combined Execution: Flask + Templates          ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        System.out.println("📂 Base Flask File: " + flaskPath.getFileName());
        System.out.println("📁 Templates Found: " + templatePaths.size() + " file(s)");

        // ════════════════════════════════════════════════════════════════════════════════
        // PHASE 1: Flask Parsing & Symbol Table Building (HAPPENS ONCE)
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("\n" + "═".repeat(70));
        System.out.println("⚙️  Phase 1: Flask Parsing & Symbol Table Building");
        System.out.println("═".repeat(70));

        Program flaskProgram = parseFlask(flaskPath);
        ASTGraphvizPrinter.print(flaskProgram, FLASK_AST_OUTPUT);
        System.out.println("📝 Base Flask AST saved to: " + FLASK_AST_OUTPUT);

        FlaskSymbolTable flaskTable = new FlaskSymbolTable("flask-global", flaskPath.toString());
        SymbolTableRepository repository = new SymbolTableRepository(flaskTable, new TemplateSymbolTable("template-global", null));

        System.out.println("🔨 Building Flask symbol table...");
        FlaskSymbolTableBuilder flaskBuilder = new FlaskSymbolTableBuilder(repository);
        flaskBuilder.build(flaskProgram);

        System.out.println("✓ Flask symbol table built\n");
        System.out.println("📌 [Flask Table Context]:");
        System.out.println(flaskTable);
        System.out.println("\n🔍 [Flask Reference Index Report]:");
        System.out.println(flaskBuilder.getReferenceIndex().formatReport());

        // ════════════════════════════════════════════════════════════════════════════════
        // PHASE 1b: Run Flask Semantic Analysis ONCE (populate types for all templates)
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("\n" + "─".repeat(70));
        System.out.println("→ Running Flask Semantic Analysis (populates types for all templates)");
        System.out.println("─".repeat(70));

        DiagnosticCollector globalFlaskDiagnostics = new DiagnosticCollector();
        SemanticAnalysisPipeline flaskPipeline = new SemanticAnalysisPipeline(repository, globalFlaskDiagnostics, flaskBuilder);
        flaskPipeline.analyzeFlaskOnly(flaskProgram);

        System.out.println("✓ Flask type inference complete - Symbol Table ready for templates\n");

        // ════════════════════════════════════════════════════════════════════════════════
        // FOR EACH TEMPLATE: Parse, Build, Analyze, Bridge
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("═".repeat(70));
        System.out.println("⚙️  Phase 2+3: Processing Associated Template Files");
        System.out.println("═".repeat(70));

        List<DiagnosticCollector> templateDiagnosticsList = new ArrayList<>();
        List<String> templateNamesList = new ArrayList<>();
        int index = 1;

        for (Path templatePath : templatePaths) {
            System.out.println("\n" + "─".repeat(50));
            System.out.println("📄 [" + index++ + "/" + templatePaths.size() + "] Processing: " + templatePath.getFileName());
            System.out.println("─".repeat(50));

            // Parse template
            ASTNode root = parseTemplate(templatePath);
            String outputName = "ast-" + safeFileStem(templatePath) + ".dot";
            ASTGraphvizPrinter.print(root, outputName);
            System.out.println("📝 Template AST graph saved as: " + outputName);

            // Build template symbol table (fresh, per template)
            String templateName = templatePath.getFileName().toString();
            SymbolTableRepository templateRepository = new SymbolTableRepository(flaskTable,  // Reuse Flask table (already typed from Phase 1)
                    new TemplateSymbolTable("template-global", templateName));

            System.out.println("🔨 Building template symbol table...");
            TemplateSymbolTableBuilder templateBuilder = new TemplateSymbolTableBuilder(templateRepository);
            templateBuilder.buildTemplate(root);

            System.out.println("✓ Template symbol table built\n");
            System.out.println("📊 [Template Local Table]:");
            System.out.println(templateRepository.getTemplateGlobal());
            System.out.println("\n🔍 [Template Reference Index]:");
            System.out.println(templateBuilder.getReferenceIndex().formatReport());

            // Semantic analysis for this template
            DiagnosticCollector diagnostics = new DiagnosticCollector();
            SemanticAnalysisPipeline templatePipeline = new SemanticAnalysisPipeline(templateRepository, diagnostics);

            if (root instanceof TemplateNode templateRoot) {
                System.out.println("→ Template Semantic Analysis");
                templatePipeline.analyzeTemplateOnly(templateRoot, templateBuilder.getReferenceIndex());

                System.out.println("→ Flask-Template Bridge (Cross-Context Type Checking)");
                templatePipeline.bridgeTemplateWithFlask(flaskProgram, templateRoot, templateBuilder.getReferenceIndex());

                System.out.println("\n🔗 [Context Bridge Integration]:");
                System.out.println(templatePipeline.getContextBridge().formatReport());
            } else {
                diagnostics.addDiagnostic(new Diagnostic(null, ErrorCode.H001_SUGGESTION, "Template semantic analysis skipped: Template root is not a TemplateNode.", "Ensure parser/visitor produces a TemplateNode."));
            }

            templateDiagnosticsList.add(diagnostics);
            templateNamesList.add(templateName);
        }

        // ════════════════════════════════════════════════════════════════════════════════
        // Display all diagnostics: Flask first, then each template
        // ════════════════════════════════════════════════════════════════════════════════
        System.out.println("\n" + "═".repeat(70));
        System.out.println(ColoredLogger.color("semantic.diagnostics.ColoredLogger", "36;1", "📊 Flask Diagnostics"));
        System.out.println("═".repeat(70));
        // Use centralized collector reporting (which uses ColoredLogger internally)
        globalFlaskDiagnostics.reportAll();

        for (int i = 0; i < templateDiagnosticsList.size(); i++) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println(ColoredLogger.color("semantic.diagnostics.ColoredLogger", "36;1", "📊 Template: " + templateNamesList.get(i) + " Diagnostics"));
            System.out.println("═".repeat(70));
            templateDiagnosticsList.get(i).reportAll();
        }

        System.out.println("\n" + "═".repeat(70));
        System.out.println("✨ Combined execution successfully finished for Flask and " + templatePaths.size() + " templates.");
        System.out.println("═".repeat(70) + "\n");
    }

    private static Program parseFlask(Path path) throws Exception {
        CharStream input = CharStreams.fromPath(path);
        FlaskLexer lexer = new FlaskLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FlaskParser parser = new FlaskParser(tokens);
        ParseTree tree = parser.prog();
        System.out.println("⏳ Parsing Flask source code syntax tree...");
        showModernParseTree("Flask Parse Tree", Files.readString(path), tree, parser);
        ProgramVisitor visitor = new ProgramVisitor();
        return visitor.visitProg((FlaskParser.ProgContext) tree);
    }

    private static ASTNode parseTemplate(Path path) throws Exception {
        CharStream input = CharStreams.fromPath(path);
        TemplateLexer lexer = new TemplateLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TemplateParser parser = new TemplateParser(tokens);
        ParseTree templateTree = parser.template();
        System.out.println("⏳ Parsing Template source code syntax tree...");
        showModernParseTree("Template Parse Tree", Files.readString(path), templateTree, parser);
        TemplateVisitor visitor = new TemplateVisitor();
        return visitor.visitTemplateRoot((TemplateParser.TemplateRootContext) templateTree);
    }

    private static void showModernParseTree(String title, String code, ParseTree parseTree, Parser parser) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1600, 900);

        Color bg = new Color(30, 30, 30);
        Color fg = new Color(220, 220, 220);

        JTextArea codeArea = new JTextArea(code);
        codeArea.setBackground(new Color(25, 25, 25));
        codeArea.setForeground(Color.WHITE);
        codeArea.setCaretColor(Color.WHITE);
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        codeArea.setEditable(false);

        JScrollPane codeScroll = new JScrollPane(codeArea);

        JTree tree = new JTree(new DefaultTreeModel(buildNode(parseTree, parser)));
        tree.setBackground(bg);
        tree.setForeground(fg);
        tree.setRowHeight(24);
        tree.setCellRenderer(new ParseTreeRenderer());

        expandAll(tree);

        JScrollPane treeScroll = new JScrollPane(tree);

        JLabel infoLabel = new JLabel(" Ready");

        JTextField searchField = new JTextField();

        JButton searchBtn = new JButton("Find");
        JButton expandBtn = new JButton("Expand");
        JButton collapseBtn = new JButton("Collapse");

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(new JLabel(" Search: "));
        toolBar.add(searchField);
        toolBar.add(searchBtn);
        toolBar.add(expandBtn);
        toolBar.add(collapseBtn);

        searchBtn.addActionListener(e -> searchNode(tree, searchField.getText().trim().toLowerCase()));
        expandBtn.addActionListener(e -> expandAll(tree));
        collapseBtn.addActionListener(e -> {
            for (int i = tree.getRowCount() - 1; i > 0; i--) tree.collapseRow(i);
        });

        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null) return;
            Object obj = node.getUserObject();
            if (!(obj instanceof NodeData data)) return;

            List<Token> tokens = new ArrayList<>();
            collectTokens(data.tree, tokens);

            if (!tokens.isEmpty()) {
                int startPos = tokens.get(0).getStartIndex();
                int stopPos = tokens.get(tokens.size() - 1).getStopIndex() + 1;
                int len = codeArea.getDocument().getLength();
                startPos = Math.min(startPos, len);
                stopPos = Math.min(stopPos, len);
                codeArea.requestFocus();
                codeArea.setCaretPosition(startPos);
                codeArea.moveCaretPosition(stopPos);
            }

            infoLabel.setText(" Node: " + data.label + " | Children: " + data.tree.getChildCount());
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, codeScroll);
        split.setDividerLocation(500);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        panel.add(infoLabel, BorderLayout.SOUTH);

        frame.setContentPane(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static class ParseTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            setBackgroundNonSelectionColor(new Color(30, 30, 30));
            setTextNonSelectionColor(Color.WHITE);
            String text = value.toString();
            if (text.contains("IDENTIFIER")) setForeground(new Color(0, 255, 180));
            else if (text.contains("STRING")) setForeground(new Color(255, 200, 0));
            else if (text.contains("NUMBER")) setForeground(new Color(120, 220, 255));
            else setForeground(Color.WHITE);
            return this;
        }
    }

    private static void searchNode(JTree tree, String text) {
        if (text == null || text.isEmpty()) return;
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        java.util.Enumeration<?> en = root.depthFirstEnumeration();
        while (en.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) en.nextElement();
            if (node.toString().toLowerCase().contains(text)) {
                TreePath path = new TreePath(node.getPath());
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                return;
            }
        }
    }

    private static DefaultMutableTreeNode buildNode(ParseTree tree, Parser parser) {
        String label;
        if (tree instanceof RuleNode r) {
            label = parser.getRuleNames()[r.getRuleContext().getRuleIndex()];
        } else if (tree instanceof TerminalNode tn) {
            Token token = tn.getSymbol();
            String tokenName = parser.getVocabulary().getSymbolicName(token.getType());
            String tokenText = token.getText().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
            label = tokenName + " : '" + tokenText + "'";
        } else {
            label = tree.getText();
        }
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NodeData(label, tree));
        for (int i = 0; i < tree.getChildCount(); i++) {
            node.add(buildNode(tree.getChild(i), parser));
        }
        return node;
    }

    private static void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private static void collectTokens(ParseTree tree, List<Token> tokens) {
        if (tree instanceof TerminalNode tn) {
            Token tok = tn.getSymbol();
            if (tok != null) tokens.add(tok);
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            collectTokens(tree.getChild(i), tokens);
        }
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
                stream.filter(path -> path.toString().endsWith(".html")).forEach(paths::add);
            } catch (Exception ignored) {
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
}