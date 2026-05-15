Symbol Table (Laila)
=======================

This folder scaffolds the symbol table subsystem for Laila. The project uses
two primary symbol table families: one for `Flask` (application-level symbols)
and one for `Template` (Jinja template-level symbols). The scaffolds provide
clean extension points and clear TODO markers where Laila should implement
the actual logic.

Core files (what was added):
- `Symbol.java` — data holder for a symbol (`name`, `SymbolKind`, optional `origin`).
- `SymbolKind.java` — enum with professional classification (VARIABLE, FUNCTION, TEMPLATE, ...).
- `ISymbolTable.java` — public interface with required operations.
- `AbstractSymbolTable.java` — base class that provides common storage fields (`symbols`, `children`).
- `FlaskSymbolTable.java` — scaffold specialized for Flask (application) symbols.
- `TemplateSymbolTable.java` — scaffold specialized for Template (Jinja) symbols.
- `SymbolTableRepository.java` — central place holding references to both primary tables and providing cross-resolution hooks.
- `SymbolTableBuilder.java` — abstract builder that will walk the AST to populate tables (signatures only).
- `FlaskSymbolTableBuilder.java` — concrete scaffold for Flask symbols (TODOs only).
- `TemplateSymbolTableBuilder.java` — concrete scaffold for Template symbols (TODOs only).

Design & Best Practices (recommended for Laila)
- Keep the two tables separate to avoid accidental name collisions: `FlaskSymbolTable` and `TemplateSymbolTable`.
- Use `SymbolKind` rather than raw strings to classify symbols.
- Back each scope with a `Map<String, Symbol>` (the scaffold uses a `LinkedHashMap` for deterministic iteration).
- Document and implement a clear lookup policy:
	- Template lookup typically checks the `TemplateSymbolTable` first. If `allowImplicitGlobals` is enabled, the template table may fall back to Flask symbols via `SymbolTableRepository`.
	- Flask lookup is normally limited to `FlaskSymbolTable` unless project policy requires otherwise.
- Keep `enterScope()`/`exitScope()` explicit and return new table objects for nested scopes (avoid mutating global state inadvertently).
- Use `Optional<Symbol>` for lookups to avoid `null`.
- Add unit tests for: shadowing, lookup local vs parent, cross-table fallback (if enabled), and conflict detection.

How to implement (practical steps)
1. Implement `define`, `lookupLocal`, `lookup`, `enterScope`, `exitScope`, and `listLocalSymbols` in both `FlaskSymbolTable` and `TemplateSymbolTable`.
2. Implement `SymbolTableBuilder` concrete classes: `FlaskSymbolTableBuilder` and `TemplateSymbolTableBuilder`. Use `SymbolTableVisitor` hooks to keep the builder code organized.
3. Use `SymbolTableRepository` when templates must resolve names from Flask context. Implement `resolveAcross` according to the chosen policy.
4. Add unit tests under `Tests/` verifying the behavior.

Recommended wiring
- Keep `SymbolTableBuilder` as the main orchestration layer that owns the active table(s) and decides which table is currently being populated.
- For Flask AST nodes, the builder should populate `FlaskSymbolTable`.
- For Template AST nodes, the builder should populate `TemplateSymbolTable`.
- If template resolution may see Flask globals, the builder should consult `SymbolTableRepository` instead of mixing both scopes into one structure.
- Do not merge the two symbol tables into a single map; keep them separate and explicit. That keeps the design easy to reason about and easy to test.

Laila: implement methods where `TODO` is indicated and follow the Javadocs.
