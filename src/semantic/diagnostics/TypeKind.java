package semantic.diagnostics;

import java.util.Optional;

/**
 * Enumeration representing type kinds for semantic analysis.
 * <p>
 * TypeKind abstracts over concrete type representations (Python types, Jinja types, etc.)
 * and provides a unified type system for:
 * - Type checking and inference
 * - Type mismatch diagnostics
 * - Cross-context type resolution (Flask → Template)
 * <p>
 * Each TypeKind can optionally carry type parameters (for generic types like List[int], Dict[str, User]).
 * <p>
 * NOTE: This enum currently represents *kinds* only (no parameters). Parametric types are
 * planned in future; compatibility logic here is conservative and non-breaking.
 */
public enum TypeKind {

    // === Primitive Types ===

    /**
     * INT: Integer type (Python int, Jinja number literal).
     */
    INT("int"),

    /**
     * FLOAT: Floating-point type (Python float).
     */
    FLOAT("float"),

    /**
     * STR: String type (Python str, Jinja string).
     */
    STR("str"),

    /**
     * BOOL: Boolean type (Python bool, Jinja boolean).
     */
    BOOL("bool"),

    /**
     * NONE: None/null type (Python None, Jinja undefined).
     */
    NONE("none"),


    // === Container Types ===

    /**
     * LIST: List/array type (Python list, Jinja iterable).
     * May be parameterized in a future implementation.
     */
    LIST("list"),

    /**
     * DICT: Dictionary/map type (Python dict, Jinja mapping).
     */
    DICT("dict"),

    /**
     * SET: Set type (Python set).
     */
    SET("set"),

    /**
     * TUPLE: Tuple type (Python tuple).
     */
    TUPLE("tuple"),


    // === Special Types ===

    /**
     * FUNCTION: Function or callable type (Python function, Jinja macro).
     */
    FUNCTION("function"),

    /**
     * CLASS: Class or user-defined type (Python class, Jinja object).
     */
    CLASS("class"),

    /**
     * OBJECT: Generic object or instance (Python object).
     */
    OBJECT("object"),


    // === Meta Types ===

    /**
     * UNKNOWN: Type unknown or not yet inferred.
     * <p>
     * UNKNOWN is compatible with any type (for recovery in error cases).
     */
    UNKNOWN("unknown"),

    /**
     * ANY: Explicitly any/dynamic type (Python typing.Any, untyped variable).
     * <p>
     * ANY indicates explicit permissiveness (no error on type mismatch).
     */
    ANY("any"),

    /**
     * UNION: Union of multiple types (Python Union[T1, T2, ...]).
     * <p>
     * With current representation UNION is a marker; future representation should capture members.
     */
    UNION("union");

    // === Fields ===

    /**
     * Display name of the type.
     */
    private final String displayName;


    // === Constructor ===

    /**
     * Construct a TypeKind.
     *
     * @param displayName Display name for the type (used in diagnostics).
     */
    TypeKind(String displayName) {
        this.displayName = displayName;
    }


    // === Getters ===

    /**
     * Get the display name of this type.
     *
     * @return Display name (e.g., "int", "list", "unknown").
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this is a container type (LIST, DICT, SET, TUPLE).
     *
     * @return true if container type.
     */
    public boolean isContainerType() {
        return this == LIST || this == DICT || this == SET || this == TUPLE;
    }

    /**
     * Check if this is a primitive type (INT, FLOAT, STR, BOOL, NONE).
     *
     * @return true if primitive type.
     */
    public boolean isPrimitiveType() {
        return this == INT || this == FLOAT || this == STR || this == BOOL || this == NONE;
    }

    /**
     * Check if this is a meta-type (UNKNOWN, ANY, UNION).
     *
     * @return true if meta-type.
     */
    public boolean isMetaType() {
        return this == UNKNOWN || this == ANY || this == UNION;
    }

    /**
     * Check if this type is compatible with another type for assignment.
     * <p>
     * Compatibility rules (conservative and non-breaking):
     * - UNKNOWN and ANY are compatible with all types (recovery).
     * - UNION is treated permissively (compatible with its members — here, we assume permissive).
     * - NONE is compatible with NONE and ANY (and permissive when paired with UNKNOWN).
     * - Exact match required for primitives in general, with a small allowance between INT and FLOAT.
     * - Container kinds (LIST, DICT, SET, TUPLE) are considered compatible with same container kind (non-parametric).
     * <p>
     * TODO: In future, extend TypeKind to hold parameters (type arguments) and implement deep compatibility.
     *
     * @param other Another TypeKind to check against.
     * @return true if this type is assignable to other.
     */
    public boolean isCompatibleWith(TypeKind other) {
        if (other == null) return false;

        // If either side is ANY or UNKNOWN, allow compatibility (recovery)
        if (this == UNKNOWN || this == ANY || other == UNKNOWN || other == ANY) {
            return true;
        }

        // If either is UNION, be permissive (detailed union-members handling requires richer representation)
        if (this == UNION || other == UNION) {
            return true;
        }

        // Exact match
        if (this == other) {
            return true;
        }

        // Numeric compatibility: INT <-> FLOAT allowed (conservative)
        if ((this == INT && other == FLOAT) || (this == FLOAT && other == INT)) {
            return true;
        }

        // NONE compatibility: allow NONE <-> NONE, and allow None with ANY/UNKNOWN handled earlier.
        if (this == NONE || other == NONE) {
            return this == other || this == ANY || other == ANY || this == UNKNOWN || other == UNKNOWN;
        }

        // Container kinds: consider same container kinds compatible (non-parametric)
        if (this.isContainerType() && other.isContainerType()) {
            return this == other;
        }

        // As a fallback, not compatible.
        return false;
    }

    /**
     * Get a string representation for diagnostics.
     *
     * @return Display name.
     */
    @Override
    public String toString() {
        return displayName;
    }
}