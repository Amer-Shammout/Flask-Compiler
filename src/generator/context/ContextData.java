package generator.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Variables passed from Flask {@code render_template(...)} into a Jinja template.
 * <p>
 * Example for products.html:
 * <pre>
 *   products = [Product(1,"Laptop",1200.0,True), Product(2,"Headphones",250.0,False)]
 * </pre>
 * becomes {@code ContextData} with key {@code "products"} → LIST of OBJECT values.
 */
public final class ContextData {

    private final Map<String, RuntimeValue> variables;

    public ContextData() {
        this.variables = new LinkedHashMap<>();
    }

    public ContextData(Map<String, RuntimeValue> initial) {
        this.variables = new LinkedHashMap<>(initial);
    }

    public void put(String name, RuntimeValue value) {
        variables.put(name, value);
    }

    public RuntimeValue get(String name) {
        return variables.getOrDefault(name, RuntimeValue.none());
    }

    public boolean contains(String name) {
        return variables.containsKey(name);
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(variables.keySet());
    }

    public Map<String, RuntimeValue> asMap() {
        return Collections.unmodifiableMap(variables);
    }

    public boolean isEmpty() {
        return variables.isEmpty();
    }

    /** Child context for loops: copies parent vars then adds/overrides loop locals. */
    public ContextData withLocal(String name, RuntimeValue value) {
        ContextData child = new ContextData(this.variables);
        child.put(name, value);
        return child;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ContextData{");
        boolean first = true;
        for (Map.Entry<String, RuntimeValue> e : variables.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}
