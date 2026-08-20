package generator;

import AST.ASTNode;
import AST.SourceRange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Serializes an AST into a simple JSON tree for {@code compiler_output/}.
 * Uses {@link ASTNode#getNodeName()}, line number, and {@link ASTNode#getChildren()}.
 */
public final class AstJsonExporter {

    private AstJsonExporter() {
    }

    public static String toJson(ASTNode root) {
        StringBuilder sb = new StringBuilder();
        appendNode(sb, root, 0);
        sb.append('\n');
        return sb.toString();
    }

    /** Write one AST as a JSON file. */
    public static Path write(ASTNode root, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, toJson(root), StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Write a map of named ASTs (e.g. template file → tree) as one JSON object.
     */
    public static Path writeNamed(Map<String, ASTNode> roots, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int i = 0;
        for (Map.Entry<String, ASTNode> entry : roots.entrySet()) {
            if (i++ > 0) {
                sb.append(",\n");
            }
            indent(sb, 1);
            sb.append('"').append(escape(entry.getKey())).append("\": ");
            appendNode(sb, entry.getValue(), 1);
        }
        sb.append("\n}\n");
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        return file;
    }

    private static void appendNode(StringBuilder sb, ASTNode node, int depth) {
        if (node == null) {
            sb.append("null");
            return;
        }

        sb.append("{\n");
        indent(sb, depth + 1);
        sb.append("\"type\": \"").append(escape(node.getNodeName())).append("\",\n");
        indent(sb, depth + 1);
        sb.append("\"line\": ").append(node.getLineNumber()).append(",\n");

        SourceRange range = node.getSourceRange();
        indent(sb, depth + 1);
        if (range != null) {
            sb.append("\"range\": \"").append(escape(range.toString())).append("\",\n");
        } else {
            sb.append("\"range\": null,\n");
        }

        // Extra human-readable label from toString (truncated)
        String label = node.toString();
        indent(sb, depth + 1);
        sb.append("\"label\": \"").append(escape(truncate(label, 120))).append("\",\n");

        indent(sb, depth + 1);
        sb.append("\"children\": [");
        List<ASTNode> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            sb.append("]\n");
        } else {
            sb.append('\n');
            for (int i = 0; i < children.size(); i++) {
                indent(sb, depth + 2);
                appendNode(sb, children.get(i), depth + 2);
                if (i < children.size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            indent(sb, depth + 1);
            sb.append("]\n");
        }
        indent(sb, depth);
        sb.append('}');
    }

    private static void indent(StringBuilder sb, int depth) {
        sb.append("  ".repeat(Math.max(0, depth)));
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
