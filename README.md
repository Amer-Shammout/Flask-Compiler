# Flask Compiler using ANTLR4

This project is an **educational compiler / interpreter front-end** for the **Flask web framework**, built using **ANTLR4**.  
It focuses on parsing, building Abstract Syntax Trees (AST), and preparing the foundation for semantic analysis and symbol tables for both **Flask (Python)** code and **Template files (HTML, Internal CSS, Jinja2)**.

The project is designed for **Compiler Construction / Software Engineering courses** and demonstrates real-world language processing on a modern web framework.

---

## ✨ Project Overview

The compiler is divided into **two main language domains**:

1. **Flask (Python-based backend)**
2. **Template Language (HTML + Internal CSS + Jinja2)**

Each domain has:
- Its own **Lexer**
- Its own **Parser**
- Its own **Parse Tree**
- Its own **AST (Abstract Syntax Tree)**

---

## 🧠 Architecture

Source Code
│
▼
ANTLR4 Lexer
│
ANTLR4 Parser
│
Parse Tree
│
Visitor
│
Abstract Syntax Tree (AST)
│
(Symbol Table - Planned)

---


---

## 🐍 Flask (Python) Compiler Part

### ✔ Lexer & Parser
- **FlaskLexer**
- **FlaskParser**

Generated using **ANTLR4** from a Python-like grammar tailored for Flask applications.

### ✔ AST Structure

All AST nodes inherit from a common base class:

- **ASTNode** (Abstract)

Core abstract categories:
- **Expression**
- **Literal**
- **Statement**
- **Suite**

All of the above:
- Are **abstract classes**
- Extend **ASTNode**

#### Example Expression Hierarchy
- `BinaryExpr`
- `CompareExpr`
- `UnaryExpr`
- `CallExpr`
- ...

Each concrete expression:
- Extends `Expression`
- Follows a clean and extensible inheritance pattern

---

### ✔ Visitors

Two visitors are implemented for the Flask side:

- **FlaskVisitor**
  - Traverses the ANTLR Parse Tree
  - Builds the corresponding AST

- **ProgramVisitor**
  - Handles higher-level program structures
  - Entry point for AST construction

---

## 🌐 Template Compiler Part

### ✔ Lexer & Parser
- **TemplateLexer**
- **TemplateParser**

The Template language supports:
- HTML
- Internal CSS
- Jinja2 constructs (`{{ }}`, `{% %}`)

### 🔧 AST (Planned)
- The Template AST has **not been implemented yet**
- Will follow a similar design to the Flask AST
- Separate node hierarchy for:
  - HTML elements
  - Attributes
  - CSS rules
  - Jinja2 expressions and statements

---

## 🌳 AST Visualization (Graphviz)

To visualize AST structures, the project includes:

### ✔ ASTGraphvizPrinter

- Generates a file called `ast.dot`
- Represents the AST in **Graphviz DOT format**

### ✔ How to View the AST
1. Run the compiler to generate `ast.dot`
2. Copy the file contents
3. Paste it into the following website: https://dreampuf.github.io/GraphvizOnline/?engine=dot
4. Instantly view the AST as a visual tree

This is extremely useful for:
- Debugging
- Understanding AST structure
- Academic demonstrations

---

## 📦 Symbol Table 

A **Symbol Table** was added to support:
- Variable declarations
- Function definitions
- Scope handling
- Semantic analysis

It will be integrated after AST construction for both:
- Flask code 
- Template code 

---

## 🛠 Technologies Used

- **Java**
- **ANTLR4**
- **Graphviz**
- **Flask (as target language)**
- **HTML / CSS / Jinja2**

---

## 🎯 Project Goals

- Demonstrate real-world compiler design
- Apply ANTLR4 to a modern framework (Flask)
- Build clean and extensible ASTs
- Prepare groundwork for semantic analysis
- Provide visual AST inspection via Graphviz

---

## 🚀 Future Work

- [ ] Implement Template AST
- [ ] Build Symbol Table
- [ ] Add Semantic Analysis
- [ ] Error handling & diagnostics
- [ ] Code generation or interpretation phase

---

## 📚 Academic Context

This project is suitable for:
- Compiler Design courses
- Programming Languages courses
- Software Engineering projects
- Advanced ANTLR4 practice

---

## 📄 License

This project is intended for **educational purposes**.  
You are free to use, modify, and extend it for learning and academic work.

---

**Author:** Amer Shammout  
**Framework:** Flask  
**Tooling:** ANTLR4
