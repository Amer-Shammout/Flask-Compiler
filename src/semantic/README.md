# Semantic Module README (Updated)

**Updated on:** 2026-05-14

---

## Overview

The semantic module now has **3 layers**:

1. **Local Flask analysis**
2. **Local Template analysis**
3. **Cross-context bridge analysis** (Template ↔ Flask)

This means semantic errors are **not handled by bridge alone**. Bridge is only responsible for cross-context issues.

---

## Current Structure

```text
src/semantic/
├── analyzers/
│   ├── FlaskSemanticAnalyzer.java
│   ├── TemplateSemanticAnalyzer.java
│   └── SemanticAnalysisPipeline.java
├── bridge/
│   ├── TemplateContext.java
│   └── TemplateContextBridge.java
    └── RenderTempalteCall.java
    └── FlaskContextExtractor.java
    └── CrossContextResolutionIndex.java
    └── TemplateContextBridge.java
├── diagnostics/
│   ├── DiagnosticSeverity.java
│   ├── ErrorCode.java
│   ├── TypeKind.java
│   ├── ResolutionStatus.java
│   ├── Diagnostic.java
│   └── DiagnosticCollector.java
├── README.md
└── UPGRADES.md
```

---

## Diagnostics Layer

### DiagnosticSeverity
- `ERROR`, `WARNING`, `INFO`, `HINT`

### ErrorCode (enum-based codes)
- `E0xx`: undefined/not found
- `E1xx`: type-related
- `E2xx`: scope-related
- `Wxxx`: warnings
- `Ixxx`: informational
- `Hxxx`: hints

Examples:
- `E001_UNDEFINED_VARIABLE`
- `E101_TYPE_MISMATCH`
- `E203_OUT_OF_SCOPE`
- `W101_SHADOWING`

### TypeKind (enum-based type system)
- Core: `INT`, `FLOAT`, `STR`, `BOOL`, `NONE`
- Containers: `LIST`, `DICT`, `SET`, `TUPLE`
- Others: `FUNCTION`, `CLASS`, `OBJECT`
- Recovery/meta: `UNKNOWN`, `ANY`, `UNION`

### ResolutionStatus
- `RESOLVED`, `UNDEFINED`, `OUT_OF_SCOPE`, `AMBIGUOUS`, `PARTIAL`, `SHADOWED`, `UNAVAILABLE`

### Diagnostic
- Uses `ErrorCode` (not string code)
- Severity is derived from `ErrorCode`
- Supports optional `hint` and optional `TypeKind relatedType`

### DiagnosticCollector
- Central collector for all phases
- Supports helper methods for common diagnostics:
  - undefined variable
  - type mismatch
  - type error
  - scope error
  - missing Flask variable
  - shadowing
  - unused symbol
  - info/hint helpers

---

## Analyzer Layer

### FlaskSemanticAnalyzer
Handles **Flask-local** semantic checks.

Scope:
- Flask symbol building/usage
- Flask-local scope checks
- Flask-local type checks

Current state:
- Scaffolded and callable
- Integrates with shared `DiagnosticCollector`
- Gracefully emits hint diagnostics while dependent builders are still TODO

### TemplateSemanticAnalyzer
Handles **Template-local** semantic checks.

Scope:
- Template-local names/blocks/macros checks
- Template-local scope checks
- Template-local type checks

Current state:
- Scaffolded and callable
- Integrates with shared `DiagnosticCollector`
- Ready for Member 4/5 rules implementation

### SemanticAnalysisPipeline
Orchestrates semantic phases in order:

1. `FlaskSemanticAnalyzer.analyze(program)`
2. `TemplateSemanticAnalyzer.analyze(templateRoot)`
3. `TemplateContextBridge.bridge(program, templateRoot)`

Pipeline output:
- One shared `DiagnosticCollector` containing diagnostics from all phases.

---

## Bridge Layer

### TemplateContext
Represents one template symbol context with:
- `templateSymbol`
- optional linked `flaskSymbol`
- `origin` (`FLASK_CONTEXT`, `PARENT_TEMPLATE`, `LOCAL`, `JINJA_BUILTIN`, `UNKNOWN`)
- `type` as `TypeKind` (defaults to `UNKNOWN`)
- optional `sourceHint`

### TemplateContextBridge
Bridge is for **cross-context checks only**:
- resolve template symbol against Flask context
- detect template variable missing from Flask context
- detect cross-context shadowing
- detect cross-context type mismatch

Bridge should not replace local analyzers.

---

## Responsibility Split (Important)

Use this rule to decide where each diagnostic belongs:

- **FlaskSemanticAnalyzer**: Flask-only issues
- **TemplateSemanticAnalyzer**: Template-only issues
- **TemplateContextBridge**: Template-vs-Flask integration issues

This split prevents overloading bridge and keeps diagnostics precise.

---

## Integration Flow

```text
1) Parse Flask -> Program
2) Parse Template -> TemplateNode
3) Create SymbolTableRepository
4) Create shared DiagnosticCollector
5) Create SemanticAnalysisPipeline
6) pipeline.analyze(program, templateRoot)
7) collector.reportAll()
```

---

## Current TODOs

### Member 4 (Symbol Tables)
- Complete Flask/Template symbol table methods
- Complete repository cross-resolution policy

### Member 5 (Semantic)
- Implement concrete rules inside FlaskSemanticAnalyzer
- Implement concrete rules inside TemplateSemanticAnalyzer
- Complete bridge algorithm in `TemplateContextBridge.bridge(...)`
- Improve diagnostics ordering/dedup in `DiagnosticCollector`

---

## Notes

- The analyzers were added to establish a clean architecture now, even before all symbol-table internals are fully implemented.
- Bridge remains essential, but it is now explicitly the **third phase**, not the entire semantic engine.
