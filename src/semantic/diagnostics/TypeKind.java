package semantic.diagnostics;

import java.util.Optional;

/**
 * Enumeration representing type kinds for semantic analysis.
 *
 * TypeKind abstracts over concrete type representations (Python types, Jinja types, etc.)
 * and provides a unified type system for:
 * - Type checking and inference
 * - Type mismatch diagnostics
 * - Cross-context type resolution (Flask → Template)
 *
 * Each TypeKind can optionally carry type parameters (for generic types like List[int], Dict[str, User]).
 *
 * Usage:
 *   TypeKind intType = TypeKind.INT;
 *   TypeKind listType = TypeKind.LIST; // non-parametric
 *   TypeKind listOfInt = TypeKind.parameterized(TypeKind.LIST, TypeKind.INT);
 *
 * TODO(Sedra): Implement parametric types fully (nested generics, union types, etc.).
 * TODO(Sedra): Add type compatibility and conversion checking.
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
     * May be parameterized: LIST[INT], LIST[STR], etc.
     */
    LIST("list"),

    /**
     * DICT: Dictionary/map type (Python dict, Jinja mapping).
     * May be parameterized: DICT[STR, USER], etc.
     */
    DICT("dict"),

    /**
     * SET: Set type (Python set).
     * May be parameterized: SET[INT], etc.
     */
    SET("set"),

    /**
     * TUPLE: Tuple type (Python tuple).
     * May be parameterized: TUPLE[INT, STR], etc.
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
     *
     * Used when:
     * - A symbol's type cannot be determined.
     * - Type inference failed or was skipped.
     * - A Flask variable's type is not declared.
     *
     * UNKNOWN is compatible with any type (for recovery in error cases),
     * but diagnostics may be generated for clarity.
     */
    UNKNOWN("unknown"),

    /**
     * ANY: Explicitly any/dynamic type (Python typing.Any, untyped variable).
     *
     * Different from UNKNOWN:
     * - UNKNOWN: type not determined
     * - ANY: explicitly allowed to be anything (no error on type mismatch)
     */
    ANY("any"),

    /**
     * UNION: Union of multiple types (Python Union[T1, T2, ...]).
     * May be parameterized: UNION[INT, STR], etc.
     */
    UNION("union");


    // === Fields ===

    /** Display name of the type. */
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
     * @return Display name (e.g., "int", "List", "unknown").
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
     *
     * Compatibility rules:
     * - UNKNOWN and ANY are compatible with all types (recovery).
     * - NONE is only compatible with NONE or ANY.
     * - Exact match required for primitives (INT != FLOAT).
     * - Container types checked by element type if parametrized.
     *
     * TODO(Sedra): Implement full type compatibility matrix with inheritance/subtypes.
     *
     * @param other Another TypeKind to check against.
     * @return true if this type is assignable to other.
     */
    public boolean isCompatibleWith(TypeKind other) {
        // UNKNOWN and ANY are compatible with anything
        if (this == UNKNOWN || this == ANY || other == UNKNOWN || other == ANY) {
            return true;
        }

        // Same type is compatible
        if (this == other) {
            return true;
        }

        // INT and FLOAT may be compatible (with warning)
        if ((this == INT && other == FLOAT) || (this == FLOAT && other == INT)) {
            return true; // TODO(Sedra): Decide if this should be warning instead
        }

        // NONE is only compatible with NONE/ANY
        if (this == NONE || other == NONE) {
            return this == other || other == ANY;
        }

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
