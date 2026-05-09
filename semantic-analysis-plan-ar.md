# خطة تنفيذ Semantic Analysis (3 أسابيع) — مشروع Jinja2Compiler

التاريخ: 2026-05-08

هذه الخطة تركّز على معالجة الأخطاء المطلوبة:
- Type Error
- Type Mismatch
- Undefined Variables
- Scope Error
- Missing Flask Variable
- أي استعمال متغير داخل تعبير Jinja2 غير معرف في Flask

الخطة مرنة وقابلة للتغيير حسب تقدّم التنفيذ.

---

## ملخص الحالة الحالية

- الـ AST يحتاج إضافات إلزامية (getters + معلومات موقع + ربط الرموز + نوع مستنتج).
- الـ Symbol Table الحالي متضارب (نموذجين مختلفين)، ويُفضّل إعادة بنائه بشكل موحّد من الصفر.
- لا يوجد نظام تشخيص (Diagnostics) موحّد للأخطاء.

**قرار مقترح**: إعادة بناء Symbol Table + إضافة طبقة Semantic Analysis متعددة المراحل.

---

## الأسبوع 1: تثبيت البنية الأساسية (AST + Symbol Table)

### أهداف الأسبوع
- جعل الـ AST مناسب للتحليل الدلالي.
- بناء Symbol Table موحّد وقابل للتوسّع.
- تجهيز نظام تشخيص أخطاء عام.

### المهام

1) **تعديلات إلزامية على AST**
- إضافة getters صريحة لكل عقدة بدل الاعتماد على `getChildren()` فقط.
- إضافة معلومات موقع أدق (line/column وربما start/end).
- إضافة حقل نوع مستنتج (inferredType) في expressions.
- إضافة رابط من Identifier إلى Symbol بعد مرحلة resolution.

2) **إعادة بناء Symbol Table**
- اعتماد نموذج واحد فقط (Scope Tree موصى به).
- تعريف:
  - SymbolKind: VARIABLE, FUNCTION, CLASS, PARAMETER
  - ScopeType: GLOBAL, FUNCTION, CLASS, BLOCK, LOOP
  - نوع الرمز (declaredType / inferredType)
- توفير APIs واضحة:
  - `enterScope`, `exitScope`, `declare`, `lookup`, `lookupCurrent`
- منع التضارب في التعريف داخل نفس النطاق.

3) **نظام تشخيص موحّد (Diagnostics)**
- تعريف كائن Diagnostic:
  - code, message, line, column, severity
- تجميع الأخطاء في قائمة بدل `System.err`.

### مخرجات الأسبوع 1
- AST مُحسّن وقابل للربط الدلالي.
- Symbol Table موحّد وجاهز للتحليل.
- نظام Diagnostics قابل للاستخدام في كل المراحل.

---

## الأسبوع 2: بناء مراحل التحليل الدلالي الأساسية

### أهداف الأسبوع
- اكتشاف Undefined Variables وScope Error.
- بناء Inference بسيط للأنواع.
- تطبيق Type Error وType Mismatch.
- ربط متغيرات Jinja2 بمتغيرات Flask.

### المراحل المقترحة

1) **Resolution Pass (ربط الرموز)**
- بناء جدول الرموز لـ Flask.
- ربط كل Identifier بالـ Symbol.
- إنتاج أخطاء: Undefined Variables + Scope Error.

2) **Type Inference (أساسي وبسيط)**
- دعم الأنواع المبدئية: Number, String, Bool, None, List, Set, Unknown.
- تعيين نوع المتغير من الإسناد.
- دعم نشر نوع العمليات البسيطة.

3) **Type Checking**
- Type Error: عمليات على أنواع غير متوافقة.
- Type Mismatch: إسناد نوع لا يطابق المتغير.
- فحص شروط if/while كقيمة منطقية.

4) **Jinja ↔ Flask Binding**
- استخراج كل المتغيرات المستخدمة داخل Jinja Expressions.
- التأكد أنها معرفة في Flask أو passed إلى `render_template`.
- إنتاج أخطاء: Missing Flask Variable أو Undefined Jinja Variable.

### مخرجات الأسبوع 2
- Passes واضحة ومنفصلة.
- Diagnostics دقيقة مع مواقع الخطأ.

---

## الأسبوع 3: صقل، توسعة، واختبارات

### أهداف الأسبوع
- رفع دقة الأنواع.
- تحسين رسائل الأخطاء.
- تجهيز اختبارات للسيناريوهات السلبية.

### المهام

1) **توسعة قواعد النوع**
- دعم نوع الاستدعاء (function return type) إن توفّر.
- دعم الأنواع المركبة (List<Number> إلخ) إن أمكن.

2) **تحسين Diagnostics**
- توحيد صياغة الأخطاء (code + message واضح).
- ترتيب الأخطاء حسب السطر.

3) **اختبارات سلبية**
- ملفات مخصصة لكل نوع خطأ.
- مقارنة النتائج مع قائمة متوقعة.

### مخرجات الأسبوع 3
- Semantic Analysis مستقر وقابل للتوسعة.
- حزمة اختبارات واضحة لتغطية الأخطاء المطلوبة.

---

## آليات معالجة الأخطاء المطلوبة

- **Undefined Variables**: اكتشاف في Resolution Pass.
- **Scope Error**: اكتشاف في Resolution Pass (خروج من scope أو shadowing غير مسموح).
- **Type Error**: في Type Checking (عملية على نوع غير صحيح).
- **Type Mismatch**: في Type Checking (إسناد أو return غير متوافق).
- **Missing Flask Variable**: في Jinja Binding (متغير Jinja غير موجود في Flask).
- **Jinja undefined**: نفس المرحلة السابقة.

---

## قرار بخصوص Symbol Table

**النتيجة**: الوضع الحالي غير مناسب للمرحلة القادمة بسبب وجود نموذجين متضاربين. الأفضل إعادة بناء موحدة من الصفر مع إعادة استخدام بعض الأسماء لتسهيل الدمج.

---

## ملاحظات مرونة الخطة

- يمكن دمج الأسبوعين 2 و3 إذا تم تقليص تفاصيل النوع.
- يمكن تأجيل دعم بعض القواعد المتقدمة (List/Set generic types) إلى نسخة لاحقة.

---

نهاية الخطة.
