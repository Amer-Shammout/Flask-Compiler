# اعتماديات مهام الأعضاء (Dependencies)

التاريخ: 2026-05-13

هذا الملف يوضح الاعتماديات بين مهام الأعضاء الخمسة بصورة مختصرة وواضحة لتنسيق العمل.

---

## نظرة عامة سريعة

- **Member 1 (Integrator / Structure):** يُنشئ الهيكل والملفات مع TODOs؛ يجب أن تكون هذه الملفات موجودة قبل بداية تنفيذ بقية الأعضاء.
- **Member 2 (Flask AST):** يبني AST لعقد Flask، ويملأ `SourceRange`/مواقع النص — مطلوب لـ Member 4 و Member 5.
- **Member 3 (Template AST):** يبني AST لعقد Template (HTML/CSS/Jinja) ويملأ `SourceRange` — مطلوب لـ Member 4 و Member 5.
- **Member 4 (Symbol Tables):** يبني جداول الرموز لـ Flask و Template ويزود `SymbolTableRepository` لربط الجداول — يعتمد على مخرجات Member 2 و Member 3.
- **Member 5 (Diagnostics + Bridge):** يبني نموذج التشخيص (`Diagnostic`)، و `TemplateContextBridge` لربط سياق Flask مع Template. يعتمد بشدة على Member 2–4.

---

## اعتماديات مفصّلة (بالترتيب)

1. Member 1 → (الجميع)
   - يجب أن تكون الملفات/الكلاسات الموجودة كهيكل حاضرة (TODOs). جميع الأعضاء يعتمدون على الهيكل لبدء التنفيذ.

2. Member 2 (Flask AST) → Member 4, Member 5
   - Member 4 يحتاج AST لعزل تعريفات الدوال/المتغيرات ولبناء Scope.
   - Member 5 يحتاج `SourceRange` وبيانات AST لربط الأخطاء بمكانها في الملف.

3. Member 3 (Template AST) → Member 4, Member 5
   - Member 4 يحتاج الـ Template AST لتسجيل متغيرات القالب والـ blocks والـ macros.
   - Member 5 يحتاج AST القالب و`SourceRange` لتوليد رسائل تشخيص دقيقة ولربط السياق مع Flask.

4. Member 2 & Member 3 → Member 4
   - Symbol table builders (Member 4) يجمعون المعلومات من كلا الشجرتين؛ لذلك تنفيذ بنّاءي جزئي أو كامل على كلا الجانبين مطلوب قبل اختبار البناة.

5. Member 4 → Member 5
   - `TemplateContextBridge` يحتاج واجهة لقراءة جداول الرموز (lookup, resolution) عبر `SymbolTableRepository`.
   - Diagnostics يعتمد على نتائج التحليل الرمزي (مثلاً undefined symbol, shadowing) لإنتاج رسائل مفيدة.

---

## نقاط تنفيذية وتوصيات زمنية

- يمكن لـ Member 5 البدء بكتابة تعريفات الواجهات والنماذج (`Diagnostic`, `DiagnosticCollector`, `TemplateContextBridge`) طالما أن Member 1 أنشأ الملفات.
- لكن للاختبار العملي والاندماج، Member 5 يحتاج على الأقل stubs وظيفية من Member 2/3 (AST مع `SourceRange`) و Member 4 (واجهات lookup في `SymbolTableRepository`).
- مقترح تسلسل عملي للدمج:
  1. Member 1 يوفر الهيكل.
  2. Member 2 و Member 3 يوفّران stubs/نماذج AST مع `SourceRange` (حتى لو جزئية).
  3. Member 4 ينفّذ وظائف lookup/define الأساسية و API بسيطة لـ `SymbolTableRepository`.
  4. Member 5 يربط الـ Bridge ويبدأ بتشغيل Diagnostics على حالات اختبار صغيرة.

---

## خلاصة قصيرة لعضو 5

- الاعتماد الأساسي: Member 5 يعتمد على Member 2، Member 3، وMember 4 (بترتيب: AST → جداول الرموز → جسر السياق/التشخيص).
- يمكن بدء العمل على تعريفات الملفات والنماذج فوراً، لكن الاختبار والتكامل يتطلّبان stubs من الأعضاء السابقين.

---

## ملاحظات إضافية

- إذا أردتم، أضيف مخطط mermaid يوضح العلاقات بصريًا.
