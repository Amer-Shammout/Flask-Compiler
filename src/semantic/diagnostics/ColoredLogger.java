package semantic.diagnostics;

/**
 * ColoredLogger
 */
public class ColoredLogger {

    private static final String RESET = "\u001B[0m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_BLUE = "\u001B[94m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BOLD = "\u001B[1m";

    private static final boolean COLORS_ENABLED = true;

    public static void printDiagnostic(Diagnostic diagnostic) {
        var code = diagnostic.getErrorCode();
        String codeStr = code.getCode();
        String codeDesc = code.getDescription();
        String message = diagnostic.getMessage();

        // For type-related error codes (E1xx) ensure message starts with "TypeError: "
        if (codeStr.startsWith("E1") && !message.startsWith("TypeError:")) {
            message = "TypeError: " + message;
        }

        // Location string
        String srcRangeStr = "";
        String startLineCol = "";
        if (diagnostic.getSourceRange() != null && diagnostic.getSourceRange().getStart() != null) {
            srcRangeStr = diagnostic.getSourceRange().toString();
            startLineCol = String.format(" [Line %d, Col %d]", diagnostic.getSourceRange().getStart().getLine(), diagnostic.getSourceRange().getStart().getColumn());
        }

        String header;
        switch (diagnostic.getSeverity()) {
            case ERROR -> header = color(BRIGHT_RED + BOLD, "❌ ERROR");
            case WARNING -> header = color(BRIGHT_YELLOW + BOLD, "⚠️  WARNING");
            case INFO -> header = color(BRIGHT_BLUE + BOLD, "ℹ️  INFO");
            case HINT -> header = color(BRIGHT_GREEN + BOLD, "💡 HINT");
            default -> header = "[?]";
        }

        // Format:
        // ─── ❌ ERROR ────────────────────────────────────────────────────────
        // Code    : [E104] Not callable
        // Location: @ srcRange [Line X, Col Y]
        // Message : TypeError: 'int' object is not callable
        // Hint    : ...
        StringBuilder sb = new StringBuilder();
        sb.append("─── ").append(header).append(" ─".repeat(50)).append("\n");
        sb.append("  ").append(color(BOLD, "Code    : ")).append("[").append(codeStr).append("] ").append(codeDesc).append("\n");

        if (!srcRangeStr.isEmpty()) {
            sb.append("  ").append(color(BOLD, "Location: ")).append("@ ").append(srcRangeStr).append(color(BRIGHT_BLUE, startLineCol)).append("\n");
        }

        sb.append("  ").append(color(BOLD, "Details : ")).append(message).append("\n");

        diagnostic.getHint().ifPresent(h -> sb.append("  ").append(color(BOLD, "Context : ")).append(color(BRIGHT_GREEN, "💡 Hint: " + h)).append("\n"));

        System.out.println(sb.toString());
    }

    public static void printSummary(DiagnosticCollector collector) {
        if (collector.isEmpty()) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println(color(BRIGHT_GREEN + BOLD, "  ✨ Success: Semantic analysis passed - No errors or warnings found!"));
            System.out.println("═".repeat(70) + "\n");
            return;
        }

        System.out.println("\n" + "═".repeat(70));
        System.out.println(color(BRIGHT_RED + BOLD, "                  🚨  SEMANTIC ERRORS DETECTION REPORT  🚨"));
        System.out.println("═".repeat(70));

        System.out.println("  📊 " + color(BRIGHT_RED + BOLD, "Errors: " + collector.getErrorCount()) + "  |  " + color(BRIGHT_YELLOW + BOLD, "Warnings: " + collector.getWarningCount()) + "  |  " + color(BRIGHT_BLUE + BOLD, "Infos: " + collector.getInfoCount()) + "  |  " + color(BRIGHT_GREEN + BOLD, "Hints: " + collector.getHintCount()));
        System.out.println("═".repeat(70) + "\n");

        // Use sorted diagnostics provided by DiagnosticCollector to make output deterministic
        for (Diagnostic diag : collector.getSortedDiagnostics()) {
            printDiagnostic(diag);
        }

        System.out.println("═".repeat(70));
        System.out.println(color(BOLD, "  ℹ️  End of Diagnostic Report"));
        System.out.println("═".repeat(70) + "\n");
    }


    public static String color(String className, String colorCode, String text) {
        return COLORS_ENABLED ? "\u001B[" + colorCode + "m" + text + RESET : text;
    }

    private static String color(String code, String text) {
        return COLORS_ENABLED ? code + text + RESET : text;
    }
}