package SymbolTable;

import java.util.List;

public class Symbol {

    public enum SymbolType {
        VARIABLE,
        FUNCTION,
        PARAMETER
    }

    private final String name;
    private final SymbolType type;
    private final int lineNumber;
    private final List<String> parameters;


    public Symbol(String name, SymbolType type, int lineNumber) {
        this.name = name;
        this.type = type;
        this.lineNumber = lineNumber;
        this.parameters = null;
    }


    public Symbol(String name, int lineNumber, List<String> parameters) {
        this.name = name;
        this.type = SymbolType.FUNCTION;
        this.lineNumber = lineNumber;
        this.parameters = parameters;
    }


    public String getName() { return name; }
    public SymbolType getType() { return type; }
    public int getLineNumber() { return lineNumber; }
    public List<String> getParameters() { return parameters; }

    @Override
    public String toString() {
        switch (type) {
            case VARIABLE:
                return "Variable: " + name + " (line " + lineNumber + ")";
            case PARAMETER:
                return "Parameter: " + name + " (line " + lineNumber + ")";
            case FUNCTION:
                return "Function: " + name + " (line " + lineNumber + "), params=" + parameters;
            default:
                return name;
        }
    }
}
