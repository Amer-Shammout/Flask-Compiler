package SymbolTable;

/**
 * Distinguishes symbol sites that introduce a name from sites that refer to one.
 */
public enum SymbolUseKind {
    DEFINITION,
    REFERENCE,
    GLOBAL_DECLARATION,
    NONLOCAL_DECLARATION
}
