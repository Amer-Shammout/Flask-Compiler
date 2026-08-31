package generator.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;

/**
 * Runtime representation of a Python value used during code generation.
 * <p>
 * This is NOT tied to the Symbol Table — generation evaluates AST values directly
 * into these objects, then Jinja rendering reads them (e.g. {{ product.name }}).
 */
public final class RuntimeValue {

    public enum Kind {
        NONE,
        BOOL,
        INT,
        FLOAT,
        STRING,
        LIST,
        OBJECT
    }

    private final Kind kind;
    private final Object value;

    private RuntimeValue(Kind kind, Object value) {
        this.kind = kind;
        this.value = value;
    }

    // ── Factories ──────────────────────────────────────────────────────────

    public static RuntimeValue none() {
        return new RuntimeValue(Kind.NONE, null);
    }

    public static RuntimeValue ofBool(boolean b) {
        return new RuntimeValue(Kind.BOOL, b);
    }

    public static RuntimeValue ofInt(long n) {
        return new RuntimeValue(Kind.INT, n);
    }

    public static RuntimeValue ofFloat(double d) {
        return new RuntimeValue(Kind.FLOAT, d);
    }

    public static RuntimeValue ofString(String s) {
        return new RuntimeValue(Kind.STRING, s == null ? "" : s);
    }

    public static RuntimeValue ofList(List<RuntimeValue> items) {
        return new RuntimeValue(Kind.LIST, new ArrayList<>(items));
    }

    public static RuntimeValue ofObject(Map<String, RuntimeValue> fields) {
        return new RuntimeValue(Kind.OBJECT, new LinkedHashMap<>(fields));
    }

    /** Convenience: build a Product-like object with id/name/price/in_stock. */
    public static RuntimeValue ofProduct(long id, String name, double price, boolean inStock) {
        Map<String, RuntimeValue> fields = new LinkedHashMap<>();
        fields.put("id", ofInt(id));
        fields.put("name", ofString(name));
        fields.put("price", ofFloat(price));
        fields.put("in_stock", ofBool(inStock));
        return ofObject(fields);
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    public Kind getKind() {
        return kind;
    }

    public boolean isNone() {
        return kind == Kind.NONE;
    }

    public boolean isTruthy() {
        return switch (kind) {
            case NONE -> false;
            case BOOL -> (Boolean) value;
            case INT -> ((Long) value) != 0L;
            case FLOAT -> ((Double) value) != 0.0;
            case STRING -> !((String) value).isEmpty();
            case LIST -> !asList().isEmpty();
            case OBJECT -> !asObject().isEmpty();
        };
    }

    public boolean asBool() {
        if (kind != Kind.BOOL) {
            throw new IllegalStateException("Expected BOOL, got " + kind);
        }
        return (Boolean) value;
    }

    public long asInt() {
        if (kind == Kind.INT) return (Long) value;
        if (kind == Kind.FLOAT) return ((Double) value).longValue();
        throw new IllegalStateException("Expected INT/FLOAT, got " + kind);
    }

    public double asFloat() {
        if (kind == Kind.FLOAT) return (Double) value;
        if (kind == Kind.INT) return ((Long) value).doubleValue();
        throw new IllegalStateException("Expected FLOAT/INT, got " + kind);
    }

    public String asString() {
        if (kind != Kind.STRING) {
            throw new IllegalStateException("Expected STRING, got " + kind);
        }
        return (String) value;
    }

    @SuppressWarnings("unchecked")
    public List<RuntimeValue> asList() {
        if (kind != Kind.LIST) {
            throw new IllegalStateException("Expected LIST, got " + kind);
        }
        return Collections.unmodifiableList((List<RuntimeValue>) value);
    }

    @SuppressWarnings("unchecked")
    public Map<String, RuntimeValue> asObject() {
        if (kind != Kind.OBJECT) {
            throw new IllegalStateException("Expected OBJECT, got " + kind);
        }
        return Collections.unmodifiableMap((Map<String, RuntimeValue>) value);
    }

    /**
     * Attribute access: product.name → getAttr("name").
     * <p>
     * Mirrors Jinja's default (lenient) Undefined behavior: accessing an attribute on a
     * non-object value — e.g. {@code product} is None because no product matched — does not
     * abort generation. It evaluates to None, the same way {{ product.name }} silently renders
     * blank in real Jinja instead of raising, so one missing/empty data case can't take down
     * the whole generation run.
     */
    public RuntimeValue getAttr(String name) {
        if (kind != Kind.OBJECT) {
            return none();
        }
        RuntimeValue field = asObject().get(name);
        return field != null ? field : none();
    }

    /** Index access: product[0] or list[i] */
    public RuntimeValue getIndex(int index) {
        if (kind == Kind.LIST) {
            List<RuntimeValue> list = asList();
            if (index < 0 || index >= list.size()) {
                throw new IndexOutOfBoundsException("Index " + index + " out of bounds for list size " + list.size());
            }
            return list.get(index);
        }
        throw new IllegalStateException("Index access on non-list: " + kind);
    }

    // ── Display (what {{ value }} prints in HTML) ──────────────────────────

    @Override
    public String toString() {
        return switch (kind) {
            case NONE -> "None";
            case BOOL -> ((Boolean) value) ? "True" : "False";
            case INT -> Long.toString((Long) value);
            case FLOAT -> {
                double d = (Double) value;
                if (d == Math.rint(d) && !Double.isInfinite(d)) {
                    yield String.format(Locale.ROOT, "%.1f", d);
                }
                yield String.format(Locale.ROOT, "%s", Double.toString(d));
            }
            case STRING -> (String) value;
            case LIST -> value.toString();
            case OBJECT -> value.toString();
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuntimeValue other)) return false;
        return kind == other.kind && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, value);
    }
}
