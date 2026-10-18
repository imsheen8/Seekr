package __APP_ID__;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

/** الإعدادات بتصميم كروم: صفحة رئيسية بأقسام وبطاقات مستديرة وصفحات فرعية، وتشمل كل إعدادات محرك Seekr. */
final class SettingsPages {
    private SettingsPages() {}

    interface Refreshable { void refresh(); }

    interface Builder { void build(Page p, LinearLayout list); }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    // =====================================================================
    //  هيكل الصفحة
    // =====================================================================
    static final class Page extends LinearLayout implements Refreshable {
        final BrowserView b;
        final LinearLayout list;
        final EditText search;
        private final Builder builder;

        Page(final BrowserView b, String title, String searchHint, Builder builder) {
            super(b.act);
            this.b = b;
            this.builder = builder;
            setOrientation(VERTICAL);
            setBackgroundColor(Theme.bg);
            setClickable(true);

            LinearLayout h = new LinearLayout(b.act);
            h.setOrientation(HORIZONTAL);
            h.setGravity(Gravity.CENTER_VERTICAL);
            h.setPadding(Theme.dp(8), Theme.dp(8), Theme.dp(16), Theme.dp(8));
            ImageView back = U.ibtn(b.act, Ico.BACK, Theme.text, new View.OnClickListener() {
                @Override public void onClick(View v) { b.popPage(); }
            });
            if (Theme.ar) back.setScaleX(-1f);
            h.addView(back);
            TextView t = U.tv(b.act, title, 22, Theme.text);
            t.setPadding(Theme.dp(12), 0, 0, 0);
            h.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));
            addView(h);

