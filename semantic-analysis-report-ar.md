# تقرير جاهزية التحليل الدلالي (Semantic Analysis) لمشروع Jinja2Compiler

التاريخ: 2026-05-08

هذا التقرير منظم بحسب الملفات، ويركز على مدى جاهزية المشروع لإضافة مرحلة التحليل الدلالي (Semantic Analysis).  
تم التركيز على الجوانب العملية مثل:

- قواعد الأنواع (Type Rules)
- تحليل الرموز (Symbol Resolution)
- إدارة المجالات (Scopes)
- تحسين رسائل الأخطاء والتشخيص (Diagnostics)

---

# شرح المصطلحات

- **الدور (Role):** الوظيفة الأساسية للملف.
- **ملاحظات (Notes):** ما يقوم به الملف حاليًا.
- **توصيات التحليل الدلالي:** التعديلات أو الفحوصات المطلوبة لدعم Semantic Analysis.

---

# الملفات الرئيسية (Root Files)

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| README.md | نظرة عامة على المشروع | يشرح بنية المشروع وأهدافه | إضافة قسم خاص بالتحليل الدلالي يتضمن قواعد الرموز والأنواع وآلية الإبلاغ عن الأخطاء |
| ast.dot | مخرجات AST | ملف Graphviz ناتج عن آخر تشغيل | اعتباره ملفًا مولدًا تلقائيًا مع إمكانية إضافة نسخة ثابتة داخل docs/ لاختبارات مستقرة |
| Jinja2Compiler.iml | إعدادات IntelliJ | بيانات المشروع الخاصة بـ IntelliJ | لا تأثير دلالي له |

---

# src/Main.java

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| src/Main.java | نقطة البداية للمشروع | يبني Flask AST ويولد ast.dot. تدفق Symbol Table و Template معلق حاليًا | إضافة خيارات CLI لاختيار Flask أو Template وإضافة مرحلة Semantic مستقلة تجمع الأخطاء دون إيقاف التنفيذ مباشرة |

---

# src/antlr (Grammars المكتوبة يدويًا)

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| src/antlr/FlaskLexer.g4 | Lexer خاص بـ Flask | يحتوي على Tokens شبيهة بـ Python مع INDENT/DEDENT والعوامل والقيم | الحفاظ عليه كما هو مع التأكد من استقرار NEWLINE و INDENT لتحسين دقة مواقع الأخطاء |
| src/antlr/FlaskParser.g4 | Parser خاص بـ Flask | يعرّف العبارات والتعابير والأولوية | يفضل إضافة Labels واضحة للقواعد لتحسين سياق الأخطاء الدلالية |
| src/antlr/TemplateLexer.g4 | Lexer للقوالب | Lexer متعدد الأنماط لـ HTML/Jinja/CSS | إضافة Diagnostics أو Token Channels لاكتشاف الانتقالات غير الصحيحة بين الـ Modes |
| src/antlr/TemplateParser.g4 | Parser للقوالب | يدعم HTML و CSS و Jinja | إضافة قواعد error/invalid لتحسين التعافي من الأخطاء |

---

# src/antlr (ملفات مولدة بواسطة ANTLR - لا تعدلها)

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| src/antlr/FlaskLexer.java | Lexer مولد | ناتج ANTLR | إعادة التوليد عند تعديل الـ Grammar |
| src/antlr/FlaskParser.java | Parser مولد | ناتج ANTLR | ملف مولد فقط |
| src/antlr/TemplateLexer.java | Lexer مولد | ناتج ANTLR | ملف مولد فقط |
| src/antlr/TemplateParser.java | Parser مولد | ناتج ANTLR | ملف مولد فقط |
| بقية ملفات tokens/interp/listener/visitor | ملفات مساعدة مولدة | Metadata وواجهات ANTLR | لا تحتوي منطق Semantic فعلي |

---

# src/AST (الأساس)

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| src/AST/ASTNode.java | العقدة الأساسية للـ AST | تحتوي nodeName و lineNumber | إضافة getSourceRange() لتحسين رسائل الأخطاء وتوحيد getChildren() |
| src/AST/Program.java | الجذر الرئيسي للـ AST | يحتوي قائمة Statements | إضافة Getter للوصول للـ Statements |
| src/AST/ASTGraphvizPrinter.java | طباعة AST | يعتمد على toString() | إضافة Semantic Debug Mode لإظهار الأنواع والرموز |

---

