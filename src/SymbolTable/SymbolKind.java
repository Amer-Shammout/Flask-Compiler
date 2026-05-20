package SymbolTable;

/**
 * Professional enum capturing common symbol kinds used across both Flask
 * and Template symbol tables. Use this instead of raw strings for type-safety.
 */
public enum SymbolKind {
    VARIABLE,
    FUNCTION,
    CLASS,
    TEMPLATE,
    BLOCK,
    IMPORT,
    PARAMETER,
    MODULE
}