            if (searchHint != null) {
                LinearLayout box = new LinearLayout(b.act);
                box.setOrientation(HORIZONTAL);
                box.setGravity(Gravity.CENTER_VERTICAL);
                box.setBackground(Theme.round(Theme.surface, 26));
                ImageView ic = U.icon(b.act, Ico.SEARCH, 22, Theme.text);
                ((LinearLayout.LayoutParams) ic.getLayoutParams()).setMarginStart(Theme.dp(18));
                box.addView(ic);
                search = new EditText(b.act);
                search.setBackground(null);
                search.setSingleLine(true);
                search.setHint(searchHint);
                search.setTextColor(Theme.text);
                search.setHintTextColor(Theme.text);
                search.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                Theme.sp(search, 17);
                search.setPadding(Theme.dp(16), 0, Theme.dp(16), 0);
                box.addView(search, new LinearLayout.LayoutParams(0, MATCH, 1f));
                LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(MATCH, Theme.dp(52));
                bl.setMargins(Theme.dp(16), Theme.dp(4), Theme.dp(16), Theme.dp(12));
                addView(box, bl);
                search.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
                    @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
                    @Override public void afterTextChanged(Editable e) { refresh(); }
                });
            } else {
                search = null;
            }

            ScrollView sv = new ScrollView(b.act);
            sv.setFillViewport(true);
            list = new LinearLayout(b.act);
            list.setOrientation(VERTICAL);
            list.setPadding(0, 0, 0, Theme.dp(32));
            sv.addView(list, new FrameLayout.LayoutParams(MATCH, WRAP));
            addView(sv, new LinearLayout.LayoutParams(MATCH, 0, 1f));
            refresh();
        }

        String query() {
            return search == null ? "" : search.getText().toString().trim().toLowerCase();
        }

        @Override
        public void refresh() {
            list.removeAllViews();
            builder.build(this, list);
        }
    }

    // =====================================================================
    //  عناصر مشتركة
    // =====================================================================
    private static Drawable cardBg(float[] radii) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Theme.surface);
        g.setCornerRadii(radii);
        GradientDrawable m = new GradientDrawable();
        m.setColor(0xFFFFFFFF);
        m.setCornerRadii(radii);
        return new RippleDrawable(ColorStateList.valueOf(Theme.dark ? 0x33FFFFFF : 0x22000000), g, m);
    }

    /** يضع الصفوف في بطاقة واحدة: حواف خارجية كبيرة وفواصل صغيرة بين الصفوف مثل كروم. */
    static void group(LinearLayout list, View... rows) {
        int n = rows.length;
        float o = Theme.dp(26), in = Theme.dp(4);
        for (int i = 0; i < n; i++) {
            float tl = i == 0 ? o : in, bl = i == n - 1 ? o : in;
            rows[i].setBackground(cardBg(new float[]{tl, tl, tl, tl, bl, bl, bl, bl}));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.setMargins(Theme.dp(16), i == 0 ? 0 : Theme.dp(3), Theme.dp(16), 0);
            list.addView(rows[i], lp);
        }
    }

    static void section(LinearLayout list, String title) {
        TextView t = U.tv(list.getContext(), title, 14, Theme.dim);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        t.setPadding(Theme.dp(28), Theme.dp(24), Theme.dp(28), Theme.dp(10));
        list.addView(t);
    }

    static void note(LinearLayout list, String text) {
        TextView t = U.tv(list.getContext(), text, 14, Theme.dim);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        t.setPadding(Theme.dp(28), Theme.dp(10), Theme.dp(28), Theme.dp(10));
        list.addView(t);
    }

    static LinearLayout row(Context c, String title, String summary, View trailing, final Runnable click) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Theme.dp(60));
        row.setPadding(Theme.dp(20), Theme.dp(12), Theme.dp(20), Theme.dp(12));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        row.addView(col, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView tt = U.tv(c, title, 17, Theme.text);
        tt.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        col.addView(tt);
        if (summary != null && !summary.isEmpty()) {
            TextView s = U.tv(c, summary, 14, Theme.dim);
            s.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            col.addView(s);
        }
        if (trailing != null) {
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(trailing.getLayoutParams() == null ? WRAP : trailing.getLayoutParams().width, trailing.getLayoutParams() == null ? WRAP : trailing.getLayoutParams().height);
            tl.setMarginStart(Theme.dp(16));
            row.addView(trailing, tl);
        }
        if (click != null) {
            row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { click.run(); }
            });
        }
        return row;
    }

    /** يغيّر نص الملخّص (السطر الثاني) في صف أُنشئ بواسطة row(). */
    private static TextView summaryOf(View row) {
        LinearLayout col = (LinearLayout) ((LinearLayout) row).getChildAt(0);
        return col.getChildCount() > 1 ? (TextView) col.getChildAt(1) : null;
    }

    private static View toggleRow(Context c, String title, String summary, boolean on, final java.util.function.Consumer<Boolean> set) {
        final Pages.Toggle tg = new Pages.Toggle(c, on, new Pages.Toggle.Listener() {
            @Override public void onToggle(boolean now) { set.accept(now); }
        });
        return row(c, title, summary, tg, new Runnable() {
            @Override public void run() { tg.toggle(); }
        });
    }

    private static View storeToggle(final BrowserView b, String title, String summary, final String key, boolean def, final Runnable after) {
        return toggleRow(b.act, title, summary, b.store.getBool(key, def), new java.util.function.Consumer<Boolean>() {
            @Override
            public void accept(Boolean v) {
                b.store.setBool(key, v);
                if (after != null) after.run();
            }
        });
    }

    private static View cookieToggle(final BrowserView b, String title, String summary, final String cookie, boolean def) {
        return toggleRow(b.act, title, summary, "1".equals(b.prefGet(cookie, def ? "1" : "0")), new java.util.function.Consumer<Boolean>() {
            @Override public void accept(Boolean v) { b.prefSet(cookie, v ? "1" : "0"); }
        });
    }

    private interface Getter { String get(); }
    private interface Setter { void set(String v); }

    /** صف باختيار من قائمة (حوار)، الملخّص يعرض القيمة الحالية. */
    private static View choiceRow(final BrowserView b, final String title, final String[] values, final String[] names,
                                  final Getter get, final Setter set) {
        final View row = row(b.act, title, nameFor(values, names, get.get()), null, null);
        row.setClickable(true);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cur = get.get();
                int sel = 0;
                for (int i = 0; i < values.length; i++) if (values[i].equals(cur)) sel = i;
                b.dlg().setTitle(title).setSingleChoiceItems(names, sel, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        set.set(values[which]);
                        TextView s = summaryOf(row);
                        if (s != null) s.setText(names[which]);
                        d.dismiss();
                    }
                }).show();
            }
        });
        return row;
    }

    private static String nameFor(String[] values, String[] names, String cur) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(cur)) return names[i];
        return names.length > 0 ? names[0] : "";
    }

    private static View cookieChoice(final BrowserView b, String title, final String cookie, final String def, String[] values, String[] names) {
        return choiceRow(b, title, values, names, new Getter() {
            @Override public String get() { return b.prefGet(cookie, def); }
        }, new Setter() {
            @Override public void set(String v) { b.prefSet(cookie, v); }
        });
    }

    private static View storeChoice(final BrowserView b, String title, final String key, final String def, String[] values, String[] names, final Runnable after) {
        return choiceRow(b, title, values, names, new Getter() {
            @Override public String get() { return b.store.getStr(key, def); }
        }, new Setter() {
            @Override
            public void set(String v) {
                b.store.setStr(key, v);
                if (after != null) after.run();
            }
        });
    }

    private static Runnable open(final BrowserView b, final String which) {
        return new Runnable() {
            @Override
            public void run() {
                switch (which) {
                    case "engine": b.pushPage(searchEngine(b)); break;
                    case "engines": b.pushPage(engines(b)); break;
                    case "plugins": b.pushPage(plugins(b)); break;
                    case "address": b.pushPage(addressBar(b)); break;
                    case "privacy": b.pushPage(privacy(b)); break;
                    case "tabs": b.pushPage(tabsPage(b)); break;
                    case "homepage": b.pushPage(homepage(b)); break;
                    case "appearance": b.pushPage(appearance(b)); break;
                    case "access": b.pushPage(accessibility(b)); break;
                    case "site": b.pushPage(siteSettings(b)); break;
                    case "lang": b.pushPage(languages(b)); break;
                    case "about": b.pushPage(about(b)); break;
                    case "downloads": b.pushPage(downloadsSettings(b)); break;
                    case "default":
                        startSettings(b, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                        break;
                    case "notifications": {
                        try {
                            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                            i.putExtra(Settings.EXTRA_APP_PACKAGE, b.act.getPackageName());
                            b.act.startActivity(i);
                        } catch (Throwable t) {
                            startSettings(b, Settings.ACTION_SETTINGS);
                        }
                        break;
                    }
                    default: break;
                }
            }
        };
    }

    private static void startSettings(BrowserView b, String action) {
        try {
            b.act.startActivity(new Intent(action));
        } catch (Throwable t) {
            try { b.act.startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Throwable ignored) { }
        }
    }

    private static String onOff(boolean on) {
        return on ? Theme.s("On", "مفعّل") : Theme.s("Off", "متوقف");
    }

    private static String engineName(BrowserView b) {
        return "Seekr";
    }

    // =====================================================================
    //  الصفحة الرئيسية
    // =====================================================================
    static View main(final BrowserView b) {
        return new Page(b, Theme.s("Settings", "الإعدادات"), Theme.s("Search settings", "بحث في الإعدادات"), new Builder() {
            @Override
            public void build(Page p, LinearLayout list) {
                Context c = b.act;
                String q = p.query();
                if (!q.isEmpty()) {
                    int n = 0;
                    for (Object[] e : index(b)) {
                        String t = (String) e[0];
                        if (!t.toLowerCase().contains(q)) continue;
                        n++;
                        list.addView(row(c, t, (String) e[1], null, (Runnable) e[2]), rowLp());
                    }
                    if (n == 0) note(list, Theme.s("No matching settings", "لا توجد إعدادات مطابقة"));
                    return;
                }
                section(list, Theme.s("Basics", "أساسيات"));
                group(list,
                        row(c, Theme.s("Search engine", "محرك البحث"), engineName(b), null, open(b, "engine")),
                        row(c, Theme.s("Address bar", "شريط العنوان"), "bottom".equals(b.store.getStr("addr_pos", "top")) ? Theme.s("Bottom", "أسفل") : Theme.s("Top", "أعلى"), null, open(b, "address")),
                        row(c, Theme.s("Privacy and security", "الخصوصية والأمان"), null, null, open(b, "privacy")),
                        row(c, Theme.s("Default browser", "المتصفح التلقائي"), null, null, open(b, "default")));
                section(list, Theme.s("Advanced", "متقدّم"));
                group(list,
                        row(c, Theme.s("Tabs", "علامات التبويب"), null, null, open(b, "tabs")),
                        row(c, Theme.s("Homepage", "الصفحة الرئيسية"), onOff(b.store.getBool("homepage_on", true)), null, open(b, "homepage")),
                        row(c, Theme.s("Notifications", "الإشعارات"), null, null, open(b, "notifications")),
                        row(c, Theme.s("Appearance", "المظهر"), null, null, open(b, "appearance")),
                        row(c, Theme.s("Accessibility", "تسهيل الاستخدام"), null, null, open(b, "access")),
                        row(c, Theme.s("Site settings", "إعدادات المواقع"), null, null, open(b, "site")),
                        row(c, Theme.s("Languages", "اللغات"), null, null, open(b, "lang")),
                        row(c, Theme.s("Downloads", "التنزيلات"), null, null, open(b, "downloads")),
                        row(c, Theme.s("About Seekr", "حول Seekr"), null, null, open(b, "about")));
            }
        });
    }

    private static LinearLayout.LayoutParams rowLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.setMargins(Theme.dp(8), 0, Theme.dp(8), 0);
        return lp;
    }

    private static ArrayList<Object[]> index(BrowserView b) {
        ArrayList<Object[]> l = new ArrayList<>();
        String se = Theme.s("Search engine", "محرك البحث");
        l.add(new Object[]{Theme.s("Search engines", "محركات البحث"), se, open(b, "engines")});
        l.add(new Object[]{Theme.s("Plugins", "الإضافات"), se, open(b, "plugins")});
        l.add(new Object[]{Theme.s("Search language", "لغة البحث"), se, open(b, "engine")});
        l.add(new Object[]{Theme.s("SafeSearch", "البحث الآمن"), se, open(b, "engine")});
        l.add(new Object[]{Theme.s("Autocomplete", "الإكمال التلقائي"), se, open(b, "engine")});
        l.add(new Object[]{Theme.s("Default categories", "الفئات الافتراضية"), se, open(b, "engine")});
        l.add(new Object[]{Theme.s("Image proxy", "وكيل الصور"), se, open(b, "engine")});
        l.add(new Object[]{Theme.s("HTTP method", "طريقة الطلب"), se, open(b, "engine")});
        l.add(new Object[]{Theme.s("Favicon resolver", "مزوّد أيقونات المواقع"), se, open(b, "engine")});
        l.add(new Object[]{Theme.s("URL formatting", "تنسيق الروابط"), se, open(b, "engine")});
        l.add(new Object[]{Theme.s("Open results in new tab", "فتح النتائج في تبويب جديد"), se, open(b, "engine")});
        l.add(new Object[]{Theme.s("Address bar", "شريط العنوان"), "", open(b, "address")});
        l.add(new Object[]{Theme.s("Delete browsing data", "مسح بيانات التصفح"), Theme.s("Privacy and security", "الخصوصية والأمان"), open(b, "privacy")});
        l.add(new Object[]{Theme.s("Third-party cookies", "ملفات الارتباط للجهات الخارجية"), Theme.s("Privacy and security", "الخصوصية والأمان"), open(b, "privacy")});
        l.add(new Object[]{Theme.s("Safe Browsing", "التصفح الآمن"), Theme.s("Privacy and security", "الخصوصية والأمان"), open(b, "privacy")});
        l.add(new Object[]{Theme.s("Block pop-ups", "حظر النوافذ المنبثقة"), Theme.s("Site settings", "إعدادات المواقع"), open(b, "site")});
        l.add(new Object[]{Theme.s("Camera", "الكاميرا"), Theme.s("Site settings", "إعدادات المواقع"), open(b, "site")});
        l.add(new Object[]{Theme.s("Microphone", "الميكروفون"), Theme.s("Site settings", "إعدادات المواقع"), open(b, "site")});
        l.add(new Object[]{Theme.s("Location", "الموقع"), Theme.s("Site settings", "إعدادات المواقع"), open(b, "site")});
        l.add(new Object[]{"JavaScript", Theme.s("Site settings", "إعدادات المواقع"), open(b, "site")});
        l.add(new Object[]{Theme.s("Tabs", "علامات التبويب"), "", open(b, "tabs")});
        l.add(new Object[]{Theme.s("Homepage", "الصفحة الرئيسية"), "", open(b, "homepage")});
        l.add(new Object[]{Theme.s("Theme", "المظهر"), Theme.s("Appearance", "المظهر"), open(b, "appearance")});
        l.add(new Object[]{Theme.s("Text size", "حجم النص"), Theme.s("Accessibility", "تسهيل الاستخدام"), open(b, "access")});
        l.add(new Object[]{Theme.s("Desktop site", "نسخة سطح المكتب"), Theme.s("Accessibility", "تسهيل الاستخدام"), open(b, "access")});
        l.add(new Object[]{Theme.s("Interface language", "لغة الواجهة"), Theme.s("Languages", "اللغات"), open(b, "lang")});
        l.add(new Object[]{Theme.s("Downloads", "التنزيلات"), "", open(b, "downloads")});
        l.add(new Object[]{Theme.s("About Seekr", "حول Seekr"), "", open(b, "about")});
        return l;
    }

    // =====================================================================
    //  محرك البحث (كل إعدادات Seekr)
    // =====================================================================
    static View searchEngine(final BrowserView b) {
        return new Page(b, Theme.s("Search engine", "محرك البحث"), null, new Builder() {
            @Override
            public void build(Page p, LinearLayout list) {
                final Context c = b.act;
                section(list, Theme.s("Engines", "المحركات"));
                group(list,
                        row(c, Theme.s("Search engines", "محركات البحث"), Theme.s("Choose which engines Seekr queries", "اختر المحركات التي يستعلم منها Seekr"), null, open(b, "engines")),
                        categoriesRow(b),
                        row(c, Theme.s("Plugins", "الإضافات"), Theme.s("Calculator, tracker remover, unit converter…", "الآلة الحاسبة، إزالة المتتبعات، محوّل الوحدات…"), null, open(b, "plugins")));

                section(list, Theme.s("Results", "النتائج"));
                String[] sLang = {"auto", "all", "ar", "en", "fr", "de", "es", "it", "pt", "ru", "tr", "fa", "zh", "ja", "ko", "nl", "pl", "hi", "id"};
                String[] sLangN = {Theme.s("Detect automatically", "الاكتشاف التلقائي"), Theme.s("Any language", "أي لغة"), "العربية", "English", "Français", "Deutsch", "Español",
                        "Italiano", "Português", "Русский", "Türkçe", "فارسی", "中文", "日本語", "한국어", "Nederlands", "Polski", "हिन्दी", "Indonesia"};
                String[] acV = {"", "duckduckgo", "google", "brave", "startpage", "wikipedia", "qwant", "yandex", "swisscows"};
                String[] acN = {Theme.s("Off", "متوقف"), "DuckDuckGo", "Google", "Brave", "Startpage", "Wikipedia", "Qwant", "Yandex", "Swisscows"};
                group(list,
                        cookieChoice(b, Theme.s("Search language", "لغة البحث"), "language", "auto", sLang, sLangN),
                        cookieChoice(b, Theme.s("SafeSearch", "البحث الآمن"), "safesearch", "0", new String[]{"0", "1", "2"},
                                new String[]{Theme.s("Off", "متوقف"), Theme.s("Moderate", "معتدل"), Theme.s("Strict", "صارم")}),
                        cookieChoice(b, Theme.s("Autocomplete", "الإكمال التلقائي"), "autocomplete", "duckduckgo", acV, acN),
                        cookieChoice(b, Theme.s("URL formatting", "تنسيق الروابط"), "url_formatting", "pretty", new String[]{"pretty", "full", "host"},
                                new String[]{Theme.s("Pretty", "منسّق"), Theme.s("Full", "كامل"), Theme.s("Host only", "النطاق فقط")}),
                        cookieChoice(b, Theme.s("DOI resolver", "محلّل DOI"), "doi_resolver", "oadoi.org", new String[]{"", "oadoi.org", "doi.org", "sci-hub.se"},
                                new String[]{Theme.s("Off", "متوقف"), "oadoi.org", "doi.org", "sci-hub.se"}),
                        cookieToggle(b, Theme.s("Open results in new tab", "فتح النتائج في تبويب جديد"), null, "results_on_new_tab", false),
                        cookieToggle(b, Theme.s("Show query in page title", "إظهار الاستعلام في عنوان الصفحة"), null, "query_in_title", false),
                        cookieToggle(b, Theme.s("Search on category select", "البحث عند اختيار الفئة"), null, "search_on_category_select", true));

                section(list, Theme.s("Privacy", "الخصوصية"));
                group(list,
                        cookieToggle(b, Theme.s("Image proxy", "وكيل الصور"), Theme.s("Load result images through Seekr to hide your IP", "تحميل صور النتائج عبر Seekr لإخفاء عنوانك"), "image_proxy", true),
                        cookieChoice(b, Theme.s("Favicon resolver", "مزوّد أيقونات المواقع"), "favicon_resolver", "", new String[]{"", "duckduckgo", "google", "allesedv", "yandex"},
                                new String[]{Theme.s("Off", "متوقف"), "DuckDuckGo", "Google", "AllesEDV", "Yandex"}),
                        cookieChoice(b, Theme.s("HTTP method", "طريقة الطلب"), "method", "POST", new String[]{"POST", "GET"},
                                new String[]{"POST", "GET"}));

                section(list, Theme.s("Advanced", "متقدّم"));
                group(list,
                        cookieChoice(b, Theme.s("Hotkeys", "اختصارات لوحة المفاتيح"), "hotkeys", "default", new String[]{"default", "vim"},
                                new String[]{Theme.s("Default", "افتراضي"), "Vim"}),
                        tokensRow(b),
                        row(c, Theme.s("Reset search settings", "إعادة ضبط إعدادات البحث"), Theme.s("Restore Seekr defaults", "استعادة القيم الافتراضية لـ Seekr"), null, new Runnable() {
                            @Override
                            public void run() {
                                b.dlg().setTitle(Theme.s("Reset search settings?", "إعادة ضبط إعدادات البحث؟"))
                                        .setPositiveButton(Theme.s("Reset", "إعادة الضبط"), new android.content.DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(android.content.DialogInterface d, int w) {
                                                b.resetSearchPrefs();
                                                b.toast(Theme.s("Search settings reset", "تمت إعادة الضبط"));
                                                b.popPage();
                                                b.pushPage(searchEngine(b));
                                            }
                                        }).setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
                            }
                        }));
            }
        });
    }

    private static final String[] CAT_KEYS = {"general", "images", "videos", "news", "map", "music", "it", "science", "files", "social media"};

    private static View categoriesRow(final BrowserView b) {
        final String[] names = new String[CAT_KEYS.length];
        for (int i = 0; i < names.length; i++) names[i] = Pages.catLabel(CAT_KEYS[i]);
        final View row = row(b.act, Theme.s("Default categories", "الفئات الافتراضية"), catSummary(b, names), null, null);
        row.setClickable(true);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ArrayList<String> cur = Pages.csv(b.prefGet("categories", "general"));
                final boolean[] checked = new boolean[CAT_KEYS.length];
                for (int i = 0; i < CAT_KEYS.length; i++) checked[i] = cur.contains(CAT_KEYS[i]);
                b.dlg().setTitle(Theme.s("Default categories", "الفئات الافتراضية"))
                        .setMultiChoiceItems(names, checked, new android.content.DialogInterface.OnMultiChoiceClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int which, boolean is) { checked[which] = is; }
                        })
                        .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int w) {
                                ArrayList<String> out = new ArrayList<>();
                                for (int i = 0; i < CAT_KEYS.length; i++) if (checked[i]) out.add(CAT_KEYS[i]);
                                if (out.isEmpty()) out.add("general");
                                b.prefSet("categories", android.text.TextUtils.join(",", out));
                                TextView s = summaryOf(row);
                                if (s != null) s.setText(catSummary(b, names));
                            }
                        }).setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
            }
        });
        return row;
    }

    private static String catSummary(BrowserView b, String[] names) {
        ArrayList<String> cur = Pages.csv(b.prefGet("categories", "general"));
        ArrayList<String> out = new ArrayList<>();
        for (int i = 0; i < CAT_KEYS.length; i++) if (cur.contains(CAT_KEYS[i])) out.add(names[i]);
        return out.isEmpty() ? names[0] : android.text.TextUtils.join("، ", out);
    }

    private static View tokensRow(final BrowserView b) {
        String cur = b.prefGet("tokens", "");
        final View row = row(b.act, Theme.s("Access tokens", "رموز الوصول"), cur.isEmpty() ? Theme.s("Not set", "غير محدد") : "••••••", null, null);
        row.setClickable(true);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText in = new EditText(b.act);
                in.setText(b.prefGet("tokens", ""));
                in.setSingleLine(true);
                in.setHint(Theme.s("Comma-separated tokens", "رموز مفصولة بفواصل"));
                b.dlg().setTitle(Theme.s("Access tokens", "رموز الوصول")).setView(in)
                        .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int w) {
                                String v2 = in.getText().toString().trim();
                                b.prefSet("tokens", v2);
                                TextView s = summaryOf(row);
                                if (s != null) s.setText(v2.isEmpty() ? Theme.s("Not set", "غير محدد") : "••••••");
                            }
                        }).setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
            }
        });
        return row;
    }

    // =====================================================================
    //  صفحة المحركات
    // =====================================================================
    static View engines(final BrowserView b) {
        final Page page = new Page(b, Theme.s("Search engines", "محركات البحث"), Theme.s("Search engines", "بحث في المحركات"), new Builder() {
            @Override
            public void build(final Page p, LinearLayout list) {
                if (b.engineCache == null) {
                    boolean failed = b.engineError != null && !b.engineError.isEmpty();
                    TextView t = U.tv(b.act, failed ? Theme.s("Couldn't load engines. Tap to retry.", "تعذّر تحميل المحركات. اضغط لإعادة المحاولة.") + "\n\n" + b.engineError
                            : Theme.s("Loading…", "جارٍ التحميل…"), failed ? 13 : 16, Theme.dim);
                    t.setGravity(failed ? Gravity.START : Gravity.CENTER);
                    t.setPadding(Theme.dp(28), Theme.dp(60), Theme.dp(28), 0);
                    if (failed) {
                        t.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                b.engineError = "";
                                p.refresh();
                                b.loadEngines(new Runnable() { @Override public void run() { p.refresh(); } });
                            }
                        });
                    }
                    list.addView(t);
                    return;
                }
                String q = p.query();
                final ArrayList<String> en = Pages.csv(b.prefGet("enabled_engines", ""));
                final ArrayList<String> dis = Pages.csv(b.prefGet("disabled_engines", ""));
                String lastCat = null;
                ArrayList<View> rows = new ArrayList<>();
                for (final String[] e : b.engineCache) {
                    if (!q.isEmpty() && !e[1].toLowerCase().contains(q)) continue;
                    if (lastCat != null && !e[2].equals(lastCat)) {
                        group(list, rows.toArray(new View[0]));
                        rows.clear();
                    }
                    if (!e[2].equals(lastCat)) {
                        section(list, Pages.catLabel(e[2]));
                        lastCat = e[2];
                    }
                    final boolean def = "1".equals(e[3]);
                    boolean on = def;
                    if (en.contains(e[0])) on = true;
                    if (dis.contains(e[0])) on = false;
                    rows.add(toggleRow(b.act, e[1], null, on, new java.util.function.Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean now) {
                            ArrayList<String> en2 = Pages.csv(b.prefGet("enabled_engines", ""));
                            ArrayList<String> dis2 = Pages.csv(b.prefGet("disabled_engines", ""));
                            en2.remove(e[0]);
                            dis2.remove(e[0]);
                            if (now != def) { if (now) en2.add(e[0]); else dis2.add(e[0]); }
                            b.prefSet("enabled_engines", android.text.TextUtils.join(",", en2));
                            b.prefSet("disabled_engines", android.text.TextUtils.join(",", dis2));
                        }
                    }));
                }
                if (!rows.isEmpty()) group(list, rows.toArray(new View[0]));
            }
        });
        b.loadEngines(new Runnable() { @Override public void run() { page.refresh(); } });
        return page;
    }

    // =====================================================================
    //  الإضافات
    // =====================================================================
    static View plugins(final BrowserView b) {
        final String[] state = {"", ""};  // [0] json, [1] error
        final Page page = new Page(b, Theme.s("Plugins", "الإضافات"), null, new Builder() {
            @Override
            public void build(final Page p, LinearLayout list) {
                if (state[0].isEmpty()) {
                    boolean failed = !state[1].isEmpty();
                    TextView t = U.tv(b.act, failed ? Theme.s("Couldn't load plugins.", "تعذّر تحميل الإضافات.") + "\n\n" + state[1] : Theme.s("Loading…", "جارٍ التحميل…"),
                            failed ? 13 : 16, Theme.dim);
                    t.setGravity(failed ? Gravity.START : Gravity.CENTER);
                    t.setPadding(Theme.dp(28), Theme.dp(60), Theme.dp(28), 0);
                    list.addView(t);
                    return;
                }
                try {
                    JSONArray a = new JSONArray(state[0]);
                    ArrayList<String> en = Pages.csv(b.prefGet("enabled_plugins", ""));
                    ArrayList<String> dis = Pages.csv(b.prefGet("disabled_plugins", ""));
                    ArrayList<View> rows = new ArrayList<>();
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject o = a.getJSONObject(i);
                        final String id = o.getString("id");
                        final boolean def = o.getBoolean("on");
                        boolean on = def;
                        if (en.contains(id)) on = true;
                        if (dis.contains(id)) on = false;
                        rows.add(toggleRow(b.act, o.optString("name", id), o.optString("desc", ""), on, new java.util.function.Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean now) {
                                ArrayList<String> en2 = Pages.csv(b.prefGet("enabled_plugins", ""));
                                ArrayList<String> dis2 = Pages.csv(b.prefGet("disabled_plugins", ""));
                                en2.remove(id);
                                dis2.remove(id);
                                if (now != def) { if (now) en2.add(id); else dis2.add(id); }
                                b.prefSet("enabled_plugins", android.text.TextUtils.join(",", en2));
                                b.prefSet("disabled_plugins", android.text.TextUtils.join(",", dis2));
                            }
                        }));
                    }
                    if (!rows.isEmpty()) group(list, rows.toArray(new View[0]));
                } catch (Throwable t) {
                    note(list, String.valueOf(t));
                }
            }
        });
        b.api("/__api/plugins", new BrowserView.ApiCb() {
            @Override
            public void done(String body, String err) {
                if (body != null) state[0] = body; else state[1] = err == null ? "?" : err;
                page.refresh();
            }
        });
        return page;
    }

    // =====================================================================
    //  شريط العنوان
    // =====================================================================
    private static final class Radio extends View {
        private boolean on;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Radio(Context c, boolean on) {
            super(c);
            this.on = on;
            setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(26), Theme.dp(26)));
        }

        void set(boolean v) { on = v; invalidate(); }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), cx = w / 2f, r = w / 2f - Theme.dp(2);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Theme.dp(2));
            p.setColor(on ? Theme.accent : Theme.dim);
            c.drawCircle(cx, cx, r, p);
            if (on) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(Theme.accent);
                c.drawCircle(cx, cx, r - Theme.dp(5), p);
            }
        }
    }

    private static final class PhoneView extends View {
        boolean bottom;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        PhoneView(Context c) { super(c); }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float pw = Theme.dp(54), ph = Theme.dp(116);
            float l = (w - pw) / 2f, t = (h - ph) / 2f;
            p.setStyle(Paint.Style.FILL);
            p.setColor(Theme.accentBg & 0x40FFFFFF | 0x40000000);
            c.drawOval(new RectF(l - Theme.dp(26), t + Theme.dp(bottom ? 70 : -4), l + pw + Theme.dp(26), t + Theme.dp(bottom ? 144 : 70)), p);
            r.set(l, t, l + pw, t + ph);
            p.setColor(Theme.bg);
            c.drawRoundRect(r, Theme.dp(5), Theme.dp(5), p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Theme.dp(2));
            p.setColor(Theme.accent);
            c.drawRoundRect(r, Theme.dp(5), Theme.dp(5), p);
            p.setStyle(Paint.Style.FILL);
            float by = bottom ? t + ph - Theme.dp(14) : t + Theme.dp(4);
            r.set(l + Theme.dp(8), by, l + pw - Theme.dp(8), by + Theme.dp(8));
            c.drawRoundRect(r, Theme.dp(4), Theme.dp(4), p);
        }
    }

    static View addressBar(final BrowserView b) {
        return new Page(b, Theme.s("Address bar", "شريط العنوان"), null, new Builder() {
            @Override
            public void build(Page p, LinearLayout list) {
                final PhoneView pv = new PhoneView(b.act);
                pv.bottom = "bottom".equals(b.store.getStr("addr_pos", "top"));
                pv.setBackground(Theme.round(Theme.surface, 26));
                LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(MATCH, Theme.dp(190));
                pl.setMargins(Theme.dp(16), 0, Theme.dp(16), 0);
                list.addView(pv, pl);
                note(list, Theme.s("Choose where the address bar sits on the screen.", "اختر مكان شريط العنوان على الشاشة."));
                final Radio top = new Radio(b.act, !pv.bottom);
                final Radio bot = new Radio(b.act, pv.bottom);
                View rTop = row(b.act, Theme.s("Top", "أعلى"), null, top, new Runnable() {
                    @Override
                    public void run() {
                        b.store.setStr("addr_pos", "top");
                        top.set(true); bot.set(false); pv.bottom = false; pv.invalidate();
                        b.applyToolbarPrefs();
                    }
                });
                View rBot = row(b.act, Theme.s("Bottom", "أسفل"), null, bot, new Runnable() {
                    @Override
                    public void run() {
                        b.store.setStr("addr_pos", "bottom");
                        top.set(false); bot.set(true); pv.bottom = true; pv.invalidate();
                        b.applyToolbarPrefs();
                    }
                });
                group(list, rTop, rBot);
            }
        });
    }

    // =====================================================================
    //  الخصوصية والأمان
    // =====================================================================
    static View privacy(final BrowserView b) {
        return new Page(b, Theme.s("Privacy and security", "الخصوصية والأمان"), null, new Builder() {
            @Override
            public void build(Page p, LinearLayout list) {
                Context c = b.act;
                section(list, Theme.s("Privacy", "الخصوصية"));
                group(list,
                        row(c, Theme.s("Delete browsing data", "مسح بيانات التصفح"), Theme.s("Delete history, cookies, site data, cache…", "حذف السجل وملفات الارتباط وبيانات المواقع والذاكرة المؤقتة…"), null, new Runnable() {
                            @Override public void run() { b.showClearDialog(); }
                        }),
                        storeToggle(b, Theme.s("Third-party cookies", "ملفات الارتباط للجهات الخارجية"), Theme.s("Allow third-party cookies (applies to new tabs)", "السماح بملفات الارتباط للجهات الخارجية (تُطبَّق على التبويبات الجديدة)"), "third_party_cookies", true, null),
                        storeToggle(b, Theme.s("Block pop-ups", "حظر النوافذ المنبثقة"), Theme.s("Sites can't open new tabs without your tap", "لا تفتح المواقع تبويبات جديدة دون لمسة منك"), "block_popups", true, null));
                section(list, Theme.s("Security", "الأمان"));
                group(list,
                        storeToggle(b, Theme.s("Safe Browsing", "التصفح الآمن"), Theme.s("Warns you about dangerous sites (applies to new tabs)", "تحذيرك من المواقع الخطرة (يُطبَّق على التبويبات الجديدة)"), "safe_browsing", true, null));
            }
        });
    }

    // =====================================================================
    //  علامات التبويب والصفحة الرئيسية
    // =====================================================================
    static View tabsPage(final BrowserView b) {
        return new Page(b, Theme.s("Tabs", "علامات التبويب"), null, new Builder() {
            @Override
            public void build(Page p, LinearLayout list) {
                group(list,
                        storeToggle(b, Theme.s("Restore tabs on launch", "استعادة التبويبات عند التشغيل"), Theme.s("Reopen the tabs you had open last time", "إعادة فتح التبويبات التي كانت مفتوحة آخر مرة"), "restore_tabs", true, null),
                        row(b.act, Theme.s("Close all tabs", "إغلاق كل التبويبات"), null, null, new Runnable() {
                            @Override
                            public void run() {
                                b.dlg().setTitle(Theme.s("Close all tabs?", "إغلاق كل التبويبات؟"))
                                        .setPositiveButton(Theme.s("Close", "إغلاق"), new android.content.DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(android.content.DialogInterface d, int w) {
                                                b.closeAllTabs();
                                                b.toast(Theme.s("Tabs closed", "تم إغلاق التبويبات"));
                                            }
                                        }).setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
                            }
                        }));
            }
        });
    }

    static View homepage(final BrowserView b) {
        return new Page(b, Theme.s("Homepage", "الصفحة الرئيسية"), null, new Builder() {
            @Override
            public void build(final Page p, LinearLayout list) {
                boolean on = b.store.getBool("homepage_on", true);
                group(list, storeToggle(b, Theme.s("Show home button", "إظهار زر الرئيسية"), null, "homepage_on", true, new Runnable() {
                    @Override
                    public void run() {
                        b.applyToolbarPrefs();
                        p.refresh();
                    }
                }));
                if (!on) return;
                section(list, Theme.s("Home button opens", "زر الرئيسية يفتح"));
                final Radio rNtp = new Radio(b.act, b.store.getStr("homepage_url", "").isEmpty());
                final Radio rUrl = new Radio(b.act, !b.store.getStr("homepage_url", "").isEmpty());
                final String cur = b.store.getStr("homepage_url", "");
                final View rowUrl = row(b.act, Theme.s("Custom address", "عنوان مخصص"), cur.isEmpty() ? null : cur, rUrl, null);
                rowUrl.setClickable(true);
                rowUrl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final EditText in = new EditText(b.act);
                        in.setSingleLine(true);
                        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                        in.setText(b.store.getStr("homepage_url", ""));
                        in.setHint("https://");
                        b.dlg().setTitle(Theme.s("Custom address", "عنوان مخصص")).setView(in)
                                .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface d, int w) {
                                        String u = in.getText().toString().trim();
                                        if (!u.isEmpty() && !u.contains("://")) u = "https://" + u;
                                        b.store.setStr("homepage_url", u);
                                        p.refresh();
                                    }
                                }).setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
                    }
                });
                View rowNtp = row(b.act, Theme.s("New tab page", "صفحة علامة تبويب جديدة"), null, rNtp, new Runnable() {
                    @Override
                    public void run() {
                        b.store.setStr("homepage_url", "");
                        p.refresh();
                    }
                });
                group(list, rowNtp, rowUrl);
            }
        });
    }

    // =====================================================================
    //  المظهر وتسهيل الاستخدام والمواقع واللغات
    // =====================================================================
    static View appearance(final BrowserView b) {
        return new Page(b, Theme.s("Appearance", "المظهر"), null, new Builder() {
            @Override
            public void build(Page p, LinearLayout list) {
                group(list, storeChoice(b, Theme.s("Theme", "المظهر"), "theme", "system",
                        new String[]{"system", "dark", "light"},
                        new String[]{Theme.s("System default", "حسب الجهاز"), Theme.s("Dark", "داكن"), Theme.s("Light", "فاتح")},
                        new Runnable() {
                            @Override public void run() { b.toast(Theme.s("Restart the app to apply", "أعد تشغيل التطبيق لتطبيق التغيير")); }
                        }));
            }
        });
    }

    static View accessibility(final BrowserView b) {
        return new Page(b, Theme.s("Accessibility", "تسهيل الاستخدام"), null, new Builder() {
            @Override
            public void build(Page p, LinearLayout list) {
                group(list,
                        storeChoice(b, Theme.s("Text size", "حجم النص"), "zoom_s", String.valueOf(b.store.getInt("zoom", 100)),
                                new String[]{"85", "100", "115", "130", "150"}, new String[]{"85%", "100%", "115%", "130%", "150%"},
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        try { b.store.setInt("zoom", Integer.parseInt(b.store.getStr("zoom_s", "100"))); } catch (Throwable ignored) { }
                                        b.applyTextZoom();
                                    }
                                }),
                        storeToggle(b, Theme.s("Desktop site", "نسخة سطح المكتب"), Theme.s("Request the desktop version in new tabs", "طلب نسخة سطح المكتب في التبويبات الجديدة"), "desktop", false, null));
            }
        });
    }

    static View siteSettings(final BrowserView b) {
        return new Page(b, Theme.s("Site settings", "إعدادات المواقع"), null, new Builder() {
            @Override
            public void build(Page p, LinearLayout list) {
                String ask = Theme.s("Ask first", "اسأل أولًا");
                group(list,
                        storeToggle(b, Theme.s("Camera", "الكاميرا"), Theme.s("Sites can ask to use your camera", "يمكن للمواقع طلب استخدام الكاميرا"), "perm_camera", true, null),
                        storeToggle(b, Theme.s("Microphone", "الميكروفون"), Theme.s("Sites can ask to use your microphone", "يمكن للمواقع طلب استخدام الميكروفون"), "perm_mic", true, null),
                        storeToggle(b, Theme.s("Location", "الموقع"), Theme.s("Sites can ask for your location", "يمكن للمواقع طلب موقعك"), "perm_location", true, null),
                        storeToggle(b, "JavaScript", Theme.s("Allowed (applies to new tabs)", "مسموح (يُطبَّق على التبويبات الجديدة)"), "js", true, null),
                        storeToggle(b, Theme.s("Block pop-ups", "حظر النوافذ المنبثقة"), null, "block_popups", true, null));
            }
        });
    }

    static View languages(final BrowserView b) {
        return new Page(b, Theme.s("Languages", "اللغات"), null, new Builder() {
            @Override
            public void build(Page p, LinearLayout list) {
                section(list, Theme.s("Seekr language", "لغة Seekr"));
                group(list, storeChoice(b, Theme.s("Interface language", "لغة الواجهة"), "lang", "ar",
                        new String[]{"ar", "en", "auto"}, new String[]{"العربية", "English", Theme.s("Follow device", "حسب لغة الجهاز")},
                        new Runnable() {
                            @Override public void run() { b.toast(Theme.s("Restart the app to apply", "أعد تشغيل التطبيق لتطبيق التغيير")); }
                        }));
                section(list, Theme.s("Search", "البحث"));
                String[] sLang = {"auto", "all", "ar", "en", "fr", "de", "es", "it", "pt", "ru", "tr", "fa", "zh", "ja", "ko", "nl", "pl", "hi", "id"};
                String[] sLangN = {Theme.s("Detect automatically", "الاكتشاف التلقائي"), Theme.s("Any language", "أي لغة"), "العربية", "English", "Français", "Deutsch", "Español",
                        "Italiano", "Português", "Русский", "Türkçe", "فارسی", "中文", "日本語", "한국어", "Nederlands", "Polski", "हिन्दी", "Indonesia"};
                group(list, cookieChoice(b, Theme.s("Search language", "لغة البحث"), "language", "auto", sLang, sLangN));
            }
        });
    }

    static View downloadsSettings(final BrowserView b) {
        return new Page(b, Theme.s("Downloads", "التنزيلات"), null, new Builder() {
            @Override
            public void build(final Page p, LinearLayout list) {
                final String[] vals = {"Download", "Documents", "Pictures", "Movies", "Music", "DCIM"};
                final String[] names = {Theme.s("Downloads", "التنزيلات"), Theme.s("Documents", "المستندات"), Theme.s("Pictures", "الصور"),
                        Theme.s("Movies", "الفيديو"), Theme.s("Music", "الموسيقى"), Theme.s("Camera (DCIM)", "الكاميرا (DCIM)")};
                section(list, Theme.s("Location", "الموقع"));
                group(list,
                        row(b.act, Theme.s("Download location", "مسار التنزيل"), b.downloadPath(), null, new Runnable() {
                            @Override
                            public void run() {
                                String cur = b.store.getStr("dl_dir", "Download");
                                int sel = 0;
                                for (int i = 0; i < vals.length; i++) if (vals[i].equals(cur)) sel = i;
                                b.dlg().setTitle(Theme.s("Download location", "مسار التنزيل"))
                                        .setSingleChoiceItems(names, sel, new android.content.DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(android.content.DialogInterface d, int which) {
                                                b.store.setStr("dl_dir", vals[which]);
                                                d.dismiss();
                                                p.refresh();
                                            }
                                        }).show();
                            }
                        }),
                        row(b.act, Theme.s("Sub-folder", "مجلد فرعي"), b.store.getStr("dl_sub", "").isEmpty() ? Theme.s("None", "بلا") : b.store.getStr("dl_sub", ""), null, new Runnable() {
                            @Override
                            public void run() {
                                final EditText in = new EditText(b.act);
                                in.setSingleLine(true);
                                in.setText(b.store.getStr("dl_sub", ""));
                                in.setTextColor(Theme.text);
                                b.dlg().setTitle(Theme.s("Sub-folder", "مجلد فرعي")).setView(in)
                                        .setPositiveButton(Theme.s("Save", "حفظ"), new android.content.DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(android.content.DialogInterface d, int w) {
                                                String v = in.getText().toString().trim().replaceAll("[\\\\:*?\"<>|]", "").replaceAll("^/+|/+$", "");
                                                b.store.setStr("dl_sub", v);
                                                p.refresh();
                                            }
                                        })
                                        .setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
                            }
                        }));
                section(list, Theme.s("Downloaded files", "الملفات المنزّلة"));
                group(list, row(b.act, Theme.s("Show downloads", "عرض التنزيلات"), null, null, new Runnable() {
                    @Override public void run() { b.showDownloads(); }
                }));
            }
        });
    }

    static View about(final BrowserView b) {
        return new Page(b, Theme.s("About Seekr", "حول Seekr"), null, new Builder() {
            @Override
            public void build(Page p, LinearLayout list) {
                String ver = "";
                try { ver = b.act.getPackageManager().getPackageInfo(b.act.getPackageName(), 0).versionName; } catch (Throwable ignored) { }
                group(list,
                        row(b.act, Theme.s("Application version", "إصدار التطبيق"), "Seekr " + ver, null, null),
                        row(b.act, Theme.s("Operating system", "نظام التشغيل"), "Android " + android.os.Build.VERSION.RELEASE + "; " + android.os.Build.MODEL, null, null),
                        row(b.act, Theme.s("Legal information", "معلومات قانونية"), Theme.s("Built on SearXNG (AGPL-3.0)", "مبني على SearXNG (AGPL-3.0)"), null, new Runnable() {
                            @Override public void run() { b.showAbout(); }
                        }));
            }
        });
    }
}
