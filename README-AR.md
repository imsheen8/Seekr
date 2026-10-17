# Seekr كتطبيق أندرويد (مدمج بالكامل، بلا سيرفر)

> مبني على [SearXNG](https://github.com/searxng/searxng) (AGPL-3.0-or-later) — راجع ملف `NOTICE`.

المعمارية (نفس فكرة QUARK AI): **Capacitor** (قشرة WebView) + **Chaquopy** (بايثون داخل الـ APK) + بناء عبر **GitHub Actions**.

```
التطبيق ─► MainActivity ─► Python: seekr_android.start()
                              └─ Seekr الحقيقي (searx/) عبر werkzeug على 127.0.0.1:18888
WebView  ◄─ www/index.html (شاشة تحميل) ─► /__auth?t=<رمز> ─► واجهة Seekr
```

## شكل الواجهة
الواجهة على طراز **كروم للأندرويد / جوجل**: ألوان جوجل (فاتح وداكن)، شريط بحث كبسولي، أقسام كتبويبات بخط سفلي، نتائج مسطّحة (أيقونة الموقع + النطاق + عنوان أزرق + وصف)، وصفحة التفضيلات كإعدادات كروم (صفوف بطاقية بعنوان ووصف ومفتاح تبديل). التنسيق كله في `searx/static/themes/simple/custom.css`، والخط خط النظام (بلا تحميل من الإنترنت). تحديد النص ونسخه معطّل في كل التطبيق عدا حقل البحث.

## صفحة الإعدادات (طراز Chrome)
أُعيد بناؤها بالكامل: القوالب في `searx/templates/simple/preferences.html` و`preferences/*.html` (مكوّنات الصفوف في `preferences/_cs.html`)، والتنسيق في `searx/static/themes/simple/chrome-settings.css`، والسلوك في `chrome-settings.js`. التنقل بين الصفحات الفرعية بـ CSS (`:target`) فيعمل زر الرجوع في أندرويد، والـ JS للتحسينات فقط (القيمة الحالية، البحث في الإعدادات، شريط الحفظ).

## البناء
1. ارفع محتويات هذا المجلد إلى مستودع GitHub (فرع `main`).
2. Actions ← **Build APK** ← Run workflow. الناتج: artifact باسم `seekr-apk`.

## ما الذي تغيّر عن Seekr الأصلي؟
- `searx/` كما هو تمامًا (إضافة `version_frozen.py` فقط لأن git غير موجود على الهاتف).
- `android-python/curl_cffi/`: بديل فوق `requests` (حزمة curl_cffi الأصلية تحتاج مكتبات C/Rust غير متوفرة لأندرويد).
- `android-python/msgspec/`: بديل بايثون خالص لـ `msgspec` (Struct + التحقق من الأنواع).
- الخادم: werkzeug بدل granian. الحماية: رمز عشوائي لكل تشغيل + كوكي HttpOnly، فلا يستطيع تطبيق آخر على الهاتف استخدام الخادم.

## حدود يجب معرفتها
- **لا يوجد انتحال لبصمة TLS للمتصفح** (كان curl_cffi يفعله). محركات تعتمد على ذلك (جوجل، DuckDuckGo للويب…) قد تفشل أو تطلب CAPTCHA؛ بقية المحركات (Bing، Brave، Startpage، Mojeek، Wikipedia…) لا تتأثر عادة. يمكنك تعطيل المحركات الفاشلة من إعدادات الواجهة.
- لم يُبنَ الـ APK ولم يُجرَّب على جهاز حقيقي أثناء إعداد هذا المشروع (لا إنترنت/Android SDK في بيئة العمل). المختبَر محليًا: البدائل، بحث كامل عبر Seekr الحقيقي ضد محرك وهمي، وسكربت التعديل على مشروع Capacitor مصطنع.
- `lxml` و`pyyaml` و`markupsafe` حزم أصلية تأتي من مستودع Chaquopy؛ إن فشل البناء عند هذه الخطوة، عدّل الإصدارات في `requirements-android.txt`.
- أول تشغيل قد يأخذ بعض الوقت (استيراد ~200 محرك).

## الإعدادات والبيانات
ملف الإعدادات: `files/seekr-data/settings.yml` داخل بيانات التطبيق (يُنشأ مرة واحدة ولا يُستبدل). عند التعديل لاحقًا غالبًا ستغيّر `searx/` أو هذا الملف الافتراضي في `android-python/seekr_android.py`.

## اختبارات محلية (على الحاسوب)
```
pip install flask flask-babel jinja2 pygments python-dateutil pyyaml babel markdown-it-py isodate whitenoise typing-extensions certifi requests lxml typer-slim valkey
PYTHONPATH=.:android-python python tests/test_shims.py
PYTHONPATH=.:android-python python tests/test_e2e_search.py
```