# src/AST/expr

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| Expression.java | الأساس للتعابير | Base Class | إضافة inferredType أو خريطة خارجية للأنواع |
| IdentifierExpr.java | Identifier | يخزن الاسم | ربط الـ Identifier بمدخل الـ Symbol Table |
| AttributeExpr.java | الوصول للخصائص | يمثل obj.attr | التحقق من وجود الخاصية |
| IndexExpr.java | الفهرسة | يمثل obj[index] | التحقق من قابلية الفهرسة |
| CallExpr.java | استدعاء دالة | يحوي callee و args | التحقق من عدد وأنواع المعاملات |
| BinaryExpr.java | العمليات الثنائية | left/op/right | إضافة قواعد توافق الأنواع |
| CompareExpr.java | المقارنات | سلسلة مقارنات | فرض توافق الأنواع |
| UnaryExpr.java | العمليات الأحادية | + أو - | التأكد أن المعامل رقمي |
| NotExpr.java | not expr | عملية منطقية | فرض Boolean أو Truthy Rules |
| LambdaExpr.java | Lambda | Parameters + body | التحقق من Shadowing والاستنتاج النوعي |

---

# src/AST/literal

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| LiteralExpr.java | أساس الـ Literals | Base Class | إضافة معلومات نوعية |
| NumberLiteralExpr.java | رقم | القيمة كنص | إضافة نوع الرقم int/float |
| StringLiteralExpr.java | نص | النص الخام | تطبيع النص ومعالجة Escape |
| BooleanLiteralExpr.java | Boolean | true/false | ربطه بالنوع المنطقي |
| NoneLiteralExpr.java | None | قيمة فارغة | ربطه بـ NoneType |
| ListLiteralExpr.java | List | قائمة عناصر | التحقق من توافق أنواع العناصر |
| SetLiteralExpr.java | Set | مجموعة عناصر | التحقق من قابلية Hash |

---

# src/AST/stmt

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| Statement.java | الأساس للعبارات | Abstract Base | إضافة accept موحد لـ Semantic Visitors |
| AssignmentStmt.java | إسناد | target = value | التحقق من صحة الهدف وتوافق الأنواع |
| AssignmentChainStmt.java | إسناد متسلسل | a=b=c | التحقق من كل هدف |
| ExpressionStmt.java | تعبير كتعليمة | Expression فقط | إمكانية إضافة تحذيرات للنتائج غير المستخدمة |
| ReturnStmt.java | return | قيمة مرجعة | التحقق من نوع القيمة |
| IfStmt.java | if | شرط + blocks | فرض شرط منطقي وتتبع الـ Scope |
| WhileStmt.java | while | loop | التحقق من الشرط وتتبع loop context |
| ForStmt.java | for | iterable + body | التحقق من قابلية التكرار |
| FunctionDefStmt.java | تعريف دالة | name + params + body | منع التكرار واستنتاج نوع الإرجاع |
| ClassDefStmt.java | تعريف class | اسم + parent | التحقق من وجود الـ Parent |
| Decorator.java | Decorator | يلف expressions | التحقق من قابلية الاستدعاء |
| DecoratedStmt.java | عنصر مزين | decorators + target | تطبيق semantics بالترتيب |
| GlobalStmt.java | global | أسماء عامة | فرض قواعد global |
| DelStmt.java | del | حذف targets | التحقق من صحة الهدف |
| PassStmt.java | pass | لا يفعل شيئًا | لا يحتاج Semantic |
| BreakStmt.java | break | كسر loop | التأكد أنه داخل loop |
| ContinueStmt.java | continue | متابعة loop | التأكد أنه داخل loop |
| FromImportStmt.java | import | module + names | ربط المستوردات بالـ Symbol Table |

---

# src/AST/suite

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| Suite.java | Base Suite | أساس الـ Blocks | توحيد آلية traversal |
| InlineSuite.java | تعليمة واحدة | Single Statement | التعامل معه كـ Block عادي |
| BlockSuite.java | Block | قائمة تعليمات | فتح وإغلاق Scope |

---

# src/AST/visitor

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| ProgramVisitor.java | بناء الـ AST | يبني Program | إضافة نقطة دخول للـ Semantic Phase |
| FlaskVisitor.java | Visitor لـ Flask | يحول ParseTree إلى AST | التأكد من تغطية كل قواعد الـ Grammar |

---

# src/AST/template

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| TemplateNode.java | الأساس للقوالب | Base Node | إضافة معلومات عن المصدر HTML/CSS/Jinja |
| TemplateVisitor.java | بناء AST للقوالب | يبني Html/CSS/Jinja AST | إضافة Hooks للفحوصات الدلالية |

---

# src/AST/template/html

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| HtmlNode.java | أساس HTML | Base Class | إضافة Source Range |
| HtmlDocument.java | جذر HTML | أبناء HTML | إضافة Getters |
| HtmlElement.java | عنصر HTML | tag + attributes | التحقق من توافق التاغات والخصائص |
| HtmlNormalElement.java | عنصر عادي | يحوي children | التحقق من التاغات المفتوحة والمغلقة |
| HtmlSelfClosingElement.java | Self Closing | tag + attrs | التحقق من السماح بالإغلاق الذاتي |
| HtmlVoidElement.java | Void Element | tag + attrs | منع المحتوى داخلها |
| HtmlStyleElement.java | style | يحوي CSS AST | التحقق من CSS و Jinja داخله |
| HtmlAttribute.java | Attribute | name/value | كشف التكرار والتحقق من الصحة |
| HtmlText.java | Text | نص خام | لا تغييرات مهمة |

