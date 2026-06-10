package SymbolTable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Registry of Python built-in functions, types, constants, and exceptions.
 *
 * Used as the final fallback in name resolution when all scopes fail.
 */
public final class PythonBuiltins {

    // =========================
    // BUILT-IN NAMES
    // =========================
    private static final Set<String> BUILTINS = new HashSet<>(Arrays.asList(
            // Types
            "int", "float", "str", "bool", "list", "dict", "set", "tuple",
            "bytes", "bytearray", "complex", "frozenset", "range", "object",
            "type", "memoryview", "slice",

            // Built-in functions
            "abs", "aiter", "all", "anext", "any", "ascii", "bin", "bool",
            "breakpoint", "callable", "chr", "classmethod", "compile", "delattr",
            "dir", "divmod", "enumerate", "eval", "exec", "filter", "format",
            "getattr", "globals", "hasattr", "hash", "hex", "id", "input",
            "isinstance", "issubclass", "iter", "len", "locals", "map", "max",
            "min", "next", "oct", "open", "ord", "pow", "print", "property",
            "repr", "reversed", "round", "setattr", "sorted", "staticmethod",
            "sum", "super", "vars", "zip",

            // Built-in constants
            "True", "False", "None", "NotImplemented", "Ellipsis",

            // Built-in exceptions (full coverage important for semantic analysis)
            "BaseException", "Exception", "ArithmeticError", "AssertionError",
            "AttributeError", "BlockingIOError", "BrokenPipeError", "BufferError",
            "BytesWarning", "ChildProcessError", "ConnectionError",
            "ConnectionAbortedError", "ConnectionRefusedError",
            "ConnectionResetError", "DeprecationWarning", "EOFError",
            "EnvironmentError", "FileExistsError", "FileNotFoundError",
            "FloatingPointError", "FutureWarning", "GeneratorExit",
            "IOError", "ImportError", "ImportWarning", "IndentationError",
            "IndexError", "InterruptedError", "IsADirectoryError", "KeyError",
            "KeyboardInterrupt", "LookupError", "MemoryError",
            "ModuleNotFoundError", "NameError", "NotADirectoryError",
            "NotImplementedError", "OSError", "OverflowError",
            "PendingDeprecationWarning", "PermissionError",
            "ProcessLookupError", "RecursionError", "ReferenceError",
            "ResourceWarning", "RuntimeError", "RuntimeWarning",
            "StopAsyncIteration", "StopIteration", "SyntaxError",
            "SyntaxWarning", "SystemError", "SystemExit", "TabError",
            "TimeoutError", "TypeError", "UnboundLocalError",
            "UnicodeDecodeError", "UnicodeEncodeError", "UnicodeError",
            "UnicodeTranslateError", "UnicodeWarning", "UserWarning",
            "ValueError", "Warning", "WindowsError", "ZeroDivisionError"
    ));

    // Types cache (FIX: avoid rebuilding set every call)
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
            "int", "float", "str", "bool", "list", "dict", "set", "tuple",
            "bytes", "bytearray", "complex", "frozenset", "range", "object",
            "type", "memoryview", "slice"
    ));

    // Exceptions set (FIX: correct classification)
    private static final Set<String> EXCEPTIONS = new HashSet<>(Arrays.asList(
            "BaseException", "Exception", "ArithmeticError", "AssertionError",
            "AttributeError", "BlockingIOError", "BrokenPipeError", "BufferError",
            "BytesWarning", "ChildProcessError", "ConnectionError",
            "ConnectionAbortedError", "ConnectionRefusedError",
            "ConnectionResetError", "DeprecationWarning", "EOFError",
            "EnvironmentError", "FileExistsError", "FileNotFoundError",
            "FloatingPointError", "FutureWarning", "GeneratorExit",
            "IOError", "ImportError", "ImportWarning", "IndentationError",
            "IndexError", "InterruptedError", "IsADirectoryError", "KeyError",
            "KeyboardInterrupt", "LookupError", "MemoryError",
            "ModuleNotFoundError", "NameError", "NotADirectoryError",
            "NotImplementedError", "OSError", "OverflowError",
            "PendingDeprecationWarning", "PermissionError",
            "ProcessLookupError", "RecursionError", "ReferenceError",
            "ResourceWarning", "RuntimeError", "RuntimeWarning",
            "StopAsyncIteration", "StopIteration", "SyntaxError",
            "SyntaxWarning", "SystemError", "SystemExit", "TabError",
            "TimeoutError", "TypeError", "UnboundLocalError",
            "UnicodeDecodeError", "UnicodeEncodeError", "UnicodeError",
            "UnicodeTranslateError", "UnicodeWarning", "UserWarning",
            "ValueError", "Warning", "WindowsError", "ZeroDivisionError"
    ));

    private PythonBuiltins() {
        // Static utility class
    }

    // =========================
    // LOOKUP API
    // =========================
    public static Optional<Symbol> lookup(String name) {
        if (name != null && BUILTINS.contains(name)) {
            SymbolKind kind = inferKind(name);
            return Optional.of(new Symbol(name, kind, "python-builtins"));
        }
        return Optional.empty();
    }

    public static boolean isBuiltin(String name) {
        return name != null && BUILTINS.contains(name);
    }

    // =========================
    // KIND INFERENCE (FIXED)
    // =========================
    private static SymbolKind inferKind(String name) {

        // Constants
        if ("True".equals(name) || "False".equals(name) || "None".equals(name) || "NotImplemented".equals(name) || "Ellipsis".equals(name)) {
            return SymbolKind.VARIABLE;
        }

        // Types + Exceptions
        if (isType(name) || isException(name)) {
            return SymbolKind.CLASS;
        }

        // Built-in functions
        return SymbolKind.FUNCTION;
    }

    private static boolean isType(String name) {
        return TYPES.contains(name);
    }

    private static boolean isException(String name) {
        return EXCEPTIONS.contains(name);
    }
}