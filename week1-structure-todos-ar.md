# خطة إعداد البنية + TODOs (الأسبوع الأول)

التاريخ: 2026-05-11

**المسؤول عن الهيكلة:** عضو 1 (أنت) — يبني الملفات والكلاسات كهيكل مع TODOs فقط.  
**التنفيذ لاحقًا:** يتم توزيعه على أعضاء الفريق 2–5 حسب الـ TODOs.

---

## مبدأ العمل

- كل الملفات تُنشأ **بدون implementations**.
- كل TODOs تُكتب **بالإنجليزية** داخل الكود.
- الهيكل يجب أن يبني (compiles) حتى بدون تنفيذ فعلي.

صيغة TODO المقترحة:
```java
// TODO(Member 3): Implement ...
```

---

## توزيع الـ TODOs على الأعضاء

**Member 1 (Integrator / Structure)**
- إنشاء هيكل الملفات والباكيجات.
- وضع TODOs عامة وربط المسار العام.

**Member 2 (Flask AST)**
- TODOs لكل عقد Flask AST (constructors/getters/SourceRange).
- TODOs داخل FlaskVisitor وProgramVisitor لتغذية SourceRange.

**Member 3 (Template AST)**
- TODOs لكل عقد Template AST (HTML/CSS/Jinja) مع SourceRange.
- TODOs داخل TemplateVisitor لتمرير SourceRange.

**Member 4 (Symbol Tables)**
- TODOs لبناء FlaskSymbolTable (Symbol/Scope/Kind).
- TODOs لبناء TemplateSymbolTable.

**Member 5 (Diagnostics + Bridge)** — ✅ **SCAFFOLDING COMPLETE**
- ✅ DiagnosticSeverity: Enum للخطورة (ERROR, WARNING, INFO, HINT)
- ✅ Diagnostic: رسالة واحدة مع SourceRange، code، hint
- ✅ DiagnosticCollector: مجمع ومدير الرسائل مع تصفية حسب الخطورة
- ✅ TemplateContext: سياق رمز Template مع ربط اختياري لـ Flask
- ✅ TemplateContextBridge: جسر ربط Symbol Tables مع توليد diagnostics
- ✅ README.md: دليل شامل للتنفيذ
- **TODOs للتنفيذ:**
  - تنفيذ طباعة مفصلة وتصفية في DiagnosticCollector
  - تنفيذ خوارزمية ربط كاملة في TemplateContextBridge
  - توليد diagnostics متخصصة (undefined, shadowing, type-mismatch)

---

## الخطة التنفيذية — ما سيُنشأ كهيكل

### 1) Core Location Model
- `AST/SourcePosition.java`
- `AST/SourceRange.java`

**TODOs**: Member 1 (هيكل) + Member 2/3 (الاستخدام لاحقًا).

---

### 2) Diagnostics
- `semantic/diagnostics/DiagnosticSeverity.java` ✅ (إنشاء: Enum للخطورة)
- `semantic/diagnostics/Diagnostic.java` ✅ (إنشاء: رسالة واحدة مع SourceRange)
- `semantic/diagnostics/DiagnosticCollector.java` ✅ (إنشاء: مجمع ومدير للرسائل)

**TODOs**: Member 5 (تنفيذ طباعة وتصفية التقارير).

---

### 3) AST Updates (Flask)
- تعديل: `AST/ASTNode.java` + `AST/expr/Expression.java` + `AST/stmt/Statement.java` + `AST/suite/Suite.java`
- تعديل: جميع عقد Flask في `AST/expr`, `AST/stmt`, `AST/literal`, `AST/suite`

**TODOs**: Member 2.

---

### 4) AST Updates (Template)
- تعديل: `AST/template/TemplateNode.java`
- تعديل: جميع عقد Template في `AST/template/html`, `AST/template/css`, `AST/template/jinja`

**TODOs**: Member 3.

---

### 5) Symbol Tables (New)
**Flask**
- `semantic/symbols/flask/FlaskSymbolTable.java`
- `semantic/symbols/flask/FlaskSymbol.java`
- `semantic/symbols/flask/FlaskScope.java`
- `semantic/symbols/flask/FlaskSymbolKind.java`
- `semantic/symbols/flask/FlaskScopeKind.java`

**Template**
- `semantic/symbols/template/TemplateSymbolTable.java`
- `semantic/symbols/template/TemplateSymbol.java`
- `semantic/symbols/template/TemplateScope.java`
- `semantic/symbols/template/TemplateSymbolKind.java`
- `semantic/symbols/template/TemplateScopeKind.java`

**TODOs**: Member 4.

---

### 6) Bridge (Flask → Template)
- `semantic/bridge/TemplateContext.java` ✅ (إنشاء: سياق رمز من Template)
- `semantic/bridge/TemplateContextBridge.java` ✅ (إنشاء: جسر ربط Flask + Template)

**TODOs**: Member 5 (تنفيذ خوارزمية الربط والاستدلال من السياق).

---

### 7) Visitors Wiring (AST Visitors)
- تعديل: `AST/visitor/FlaskVisitor.java`
- تعديل: `AST/visitor/ProgramVisitor.java`
- تعديل: `AST/template/TemplateVisitor.java`

**TODOs**: Member 2 (Flask), Member 3 (Template).

---

### 8) Tests Scaffolding
- `tests/semantic/` (placeholders)
- `tests/ast/` (placeholders)
- `tests/template/` (placeholders)

**TODOs**: Member 1.

---

## اختبار نهاية الأسبوع (هيكلي)

- المشروع يبني بدون implementations.
- جميع الملفات موجودة وبها TODOs واضحة.
- لا يوجد اعتماد على Symbol Table القديم.

---

نهاية الخطة.
