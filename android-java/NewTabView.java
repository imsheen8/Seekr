package __APP_ID__;

import android.graphics.Typeface;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** صفحة «علامة تبويب جديدة»: شعار Seekr، شريط بحث، اختصارات، وزر التصفّح المتخفّي. */
final class NewTabView extends ScrollView {
    private final BrowserView b;
    private final LinearLayout col;

    NewTabView(BrowserView b) {
        super(b.act);
        this.b = b;
        setBackgroundColor(Theme.bg);
        setFillViewport(true);
        col = new LinearLayout(b.act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(Theme.dp(16), Theme.dp(40), Theme.dp(16), Theme.dp(32));
        addView(col, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    void bind(Tab t) {
        col.removeAllViews();
        if (t != null && t.incognito) buildIncognito();
        else buildNormal();
    }

    private void buildIncognito() {
        col.addView(U.icon(b.act, Ico.INCOGNITO, 72, Theme.text));
        TextView h = U.tv(b.act, Theme.s("You've gone Incognito", "أنت الآن في وضع التصفّح المتخفّي"), 24, Theme.text);
        h.setGravity(Gravity.CENTER);
        h.setPadding(0, Theme.dp(20), 0, Theme.dp(12));
        col.addView(h);
        TextView p = U.tv(b.act, Theme.s(
                "Seekr won't save your browsing history, form data or page cache for pages you visit in Incognito tabs. Downloads and bookmarks are kept. Cookies are shared with normal tabs on Android WebView.",
                "لن يحفظ Seekr سجل تصفحك ولا بيانات النماذج ولا ذاكرة الصفحات في التبويبات المتخفّية. تبقى التنزيلات والإشارات المرجعية. ملفات تعريف الارتباط مشتركة مع التبويبات العادية بسبب قيود WebView في أندرويد."),
                14, Theme.dim);
        p.setGravity(Gravity.CENTER);
        p.setLineSpacing(0, 1.3f);
        col.addView(p);
        col.addView(searchPill());
    }

    private View searchPill() {
        LinearLayout pill = new LinearLayout(b.act);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackground(Theme.rippleBg(Theme.pill, 28));
        pill.setClickable(true);
        pill.setPadding(Theme.dp(18), 0, Theme.dp(6), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(56));
        lp.topMargin = Theme.dp(28);
        pill.setLayoutParams(lp);
        pill.addView(U.icon(b.act, Ico.SEARCH, 22, Theme.dim));
        TextView hint = U.tv(b.act, Theme.s("Search Seekr or type URL", "ابحث في Seekr أو اكتب عنوانًا"), 16, Theme.dim);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        hl.setMarginStart(Theme.dp(14));
        pill.addView(U.ellipsized(hint, 1), hl);
        pill.addView(U.ibtn(b.act, Ico.MIC, Theme.text, new View.OnClickListener() {
            @Override public void onClick(View v) { b.startVoice(); }
        }));
        pill.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { b.focusOmnibox(); }
        });
        return pill;
    }

    private void buildNormal() {
        // الشعار
        LinearLayout logo = new LinearLayout(b.act);
        logo.setOrientation(LinearLayout.HORIZONTAL);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        logo.addView(U.icon(b.act, Ico.SEARCH, 44, Theme.accent));
        SpannableString sp = new SpannableString("Seekr");
        sp.setSpan(new ForegroundColorSpan(Theme.accent), 4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView word = new TextView(b.act);
        word.setText(sp);
        word.setTextColor(Theme.text);
        word.setTypeface(Typeface.DEFAULT_BOLD);
        word.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        Theme.sp(word, 44);
        word.setPadding(Theme.dp(10), 0, 0, 0);
        logo.addView(word);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.bottomMargin = Theme.dp(4);
        col.addView(logo, ll);
        col.addView(searchPill());

        // زرّان: تصفّح متخفٍّ + الإشارات
        LinearLayout row = new LinearLayout(b.act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(48));
        rl.topMargin = Theme.dp(14);
        row.addView(pillButton(Ico.INCOGNITO, Theme.s("Incognito", "متخفٍّ"), new View.OnClickListener() {
            @Override public void onClick(View v) { b.openInNewTab(null, true); }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        View gap = new View(b.act);
        row.addView(gap, new LinearLayout.LayoutParams(Theme.dp(12), 1));
        row.addView(pillButton(Ico.STAR, Theme.s("Bookmarks", "الإشارات"), new View.OnClickListener() {
            @Override public void onClick(View v) { b.showBookmarks(); }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        col.addView(row, rl);

        // الاختصارات
        final List<Store.Entry> items = new ArrayList<>();
        final List<Boolean> pinned = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (Store.Entry e : b.store.shortcuts()) {
            items.add(e);
            pinned.add(Boolean.TRUE);
            String o = Store.originOf(e.url);
            if (o != null) seen.add(o);
        }
        for (Store.Entry e : b.store.topSites(8)) {
            if (items.size() >= 7) break;
            if (seen.contains(e.url)) continue;
            items.add(e);
            pinned.add(Boolean.FALSE);
        }
        LinearLayout box = new LinearLayout(b.act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Theme.round(Theme.surface, 24));
        box.setPadding(Theme.dp(8), Theme.dp(14), Theme.dp(8), Theme.dp(8));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bl.topMargin = Theme.dp(22);
        col.addView(box, bl);

        int total = items.size() + 1; // + زر الإضافة
        LinearLayout cur = null;
        for (int i = 0; i < total; i++) {
            if (i % 4 == 0) {
                cur = new LinearLayout(b.act);
                cur.setOrientation(LinearLayout.HORIZONTAL);
                box.addView(cur, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            if (i < items.size()) cur.addView(tile(items.get(i), pinned.get(i)));
            else cur.addView(addTile());
        }
        // إكمال الصف الأخير بخانات فارغة لتبقى الأعمدة متساوية
        if (cur != null) {
            int left = (4 - (total % 4)) % 4;
            for (int i = 0; i < left; i++) {
                View e = new View(b.act);
                cur.addView(e, new LinearLayout.LayoutParams(0, 1, 1f));
            }
        }
    }

    private View pillButton(int ico, String label, View.OnClickListener l) {
        LinearLayout p = new LinearLayout(b.act);
        p.setOrientation(LinearLayout.HORIZONTAL);
        p.setGravity(Gravity.CENTER);
        p.setBackground(Theme.rippleBg(Theme.pill, 24));
        p.setClickable(true);
        p.addView(U.icon(b.act, ico, 20, Theme.text));
        TextView t = U.tv(b.act, label, 15, Theme.text);
        t.setPadding(Theme.dp(8), 0, 0, 0);
        p.addView(t);
        p.setOnClickListener(l);
        return p;
    }

    private View tile(final Store.Entry e, final boolean pinned) {
        LinearLayout t = new LinearLayout(b.act);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(Theme.dp(2), Theme.dp(6), Theme.dp(2), Theme.dp(6));
        t.setBackground(Theme.rippleBg(0, 14));
        t.setClickable(true);
        View badge = U.siteBadge(b.act, b.store, e.url, e.title, 52);
        t.addView(badge);
        TextView label = U.ellipsized(U.tv(b.act, e.title == null || e.title.isEmpty() ? Store.hostOf(e.url) : e.title, 12, Theme.dim), 1);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Theme.dp(6);
        t.addView(label, lp);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { b.loadInCurrent(e.url); }
        });
        t.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String[] names = pinned
                        ? new String[]{Theme.s("Open in new tab", "فتح في تبويب جديد"), Theme.s("Remove", "إزالة")}
                        : new String[]{Theme.s("Open in new tab", "فتح في تبويب جديد"), Theme.s("Pin to shortcuts", "تثبيت في الاختصارات")};
                b.dlg().setTitle(e.title).setItems(names, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        if (which == 0) b.openInNewTab(e.url, false);
                        else if (pinned) { b.store.removeShortcut(e); bind(b.cur); }
                        else { b.store.addShortcut(e.title, e.url); bind(b.cur); }
                    }
                }).show();
                return true;
            }
        });
        return t;
    }

    private View addTile() {
        LinearLayout t = new LinearLayout(b.act);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(Theme.dp(2), Theme.dp(6), Theme.dp(2), Theme.dp(6));
        t.setBackground(Theme.rippleBg(0, 14));
        t.setClickable(true);
        ImageView plus = U.icon(b.act, Ico.PLUS, 52, Theme.text);
        int p = Theme.dp(14);
        plus.setPadding(p, p, p, p);
        plus.setBackground(Theme.round(Theme.pill, 26));
        t.addView(plus);
        TextView label = U.ellipsized(U.tv(b.act, Theme.s("Add shortcut", "إضافة اختصار"), 12, Theme.dim), 1);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Theme.dp(6);
        t.addView(label, lp);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showAddDialog(); }
        });
        return t;
    }

    private void showAddDialog() {
        LinearLayout box = new LinearLayout(b.act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Theme.dp(24), Theme.dp(8), Theme.dp(24), 0);
        final EditText name = new EditText(b.act);
        name.setHint(Theme.s("Name", "الاسم"));
        name.setSingleLine(true);
        final EditText url = new EditText(b.act);
        url.setHint("URL");
        url.setSingleLine(true);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        box.addView(name);
        box.addView(url);
        b.dlg().setTitle(Theme.s("Add shortcut", "إضافة اختصار")).setView(box)
                .setPositiveButton(Theme.s("Done", "تم"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        String u = b.resolve(url.getText().toString());
                        if (u == null || u.startsWith(b.base + "/search")) return;
                        String n = name.getText().toString().trim();
                        if (n.isEmpty()) n = Store.hostOf(u);
                        b.store.addShortcut(n, u);
                        bind(b.cur);
                    }
                })
                .setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
    }
}
