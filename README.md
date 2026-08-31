# Flask & Template Compiler

## 🐍 Flask (Python) Compiler Part

### ✔ Lexer & Parser

**FlaskLexer** – Handles Python indentation (`INDENT`/`DEDENT`), literals, operators, keywords, decorators.

**FlaskParser** – Generates a full parse tree with expressions, statements, functions, classes, imports, and control flow.

### ✔ AST Structure

All AST nodes inherit from `ASTNode`. Core abstract categories:

- `Expression`
- `Literal`
- `Statement`
- `Suite`

Concrete nodes include:

- `BinaryExpr`
- `CompareExpr`
- `UnaryExpr`
- `CallExpr`
- `AttributeExpr`
- `IndexExpr`
- `IfStmt`
- `ForStmt`
- `WhileStmt`
- `FunctionDefStmt`
- `ClassDefStmt`
- `ReturnStmt`
- etc.

**Visitors:**

- `FlaskVisitor` – Builds AST from the parse tree.
- `ProgramVisitor` – Entry point for program-level AST construction.

---

## 🌐 Template Compiler Part

### ✔ Lexer & Parser

**TemplateLexer** – Multi-mode lexer supporting HTML, CSS, and Jinja2 delimiters (`{{ }}`, `{% %}`, `{# #}`).

**TemplateParser** – Handles HTML elements, attributes, inline CSS, and Jinja2 constructs.

### ✔ AST Structure

Template AST nodes inherit from `TemplateNode` and are grouped into:

#### HTML Nodes

- `HtmlDocument`
- `HtmlNormalElement`
- `HtmlAttribute`
- etc.

#### CSS Nodes

- `CssStylesheet`
- `CssRule`
- `CssDeclaration`
- etc.

#### Jinja Nodes

- `JinjaIfStmt`
- `JinjaForStmt`
- `JinjaBlockStmt`
- `JinjaExpr`
- etc.

---

## 📦 Symbol Table System

The symbol table architecture supports lexical scoping, shadowing detection, and cross-context lookup.

### Core Components

- `ISymbolTable` – Interface
- `AbstractSymbolTable` – Common base
- `LocalSymbolTable` – Nested scopes
- `FlaskSymbolTable` – Root scope for Flask
- `TemplateSymbolTable` – Root scope for templates
- `BuiltinsScope` – Singleton for Python builtins
- `RuntimeScope` – Singleton for runtime variables
- `SymbolTableRepository` – Bridges Flask and Template tables

### Reference Indices

- `FlaskReferenceIndex`
- `TemplateReferenceIndex`

These track every definition and usage with resolution status:

- `RESOLVED`
- `UNDEFINED`
- `SHADOWED`

---

## 🧪 Semantic Analysis

The semantic analysis pipeline consists of three phases.

### Phase 1: FlaskSemanticAnalyzer

- Undefined variable/function detection
- Scope violations (use before definition, out-of-scope)
- Type checking in Python expressions

### Phase 2: TemplateSemanticAnalyzer

- Template-local scope checks
- Type checking in Jinja expressions
- Block structure validation

### Phase 3: TemplateContextBridge

- Maps `render_template()` keyword arguments to Jinja variable references
- Detects missing Flask variables (`E004`) and undefined template variables (`E001`)
- Performs cross-context type checking, e.g. passing `str` from Flask and using it as `int` in a template

### Error Codes

| Code | Category |
|---|---|
| `E0xx` | Undefined |
| `E1xx` | Type |
| `E2xx` | Scope |
| `W1xx` | Warnings |
| `I0xx` | Info |
| `H0xx` | Hints |

---

## 🛠 Code Generation

Produces self-contained HTML files from Flask data and Jinja templates.

### Key Classes

- `RuntimeValue` – Dynamic value representation (`INT`, `STRING`, `LIST`, `OBJECT`, etc.)
- `ContextData` – Variables passed from Flask to template
- `FlaskDataExtractor` – Extracts global assignments from Flask AST
- `PythonContextEvaluator` – Simulates Flask route logic to build contexts
- `JinjaExpressionEvaluator` – Evaluates Jinja expressions, filters, and operators
- `TemplateRenderer` – Walks template AST and emits HTML with proper scoping, includes, extends, and CSS inlining
- `GenerationPipeline` – Orchestrates the entire generation process
- `GenerationServer` – Interactive HTTP server (Mode 5) that re-evaluates ASTs on each request, supporting add/edit/delete of products

### Output

The generation process produces:

- Generated HTML files in `output/`
- AST JSON exports in `compiler_output/`
- Semantic reports and generation logs

---

## 🌳 AST Visualization

### ASTGraphvizPrinter

Generates `.dot` files for Graphviz.

### Swing JTree Viewer

Interactive parse tree viewer with search.

### AstJsonExporter

Exports AST as JSON for documentation and debugging.

---

## 🖥 Interactive Server

A lightweight HTTP server implemented in pure Java simulates Flask runtime behavior.

It keeps products in memory, and on each request it re-renders the pre-parsed Jinja ASTs with current data.

### Routes

- `/products`
- `/add`
- `/edit/<id>`
- `/delete/<id>`

This demonstrates that Java can perform the same dynamic rendering as Flask without actually running Flask.

---

## 👥 Team

| Member | Responsibility |
|---|---|
| **Amer Shammout** | Bridge & Context, Missing Flask Variable Checker |
| **George Abboud** | Undefined & Scope Errors |
| **Sedra Al Halabe** | Type Errors |
| **Laila Almasry** | Data Layer + Template Engine |
| **Ghalia Sbei** | Generation Pipeline, Output, Server |

All members collaborated on earlier stages, including Lexers, Parsers, ASTs, and Symbol Tables.

---

## 🚀 Usage

Run the main class `Main` and choose an execution mode:

1. **Flask only** – Semantic analysis without templates
2. **Flask + single template** – With bridge
3. **Flask + all templates** – With bridge
4. **Generate static HTML**
5. **Start interactive server**

---

## 🔮 Future Work

- Support more complex Python/Jinja features (macros, imports, advanced filters)
- Enhance error recovery and suggestion accuracy
- Implement dataflow analysis
- Integrate with Language Server Protocol (LSP) for IDE support

---

## 📄 License

This project is for educational purposes. You are free to use, modify, and extend it for learning and academic work.

### Authors

- Amer Shammout
- Laila Almasry
- Sedra Al Halabe
- Ghalia Sbei
- George Abboud

### Project Information

- **Framework:** Flask
- **Tooling:** ANTLR4, Java
