# Member 5: Diagnostics + Bridge (دليل التنفيذ)

**التاريخ:** 2026-05-14  
**آخر تحديث:** تحديث شامل: Enums + Helper Methods + TypeKind

---

## نظرة عامة

Member 5 يشارك في مرحلة **التحليل الدلالي (Semantic Analysis)** للمشروع بقسمين:

### 1. **Diagnostics System** (`semantic/diagnostics/`)
نظام تجميع وإدارة الرسائل التشخيصية (أخطاء، تحذيرات، معلومات، تلميحات) مع:
- **ErrorCode enum** لرموز موحدة (E001-E203, W101-W104, etc.)
- **TypeKind enum** لأنواع آمنة بدل Strings
- **ResolutionStatus enum** لحالات حل الرموز
- **Helper methods** في DiagnosticCollector لتوليد diagnostics موحدة

### 2. **Context Bridge** (`semantic/bridge/`)
جسر يربط سياق Flask مع Template لتحليل cross-context وتوليد رسائل تشخيص ذات معنى.

---

## البنية الجديدة

### Diagnostics (`semantic/diagnostics/`)

```
semantic/diagnostics/
├── DiagnosticSeverity.java      # Enum: ERROR, WARNING, INFO, HINT
├── ErrorCode.java               # ✅ NEW: Enum E0xx, E1xx, E2xx, W0xx, I0xx, H0xx
├── TypeKind.java                # ✅ NEW: Enum INT, STR, LIST, UNKNOWN, ANY, ...
├── ResolutionStatus.java        # ✅ NEW: Enum RESOLVED, UNDEFINED, OUT_OF_SCOPE, ...
├── Diagnostic.java              # ✅ UPDATED: استخدام ErrorCode + TypeKind
└── DiagnosticCollector.java     # ✅ UPDATED: + 10+ helper methods
```

#### `ErrorCode.java` 

Enum مركزي يجمع **كل رموز الأخطاء الممكنة**:

**E0xx - Undefined/Not Found:**
- `E001_UNDEFINED_VARIABLE`: متغير غير معرّف
- `E002_UNDEFINED_FUNCTION`: دالة غير معرّفة
- `E003_UNDEFINED_CLASS`: كلاس غير معرّف
- `E004_MISSING_FLASK_VARIABLE`: متغير Flask context غير متاح
- `E005_UNDEFINED_ATTRIBUTE`: خاصية غير معرّفة

**E1xx - Type Errors:**
- `E101_TYPE_MISMATCH`: عدم توافق أنواع (expected int, got str)
- `E102_TYPE_ERROR`: خطأ نوع في عملية (str + int)
- `E103_INCOMPATIBLE_TYPES`: أنواع غير متوافقة في المقارنة

**E2xx - Scope Errors:**
- `E201_DUPLICATE_DEFINITION`: تعريف مكرر في نفس النطاق
- `E202_USE_BEFORE_DEFINITION`: استخدام قبل التعريف
- `E203_OUT_OF_SCOPE`: متغير خارج نطاقه

**W0xx - Warnings:**
- `W101_SHADOWING`: متغير يظلل متغير آخر
- `W102_UNUSED_SYMBOL`: متغير غير مستخدم
- `W103_IMPLICIT_CONVERSION`: تحويل نوع ضمني
- `W104_DEPRECATED_SYNTAX`: صياغة مهجورة

**I0xx - Info:**
- `I001_SYMBOL_RESOLVED`: رمز تم حله بنجاح
- `I002_TYPE_INFERRED`: نوع تم استدلاله

**H0xx - Hints:**
- `H001_SUGGESTION`: اقتراح تصحيح
- `H002_AVAILABLE_SYMBOLS`: رموز متاحة

**الفوائد:**
- توحيد الرموز عبر المشروع
- سهولة الفلترة والبحث
- consistency في الرسائل
- دعم IDE (quick fixes)

#### `TypeKind.java` (جديد ✅)

Enum موحد للأنواع **بدل String type names**:

```java
TypeKind.INT           // بدل "int"
TypeKind.STR           // بدل "string" / "str"
TypeKind.LIST          // بدل "list"
TypeKind.DICT          // بدل "dict"
TypeKind.UNKNOWN       // نوع غير معروف (recovery)
TypeKind.ANY           // أي نوع (dynamic)
```

**Methods مهمة:**
- `isCompatibleWith(TypeKind other)`: هل compatible للإسناد؟
- `isContainerType()`: هل container (LIST, DICT, SET, TUPLE)?
- `isPrimitiveType()`: هل primitive (INT, FLOAT, STR, BOOL)?
- `isMetaType()`: هل meta (UNKNOWN, ANY, UNION)?

**الفوائد:**
- Type safety بدل Strings
- استدلال أنواع آمن
- Type checking سهل

#### `ResolutionStatus.java` (جديد ✅)

Enum لحالة **حل الرموز** (Symbol Resolution Status):

```java
RESOLVED          // وُجد الرمز بنجاح
UNDEFINED         // لم يُعثر على الرمز
OUT_OF_SCOPE      // الرمز موجود لكن خارج النطاق
AMBIGUOUS         // عدة رموز بنفس الاسم (conflict)
PARTIAL           // وُجد لكن بمعلومات ناقصة (type unknown)
SHADOWED          // الرمز صحيح لكن يظلل آخر (warning)
UNAVAILABLE       // غير متاح حاليًا
```

**Methods:**
- `isResolved()`: هل تم الحل؟
- `isError()`: هل خطأ (UNDEFINED, OUT_OF_SCOPE, AMBIGUOUS)?
- `isWarning()`: هل تحذير (SHADOWED)?