---

# src/AST/template/css

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| CssNode.java | أساس CSS | Base Class | إضافة Source Range |
| CssStylesheet.java | Stylesheet | Rules + Jinja | التحقق من التداخل غير الصحيح |
| CssSelector.java | Selector | نص selector | التحقق من الصياغة |
| CssRule.java | Rule | selector + block | التحقق من الخصائص |
| CssDeclaration.java | Declaration | property + value | التحقق من القيم المسموحة |
| CssValue.java | Value | أجزاء متعددة | التحقق من الوحدات والدوال |
| CssValuePart.java | جزء قيمة | Base Class | إضافة نوع CSS |
| CssPrimitiveValue.java | قيمة بسيطة | نص CSS | تحويله لأنواع فعلية |
| CssFunctionCall.java | دالة CSS | function + args | التحقق من الوسائط |
| CssJinjaExpressionValue.java | Jinja داخل CSS | Expression | التأكد من توافق النوع |
| CssJinjaValueIf.java | شرط داخل CSS | if expression | التحقق من توافق الأنواع |

---

# src/AST/template/jinja

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| JinjaNode.java | أساس Jinja | Base Class | ربطه بالـ Symbol Resolution |
| JinjaBody.java | جسم Jinja | HTML/CSS/Jinja | التحقق من الـ Scopes |

---

# src/AST/template/jinja/expr

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| JinjaExpr.java | أساس التعبيرات | Base Class | إضافة inferredType |
| JinjaIdentifierExpr.java | Identifier | name | كشف المتغيرات غير المعرفة |
| JinjaAttrExpr.java | Attribute | obj.attr | التحقق من وجود الخاصية |
| JinjaCallExpr.java | Call | function + args | التحقق من المعاملات |
| JinjaFilterExpr.java | Filter | base + filter | التحقق من وجود الفلتر |
| JinjaBinaryExpr.java | Binary | left/op/right | التحقق من التوافق النوعي |
| JinjaUnaryExpr.java | Unary | not expr | التحقق من المنطق |
| JinjaNumberLiteralExpr.java | Number | literal | ربطه بالنوع الرقمي |
| JinjaStringLiteralExpr.java | String | literal | ربطه بالنوع النصي |

---

# src/AST/template/jinja/stmt

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| JinjaStmt.java | أساس التعليمات | Base Class | إضافة Semantic Visitor |
| JinjaIfStmt.java | if | condition + body | فرض شرط منطقي |
| JinjaElifClause.java | elif | condition + body | نفس قواعد if |
| JinjaForStmt.java | for | vars + iterable | تعريف المتغيرات ضمن Scope |
| JinjaIncludeStmt.java | include | اسم قالب | التحقق من وجود الملف |
| JinjaBlockStmt.java | block | block + body | منع تكرار أسماء blocks |
| JinjaExtendsStmt.java | extends | base template | التحقق من وجود القالب الأب |

---

# src/SymbolTable

| الملف | الدور | ملاحظات | توصيات التحليل الدلالي |
| --- | --- | --- | --- |
| FlaskSymbolTable.java | Symbol Table | Stack Scopes | إضافة معلومات الأنواع |
| SymbolTableVisitor.java | بناء Symbol Table | يزور الـ AST | تحويله لـ Semantic Pass حقيقي |
| Scope.java | شجرة Scopes | نموذج بديل | اختيار نموذج واحد فقط |
| Symbol.java | رمز | name/type/params | إضافة mutability ونوع التعريف |

---

# الاختبارات (Tests)

هذه الملفات عبارة عن Inputs للاختبار وليست Source Code.  
يفضل إضافة اختبارات Semantic تغطي:

- متغيرات غير معرفة
- أخطاء أنواع
- عدد معاملات خاطئ
- break/continue غير قانونية
- أخطاء Filters و Jinja

---

# خارطة طريق مقترحة للتحليل الدلالي

1. بناء Semantic Pass مستقل يجمع الأخطاء والتحذيرات.
2. توحيد نموذج الـ Symbol Table وإضافة أنواع البيانات.
3. إضافة Getters مفقودة في الـ AST Nodes.
4. ربط الـ Identifiers بالـ Symbols أثناء الـ Resolution.
5. بناء نظام Types بسيط لـ Flask و Jinja.
6. إضافة اختبارات Semantic للحالات الخاطئة.

---

# نهاية التقرير