**الفوائد:**
- semantic flow نظيف (بدون null checks)
- أسباب واضحة للفشل
- سهولة الاستدلالات

#### `Diagnostic.java` (محدّث ✅)

رسالة تشخيصية **بدون String codes**:

```java
new Diagnostic(
    sourceRange,
    ErrorCode.E001_UNDEFINED_VARIABLE,  // Enum, ليس String
    "Undefined variable 'x'",
    "Did you mean 'X'?",
    TypeKind.UNKNOWN  // optional: for type errors
)
```

**التغييرات الرئيسية:**
- `code` → `errorCode` (ErrorCode enum)
- `severity` → مشتقة من `errorCode.getSeverity()`
- إضافة `relatedType` (TypeKind)
- تنفيذ `compareBySeverity()` (sorting)

#### `DiagnosticCollector.java` (محدّث + Helper Methods ✅)

**10+ Helper Methods لتوليد diagnostics موحدة:**

```java
collector.reportUndefinedVariable(range, "x", "Did you mean 'X'?");
collector.reportTypeMismatch(range, "items", TypeKind.LIST, TypeKind.DICT);
collector.reportTypeError(range, "addition of str and int", suggestion);
collector.reportScopeError(range, "x", ErrorCode.E203_OUT_OF_SCOPE, message, hint);
collector.reportMissingFlaskVariable(range, "request", "Available: user, config");
collector.reportShadowing(range, "name", "name", "Flask context at app.py:30");
collector.reportUnusedSymbol(range, "temp");
collector.reportSymbolResolved(range, "user", "Flask context");
collector.reportTypeInferred(range, "items", TypeKind.LIST);
collector.reportSuggestion(range, "Did you mean...?");
collector.reportAvailableSymbols(range, "name, email, phone");
```

**الفوائد:**
- توحيد صياغة الرسائل
- تقليل التكرار
- سهولة الصيانة

---

### Bridge (`semantic/bridge/`)

#### `TemplateContext.java` (محدّث ✅)

سياق رمز من Template **مع TypeKind بدل String**:

```java
new TemplateContext(
    templateSymbol,
    flaskSymbol,
    SymbolOrigin.FLASK_CONTEXT,
    TypeKind.INT,  // استخدام TypeKind enum
    "From app.py:42"
)
```

**الحقول:**
- `templateSymbol`: الرمز من Template
- `flaskSymbol`: الرمز المقابل من Flask (اختياري)
- `origin`: أصل الرمز (FLASK_CONTEXT, LOCAL, PARENT_TEMPLATE, etc.)
- `type`: TypeKind (يُعيّن افتراضيًا إلى UNKNOWN)
- `sourceHint`: تلميح سياق (اختياري)

#### `TemplateContextBridge.java` (محدّث ✅)

جسر ربط **يستخدم helper methods من DiagnosticCollector**:

```java
bridge.generateUndefinedSymbolDiagnostic(name, range, suggestions);
  // ينادي: collector.reportUndefinedVariable(...)

bridge.generateTypeMismatchDiagnostic(name, TypeKind.LIST, TypeKind.DICT, range);
  // ينادي: collector.reportTypeMismatch(...) مع TypeKinds

bridge.generateShadowingDiagnostic(name, flaskName, range);
  // ينادي: collector.reportShadowing(...)
```

---

## Diagnostic Types للمقابلة (الأخطاء المطلوبة)

### 1. **Undefined Variable** (E001)
```
[ERROR] [5:10] [E001] Undefined variable 'user'
  Hint: Did you mean 'username'?
```

### 2. **Type Mismatch** (E101)
```
[ERROR] [15:3] [E101] Type mismatch for 'items': expected List but got Dict
```

### 3. **TypeError** (E102)
```
[ERROR] [20:5] [E102] Type error: cannot add str and int
```

### 4. **Scope Error** (E203)
```
[ERROR] [8:7] [E203] Out of scope: variable 'loop_index' used outside loop
```

### 5. **Missing Flask Variable** (E004)
```
[ERROR] [12:10] [E004] Flask context variable 'request' not available
  Hint: Available: user, config, session
```

### 6. **Shadowing** (W101)
```
[WARNING] [6:3] [W101] Variable 'name' shadows Flask variable at app.py:30
```

---

## Files Created/Updated

| File | Status | التغييرات |
|------|--------|---------|
| ErrorCode.java | ✅ NEW | Enum: E/W/I/H codes |
| TypeKind.java | ✅ NEW | Enum: INT, STR, LIST, UNKNOWN, ANY |
| ResolutionStatus.java | ✅ NEW | Enum: RESOLVED, UNDEFINED, OUT_OF_SCOPE, ... |
| Diagnostic.java | ✅ UPDATED | ErrorCode + TypeKind + sorting |
| DiagnosticCollector.java | ✅ UPDATED | + 10+ helper methods |
| TemplateContext.java | ✅ UPDATED | TypeKind بدل String |
| TemplateContextBridge.java | ✅ UPDATED | استخدام helpers |

---

## Integration Points

### مع Member 4 (Symbol Tables)
- ملء `TypeKind` للرموز
- تتبع `ResolutionStatus` عند البحث
- ملء SourceRange دقيق

### مع Main.java
- استدعاء `collector.reportAll()` في النهاية
- استخدام helpers من collector مباشرة

---

## Next Steps for Member 5

- [ ] تنفيذ خوارزمية `bridge()` الكاملة
- [ ] تنفيذ resolver للرموز
- [ ] اختبارات شاملة لكل نوع خطأ
- [ ] integration مع Main

---

**نهاية دليل Member 5 (محدّث)**
