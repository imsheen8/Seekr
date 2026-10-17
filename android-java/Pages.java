package __APP_ID__;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** صفحات كاملة الشاشة: مبدّل التبويبات، السجل، الإشارات، التنزيلات، الإعدادات. */
final class Pages {
    private Pages() {}

    // =====================================================================
    //  عناصر مشتركة
    // =====================================================================
    private static LinearLayout pageRoot(Context c) {
        LinearLayout p = new LinearLayout(c);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setBackgroundColor(Theme.bg);
        p.setClickable(true);
        return p;
    }

    private static LinearLayout header(final BrowserView b, String title, View... actions) {
        LinearLayout h = new LinearLayout(b.act);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(Theme.dp(20), Theme.dp(8), Theme.dp(8), Theme.dp(8));
        TextView t = U.tv(b.act, title, 26, Theme.text);
        t.setTypeface(Typeface.DEFAULT);
        h.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        for (View a : actions) h.addView(a);
        h.addView(U.ibtn(b.act, Ico.CLOSE, Theme.text, new View.OnClickListener() {
            @Override public void onClick(View v) { b.popPage(); }
        }));
        return h;
    }

    static String dayLabel(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        Calendar n = Calendar.getInstance();
        String d = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM,
                Theme.ar ? new Locale("ar") : Locale.getDefault()).format(new Date(ts));
        if (c.get(Calendar.YEAR) == n.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR))
            return Theme.s("Today - ", "اليوم - ") + d;
        n.add(Calendar.DAY_OF_YEAR, -1);
        if (c.get(Calendar.YEAR) == n.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR))
            return Theme.s("Yesterday - ", "أمس - ") + d;
        return d;
    }

    private static TextView dayHeading(Context c, String s) {
        TextView t = U.tv(c, s, 14, Theme.dim);
        t.setPadding(Theme.dp(20), Theme.dp(18), Theme.dp(20), Theme.dp(6));
        return t;
    }

    /** مفتاح تبديل بحجم 52×32. */
    static final class Toggle extends View {
        interface Listener { void onToggle(boolean on); }

        private boolean on;
        private Listener listener;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        Toggle(Context c, boolean initial, Listener l) {
            super(c);
            on = initial;
            listener = l;
            setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(52), Theme.dp(32)));
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggle();
                }
            });
        }

        void toggle() {
            on = !on;
            invalidate();
            if (listener != null) listener.onToggle(on);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            r.set(0, 0, w, h);
            p.setStyle(Paint.Style.FILL);
            p.setColor(on ? Theme.accentBg : Theme.pill);
            c.drawRoundRect(r, h / 2, h / 2, p);
            if (!on) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Theme.dp(2));
                p.setColor(Theme.dim);
                r.set(Theme.dp(1), Theme.dp(1), w - Theme.dp(1), h - Theme.dp(1));
                c.drawRoundRect(r, h / 2, h / 2, p);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(on ? Theme.accentOn : Theme.dim);
            float rad = on ? h * 0.34f : h * 0.25f;
            float cx = on ? w - h / 2f : h / 2f;
            c.drawCircle(cx, h / 2f, rad, p);
        }
    }

    // =====================================================================
    //  السجل والإشارات المرجعية
    // =====================================================================
    static View history(final BrowserView b) {
        return entriesPage(b, Theme.s("History", "السجل"), true);
    }

    static View bookmarks(final BrowserView b) {
        return entriesPage(b, Theme.s("Bookmarks", "الإشارات المرجعية"), false);
    }

    private static View entriesPage(final BrowserView b, String title, final boolean isHistory) {
        final LinearLayout page = pageRoot(b.act);
        final EditText search = new EditText(b.act);
        final LinearLayout list = new LinearLayout(b.act);
        list.setOrientation(LinearLayout.VERTICAL);

        final Runnable[] render = new Runnable[1];
        render[0] = new Runnable() {
            @Override
            public void run() {
                list.removeAllViews();
                String q = search.getText().toString().trim().toLowerCase();
                if (isHistory) {
                    TextView clear = U.tv(b.act, Theme.s("Delete browsing data…", "مسح بيانات التصفح…"), 16, Theme.accent);
                    clear.setPadding(Theme.dp(20), Theme.dp(14), Theme.dp(20), Theme.dp(14));
                    Theme.pressable(clear);
                    clear.setBackground(Theme.rippleBg(0, 0));
                    clear.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) { b.showClearDialog(); }
                    });
                    list.addView(clear);
                    list.addView(U.divider(b.act));
                }
                List<Store.Entry> src = isHistory ? b.store.history() : b.store.bookmarks();
                String lastDay = "";
                int shown = 0;
                for (final Store.Entry e : src) {
                    if (!q.isEmpty() && !e.title.toLowerCase().contains(q) && !e.url.toLowerCase().contains(q)) continue;
                    if (shown++ >= 400) break;
                    if (isHistory) {
                        String day = dayLabel(e.ts);
                        if (!day.equals(lastDay)) {
                            list.addView(dayHeading(b.act, day));
                            lastDay = day;
                        }
                    }
                    list.addView(entryRow(b, e, isHistory, render[0]));
                }
                if (shown == 0) {
                    TextView empty = U.tv(b.act, isHistory ? Theme.s("No history here", "لا يوجد سجل")
                            : Theme.s("No bookmarks yet", "لا توجد إشارات مرجعية بعد"), 16, Theme.dim);
                    empty.setGravity(Gravity.CENTER);
                    empty.setPadding(0, Theme.dp(60), 0, 0);
                    list.addView(empty);
                }
            }
        };

        ImageView searchBtn = U.ibtn(b.act, Ico.SEARCH, Theme.text, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (search.getVisibility() == View.VISIBLE) {
                    search.setText("");
                    search.setVisibility(View.GONE);
                    b.hideIme();
                } else {
                    search.setVisibility(View.VISIBLE);
                    search.requestFocus();
                    b.showIme(search);
                }
            }
        });
        page.addView(header(b, title, searchBtn));

        search.setHint(Theme.s("Search", "بحث"));
        search.setSingleLine(true);
        search.setTextColor(Theme.text);
        search.setHintTextColor(Theme.dim);
        search.setBackground(Theme.round(Theme.pill, 24));
        search.setPadding(Theme.dp(18), 0, Theme.dp(18), 0);
        search.setVisibility(View.GONE);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(48));
        sl.setMargins(Theme.dp(16), 0, Theme.dp(16), Theme.dp(8));
        page.addView(search, sl);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void afterTextChanged(Editable e) { render[0].run(); }
        });

        ScrollView sv = new ScrollView(b.act);
        sv.addView(list, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        render[0].run();
        return page;
    }

    private static View entryRow(final BrowserView b, final Store.Entry e, final boolean isHistory, final Runnable rerender) {
        LinearLayout row = new LinearLayout(b.act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Theme.dp(20), Theme.dp(8), Theme.dp(8), Theme.dp(8));
        Theme.pressable(row);
        row.setBackground(Theme.rippleBg(0, 0));
        row.addView(U.siteBadge(b.act, b.store, e.url, e.title, 40));
        LinearLayout col = new LinearLayout(b.act);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cl.setMarginStart(Theme.dp(16));
        cl.setMarginEnd(Theme.dp(8));
        row.addView(col, cl);
        String q = null;
        if (e.url.startsWith(b.base + "/search")) {
            try { q = Uri.parse(e.url).getQueryParameter("q"); } catch (Throwable ignored) { }
        }
        String shownTitle = q != null ? q : (e.title.isEmpty() ? e.url : e.title);
        col.addView(U.ellipsized(U.tv(b.act, shownTitle, 16, Theme.text), 1));
        col.addView(U.ellipsized(U.tv(b.act, q != null ? "Seekr" : Store.hostOf(e.url), 13, Theme.dim), 1));
        row.addView(U.ibtn(b.act, Ico.CLOSE, Theme.dim, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isHistory) b.store.deleteHistory(e); else b.store.removeBookmark(e);
                rerender.run();
            }
        }));
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { b.loadInCurrent(e.url); }
        });
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String[] names = {Theme.s("Open in new tab", "فتح في تبويب جديد"), Theme.s("Copy link address", "نسخ عنوان الرابط"), Theme.s("Share link", "مشاركة الرابط")};
                b.dlg().setItems(names, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        if (which == 0) b.openInNewTab(e.url, false);
                        else if (which == 1) b.copyText(e.url);
                        else b.share(e.url, e.title);
                    }
                }).show();
                return true;
            }
        });
        return row;
    }

    // =====================================================================
    //  التنزيلات
    // =====================================================================
    static final class Dl {
        int status = -1;
        long total, so_far;
        boolean exists;
    }

    private static Dl query(DownloadManager dm, long id) {
        Dl d = new Dl();
        Cursor c = null;
        try {
            c = dm.query(new DownloadManager.Query().setFilterById(id));
            if (c != null && c.moveToFirst()) {
                d.exists = true;
                d.status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                d.total = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                d.so_far = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) c.close();
        }
        return d;
    }

    static View downloads(final BrowserView b) {
        final LinearLayout page = pageRoot(b.act);
        final DownloadManager dm = (DownloadManager) b.act.getSystemService(Context.DOWNLOAD_SERVICE);
        final EditText search = new EditText(b.act);
        final LinearLayout list = new LinearLayout(b.act);
        list.setOrientation(LinearLayout.VERTICAL);
        final TextView usage = U.tv(b.act, "", 14, Theme.dim);
        usage.setPadding(Theme.dp(20), Theme.dp(4), Theme.dp(20), Theme.dp(12));

        final Runnable[] render = new Runnable[1];
        render[0] = new Runnable() {
            @Override
            public void run() {
                list.removeAllViews();
                String q = search.getText().toString().trim().toLowerCase();
                long used = 0;
                boolean active = false;
                String lastDay = "";
                int shown = 0;
                for (final Store.Entry e : b.store.downloads()) {
                    final Dl d = query(dm, e.id);
                    if (d.exists && d.status == DownloadManager.STATUS_SUCCESSFUL) used += d.total;
                    if (d.exists && (d.status == DownloadManager.STATUS_RUNNING || d.status == DownloadManager.STATUS_PENDING)) active = true;
                    if (!q.isEmpty() && !e.title.toLowerCase().contains(q)) continue;
                    String day = dayLabel(e.ts);
                    if (!day.equals(lastDay)) {
                        list.addView(dayHeading(b.act, day));
                        lastDay = day;
                    }
                    list.addView(downloadRow(b, dm, e, d, render[0]));
                    shown++;
                }
                if (shown == 0) {
                    TextView empty = U.tv(b.act, Theme.s("No downloads yet", "لا توجد تنزيلات بعد"), 16, Theme.dim);
                    empty.setGravity(Gravity.CENTER);
                    empty.setPadding(0, Theme.dp(60), 0, 0);
                    list.addView(empty);
                }
                long totalSpace = 0;
                try { totalSpace = new StatFs(Environment.getExternalStorageDirectory().getPath()).getTotalBytes(); } catch (Throwable ignored) { }
                usage.setText(Theme.s("Using ", "المستخدم ") + Formatter.formatFileSize(b.act, used)
                        + Theme.s(" of ", " من ") + Formatter.formatFileSize(b.act, totalSpace));
                if (active) {
                    page.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (page.isAttachedToWindow()) render[0].run();
                        }
                    }, 1000);
                }
            }
        };

        ImageView searchBtn = U.ibtn(b.act, Ico.SEARCH, Theme.text, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (search.getVisibility() == View.VISIBLE) {
                    search.setText("");
                    search.setVisibility(View.GONE);
                    b.hideIme();
                } else {
                    search.setVisibility(View.VISIBLE);
                    search.requestFocus();
                    b.showIme(search);
                }
            }
        });
        page.addView(header(b, Theme.s("Downloads", "التنزيلات"), searchBtn));
        page.addView(usage);
        page.addView(U.divider(b.act));

        search.setHint(Theme.s("Search downloads", "بحث في التنزيلات"));
        search.setSingleLine(true);
        search.setTextColor(Theme.text);
        search.setHintTextColor(Theme.dim);
        search.setBackground(Theme.round(Theme.pill, 24));
        search.setPadding(Theme.dp(18), 0, Theme.dp(18), 0);
        search.setVisibility(View.GONE);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(48));
        sl.setMargins(Theme.dp(16), Theme.dp(8), Theme.dp(16), Theme.dp(4));
        page.addView(search, sl);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void afterTextChanged(Editable e) { render[0].run(); }
        });

        ScrollView sv = new ScrollView(b.act);
        sv.addView(list, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        render[0].run();
        return page;
    }

    private static View downloadRow(final BrowserView b, final DownloadManager dm, final Store.Entry e, Dl d, final Runnable rerender) {
        LinearLayout row = new LinearLayout(b.act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Theme.dp(20), Theme.dp(10), Theme.dp(8), Theme.dp(10));
        Theme.pressable(row);
        row.setBackground(Theme.rippleBg(0, 0));
        ImageView ic = U.icon(b.act, Ico.DOWNLOAD, 40, Theme.text);
        int p = Theme.dp(9);
        ic.setPadding(p, p, p, p);
        ic.setBackground(Theme.round(Theme.pill, 20));
        row.addView(ic);
        LinearLayout col = new LinearLayout(b.act);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cl.setMarginStart(Theme.dp(16));
        cl.setMarginEnd(Theme.dp(4));
        row.addView(col, cl);
        col.addView(U.ellipsized(U.tv(b.act, e.title, 16, Theme.text), 1));
        String sub;
        final boolean ok = d.exists && d.status == DownloadManager.STATUS_SUCCESSFUL;
        if (!d.exists) sub = Theme.s("File removed", "تم حذف الملف");
        else if (d.status == DownloadManager.STATUS_FAILED) sub = Theme.s("Failed", "فشل التنزيل");
        else if (ok) sub = Formatter.formatFileSize(b.act, d.total) + " • " + Store.hostOf(e.url);
        else if (d.total > 0) sub = Theme.s("Downloading ", "جارٍ التنزيل ") + (int) (d.so_far * 100 / d.total) + "%";
        else sub = Theme.s("Downloading…", "جارٍ التنزيل…");
        col.addView(U.ellipsized(U.tv(b.act, sub, 13, Theme.dim), 1));
        final ImageView more = U.ibtn(b.act, Ico.MENU, Theme.text, null);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu pm = new PopupMenu(b.act, more);
                Menu m = pm.getMenu();
                if (ok) {
                    m.add(0, 1, 0, Theme.s("Open", "فتح"));
                    m.add(0, 2, 1, Theme.s("Share", "مشاركة"));
                }
                m.add(0, 3, 2, Theme.s("Delete", "حذف"));
                pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem it) {
                        if (it.getItemId() == 1) openDownload(b, dm, e);
                        else if (it.getItemId() == 2) shareDownload(b, dm, e);
                        else {
                            try { dm.remove(e.id); } catch (Throwable ignored) { }
                            b.store.removeDownload(e);
                            rerender.run();
                        }
                        return true;
                    }
                });
                pm.show();
            }
        });
        row.addView(more);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (ok) openDownload(b, dm, e); }
        });
        return row;
    }

    private static void openDownload(BrowserView b, DownloadManager dm, Store.Entry e) {
        try {
            Uri u = dm.getUriForDownloadedFile(e.id);
            String mime = dm.getMimeTypeForDownloadedFile(e.id);
            if (u == null) { b.toast(Theme.s("File not found", "الملف غير موجود")); return; }
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(u, mime == null ? "*/*" : mime);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            b.act.startActivity(i);
        } catch (Throwable t) {
            b.toast(Theme.s("No app can open this file", "لا يوجد تطبيق لفتح هذا الملف"));
        }
    }

    private static void shareDownload(BrowserView b, DownloadManager dm, Store.Entry e) {
        try {
            Uri u = dm.getUriForDownloadedFile(e.id);
            String mime = dm.getMimeTypeForDownloadedFile(e.id);
            if (u == null) return;
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(mime == null ? "*/*" : mime);
            i.putExtra(Intent.EXTRA_STREAM, u);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            b.act.startActivity(Intent.createChooser(i, null));
        } catch (Throwable ignored) { }
    }

    // =====================================================================
    //  الإعدادات
    // =====================================================================
    private static View sectionTitle(Context c, String s) {
        TextView t = U.tv(c, s, 13, Theme.accent);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(Theme.dp(20), Theme.dp(22), Theme.dp(20), Theme.dp(6));
        return t;
    }

    private static View settingRow(Context c, String title, String summary, View trailing, final Runnable click) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Theme.dp(20), Theme.dp(14), Theme.dp(20), Theme.dp(14));
        Theme.pressable(row);
        row.setBackground(Theme.rippleBg(0, 0));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        col.addView(U.tv(c, title, 16, Theme.text));
        if (summary != null) {
            TextView s = U.tv(c, summary, 13, Theme.dim);
            col.addView(s);
        }
        if (trailing != null) row.addView(trailing);
        if (click != null) {
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { click.run(); }
            });
        }
        return row;
    }

    static View settings(final BrowserView b) {
        LinearLayout page = pageRoot(b.act);
        page.addView(header(b, Theme.s("Settings", "الإعدادات")));
        LinearLayout list = new LinearLayout(b.act);
        list.setOrientation(LinearLayout.VERTICAL);
        final Context c = b.act;

        list.addView(sectionTitle(c, Theme.s("Search (Seekr)", "البحث (Seekr)")));
        list.addView(settingRow(c, Theme.s("Search engines", "محركات البحث"), Theme.s("Choose which engines Seekr queries", "اختر المحركات التي يستعلم منها Seekr"), null, new Runnable() {
            @Override public void run() { b.loadInCurrent(b.base + "/preferences#s-engines"); }
        }));
        list.addView(settingRow(c, Theme.s("Language, SafeSearch, autocomplete", "اللغة والبحث الآمن والإكمال التلقائي"), Theme.s("Search language and suggestions", "لغة البحث والاقتراحات"), null, new Runnable() {
            @Override public void run() { b.loadInCurrent(b.base + "/preferences#s-general"); }
        }));
        list.addView(settingRow(c, Theme.s("Search privacy", "خصوصية البحث"), Theme.s("HTTP method, image proxy", "طريقة HTTP ووكيل الصور"), null, new Runnable() {
            @Override public void run() { b.loadInCurrent(b.base + "/preferences#s-privacy"); }
        }));
        list.addView(settingRow(c, Theme.s("All search settings", "كل إعدادات البحث"), Theme.s("Cookies, special queries, reset", "ملفات الارتباط والاستعلامات الخاصة وإعادة الضبط"), null, new Runnable() {
            @Override public void run() { b.loadInCurrent(b.base + "/preferences"); }
        }));

        list.addView(sectionTitle(c, Theme.s("Appearance", "المظهر واللغة")));
        final String[] langs = {"العربية", "English", Theme.s("Follow device", "حسب لغة الجهاز")};
        final String[] langKeys = {"ar", "en", "auto"};
        String curLang = b.store.getStr("lang", "ar");
        int li = 0;
        for (int i = 0; i < langKeys.length; i++) if (langKeys[i].equals(curLang)) li = i;
        list.addView(settingRow(c, Theme.s("Language", "لغة الواجهة"), langs[li], null, new Runnable() {
            @Override
            public void run() {
                int sel = 0;
                String cl = b.store.getStr("lang", "ar");
                for (int i = 0; i < langKeys.length; i++) if (langKeys[i].equals(cl)) sel = i;
                b.dlg().setTitle(Theme.s("Language", "لغة الواجهة")).setSingleChoiceItems(langs, sel, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        b.store.setStr("lang", langKeys[which]);
                        d.dismiss();
                        b.toast(Theme.s("Restart the app to apply", "أعد تشغيل التطبيق لتطبيق التغيير"));
                    }
                }).show();
            }
        }));
        final String[] themes = {Theme.s("Follow device", "حسب الجهاز"), Theme.s("Dark", "داكن"), Theme.s("Light", "فاتح")};
        final String[] themeKeys = {"system", "dark", "light"};
        String curTheme = b.store.getStr("theme", "system");
        int ti = 0;
        for (int i = 0; i < themeKeys.length; i++) if (themeKeys[i].equals(curTheme)) ti = i;
        list.addView(settingRow(c, Theme.s("Theme", "المظهر"), themes[ti], null, new Runnable() {
            @Override
            public void run() {
                int sel = 0;
                String ct = b.store.getStr("theme", "system");
                for (int i = 0; i < themeKeys.length; i++) if (themeKeys[i].equals(ct)) sel = i;
                b.dlg().setTitle(Theme.s("Theme", "المظهر")).setSingleChoiceItems(themes, sel, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        b.store.setStr("theme", themeKeys[which]);
                        d.dismiss();
                        b.toast(Theme.s("Restart the app to apply", "أعد تشغيل التطبيق لتطبيق التغيير"));
                    }
                }).show();
            }
        }));

        list.addView(sectionTitle(c, Theme.s("Privacy", "الخصوصية")));
        list.addView(settingRow(c, Theme.s("Delete browsing data", "مسح بيانات التصفح"), Theme.s("History, cookies, cache and downloads list", "السجل وملفات الارتباط والذاكرة المؤقتة وقائمة التنزيلات"), null, new Runnable() {
            @Override public void run() { b.showClearDialog(); }
        }));
        final Toggle popups = new Toggle(c, b.store.getBool("block_popups", true), new Toggle.Listener() {
            @Override public void onToggle(boolean on) { b.store.setBool("block_popups", on); }
        });
        list.addView(settingRow(c, Theme.s("Block pop-ups", "حظر النوافذ المنبثقة"), Theme.s("Sites can't open new tabs without your tap", "لا تفتح المواقع تبويبات جديدة دون لمسة منك"), popups, new Runnable() {
            @Override public void run() { popups.toggle(); }
        }));

        list.addView(sectionTitle(c, Theme.s("Advanced", "متقدّم")));
        final int[] zooms = {85, 100, 115, 130, 150};
        list.addView(settingRow(c, Theme.s("Text size", "حجم النص"), b.store.getInt("zoom", 100) + "%", null, new Runnable() {
            @Override
            public void run() {
                String[] names = new String[zooms.length];
                int sel = 1;
                for (int i = 0; i < zooms.length; i++) {
                    names[i] = zooms[i] + "%";
                    if (zooms[i] == b.store.getInt("zoom", 100)) sel = i;
                }
                b.dlg().setTitle(Theme.s("Text size", "حجم النص")).setSingleChoiceItems(names, sel, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        b.store.setInt("zoom", zooms[which]);
                        b.applyTextZoom();
                        d.dismiss();
                    }
                }).show();
            }
        }));
        final Toggle desk = new Toggle(c, b.store.getBool("desktop", false), new Toggle.Listener() {
            @Override public void onToggle(boolean on) { b.store.setBool("desktop", on); }
        });
        list.addView(settingRow(c, Theme.s("Desktop site", "نسخة سطح المكتب"), Theme.s("Request the desktop version in new tabs", "طلب نسخة سطح المكتب في التبويبات الجديدة"), desk, new Runnable() {
            @Override public void run() { desk.toggle(); }
        }));
        list.addView(settingRow(c, Theme.s("Downloads", "التنزيلات"), Theme.s("Saved to your Downloads folder", "تُحفظ في مجلد التنزيلات"), null, new Runnable() {
            @Override public void run() { b.showDownloads(); }
        }));
        list.addView(settingRow(c, Theme.s("Bookmarks", "الإشارات المرجعية"), null, null, new Runnable() {
            @Override public void run() { b.showBookmarks(); }
        }));

        list.addView(sectionTitle(c, Theme.s("About", "حول")));
        list.addView(settingRow(c, Theme.s("About Seekr", "حول Seekr"), Theme.s("Built on SearXNG (AGPL-3.0)", "مبني على SearXNG (AGPL-3.0)"), null, new Runnable() {
            @Override public void run() { b.showAbout(); }
        }));

        ScrollView sv = new ScrollView(b.act);
        sv.addView(list, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    // =====================================================================
    //  مبدّل التبويبات
    // =====================================================================
    static View tabSwitcher(final BrowserView b) {
        return new Switcher(b);
    }

    static final class Switcher extends LinearLayout {
        private final BrowserView b;
        private boolean incog;
        private boolean listMode = false;
        private final LinearLayout grid;
        private final EditText search;
        private final FrameLayout segNormal, segIncog;
        private final U.CountView seg1Count;
        private final ImageView seg2Icon;

        Switcher(final BrowserView b) {
            super(b.act);
            this.b = b;
            this.incog = b.incognitoMode;
            setOrientation(VERTICAL);
            setBackgroundColor(Theme.bg);
            setClickable(true);

            // الصف العلوي: + | مفتاح عادي/متخفٍّ | ⋮
            LinearLayout top = new LinearLayout(b.act);
            top.setOrientation(HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.setPadding(Theme.dp(12), Theme.dp(10), Theme.dp(8), Theme.dp(6));

            FrameLayout plus = new FrameLayout(b.act);
            plus.setBackground(Theme.rippleBg(Theme.accentBg, 18));
            plus.setClickable(true);
            ImageView pi = U.icon(b.act, Ico.PLUS, 26, Theme.accentOn);
            plus.addView(pi, new FrameLayout.LayoutParams(Theme.dp(26), Theme.dp(26), Gravity.CENTER));
            plus.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { b.openInNewTab(null, incog); }
            });
            top.addView(plus, new LinearLayout.LayoutParams(Theme.dp(60), Theme.dp(50)));

            View sp1 = new View(b.act);
            top.addView(sp1, new LinearLayout.LayoutParams(0, 1, 1f));

            LinearLayout seg = new LinearLayout(b.act);
            seg.setOrientation(HORIZONTAL);
            seg.setBackground(Theme.round(Theme.surface, 26));
            seg.setPadding(Theme.dp(4), Theme.dp(4), Theme.dp(4), Theme.dp(4));
            segNormal = new FrameLayout(b.act);
            seg1Count = new U.CountView(b.act);
            segNormal.addView(seg1Count, new FrameLayout.LayoutParams(Theme.dp(44), Theme.dp(44), Gravity.CENTER));
            segNormal.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { incog = false; refresh(); }
            });
            seg.addView(segNormal, new LinearLayout.LayoutParams(Theme.dp(72), Theme.dp(44)));
            segIncog = new FrameLayout(b.act);
            seg2Icon = U.icon(b.act, Ico.INCOGNITO, 24, Theme.text);
            segIncog.addView(seg2Icon, new FrameLayout.LayoutParams(Theme.dp(24), Theme.dp(24), Gravity.CENTER));
            segIncog.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { incog = true; refresh(); }
            });
            seg.addView(segIncog, new LinearLayout.LayoutParams(Theme.dp(72), Theme.dp(44)));
            top.addView(seg);

            View sp2 = new View(b.act);
            top.addView(sp2, new LinearLayout.LayoutParams(0, 1, 1f));

            final ImageView more = U.ibtn(b.act, Ico.MENU, Theme.text, null);
            more.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showMore(more); }
            });
            top.addView(more);
            addView(top);

            // مربع البحث
            search = new EditText(b.act);
            search.setHint(Theme.s("Search your tabs", "ابحث في تبويباتك"));
            search.setSingleLine(true);
            search.setTextColor(Theme.text);
            search.setHintTextColor(Theme.dim);
            Theme.sp(search, 17);
            search.setBackground(Theme.round(Theme.surface, 28));
            search.setPadding(Theme.dp(24), 0, Theme.dp(24), 0);
            LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(54));
            sl.setMargins(Theme.dp(14), Theme.dp(6), Theme.dp(14), Theme.dp(10));
            addView(search, sl);
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
                @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
                @Override public void afterTextChanged(Editable e) { refresh(); }
            });

            ScrollView sv = new ScrollView(b.act);
            grid = new LinearLayout(b.act);
            grid.setOrientation(VERTICAL);
            grid.setPadding(Theme.dp(8), 0, Theme.dp(8), Theme.dp(24));
            sv.addView(grid, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            refresh();
        }

        private void showMore(View anchor) {
            PopupMenu pm = new PopupMenu(b.act, anchor);
            Menu m = pm.getMenu();
            m.add(0, 1, 0, incog ? Theme.s("Close all Incognito tabs", "إغلاق كل التبويبات المتخفّية") : Theme.s("Close all tabs", "إغلاق كل التبويبات"));
            m.add(0, 2, 1, listMode ? Theme.s("Grid view", "عرض شبكي") : Theme.s("List view", "عرض قائمة"));
            m.add(0, 3, 2, Theme.s("New tab", "تبويب جديد"));
            m.add(0, 4, 3, Theme.s("New Incognito tab", "تبويب متخفٍّ جديد"));
            m.add(0, 5, 4, Theme.s("Settings", "الإعدادات"));
            pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem it) {
                    switch (it.getItemId()) {
                        case 1: b.closeAll(incog); if (b.hasPages()) refresh(); break;
                        case 2: listMode = !listMode; refresh(); break;
                        case 3: b.openInNewTab(null, false); break;
                        case 4: b.openInNewTab(null, true); break;
                        default: b.showSettings(); break;
                    }
                    return true;
                }
            });
            pm.show();
        }

        void refresh() {
            // حالة المفتاح
            segNormal.setBackground(incog ? null : Theme.round(Theme.accentBg, 22));
            segIncog.setBackground(incog ? Theme.round(Theme.accentBg, 22) : null);
            seg1Count.setCount(b.countFor(false), incog ? Theme.text : Theme.accentOn);
            U.setIco(seg2Icon, Ico.INCOGNITO, incog ? Theme.accentOn : Theme.text);

            grid.removeAllViews();
            String q = search.getText().toString().trim().toLowerCase();
            int cols = listMode ? 1 : 2;
            LinearLayout row = null;
            int n = 0;
            for (Tab t : b.tabs) {
                if (t.incognito != incog) continue;
                if (!q.isEmpty() && !t.displayTitle().toLowerCase().contains(q) && !(t.url != null && t.url.toLowerCase().contains(q))) continue;
                if (n % cols == 0) {
                    row = new LinearLayout(b.act);
                    row.setOrientation(HORIZONTAL);
                    grid.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                row.addView(card(t));
                n++;
            }
            if (row != null && cols == 2 && n % 2 == 1) {
                View filler = new View(b.act);
                row.addView(filler, new LinearLayout.LayoutParams(0, 1, 1f));
            }
            if (n == 0) {
                TextView empty = U.tv(b.act, incog ? Theme.s("No Incognito tabs", "لا توجد تبويبات متخفّية") : Theme.s("No tabs", "لا توجد تبويبات"), 16, Theme.dim);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, Theme.dp(60), 0, 0);
                grid.addView(empty);
            }
        }

        private View card(final Tab t) {
            boolean sel = t == b.cur;
            int fg = sel ? Theme.accentOn : Theme.text;
            LinearLayout card = new LinearLayout(b.act);
            card.setOrientation(VERTICAL);
            card.setBackground(Theme.rippleBg(sel ? Theme.accentBg : Theme.surface, 26));
            card.setClickable(true);
            card.setPadding(Theme.dp(6), Theme.dp(4), Theme.dp(6), Theme.dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(Theme.dp(6), Theme.dp(6), Theme.dp(6), Theme.dp(6));
            card.setLayoutParams(lp);

            LinearLayout head = new LinearLayout(b.act);
            head.setOrientation(HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);
            View badge = t.ntp ? U.icon(b.act, t.incognito ? Ico.INCOGNITO : Ico.SEARCH, 22, fg)
                    : U.siteBadge(b.act, b.store, t.url, t.title, 24);
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(Theme.dp(24), Theme.dp(24));
            bl.setMarginStart(Theme.dp(8));
            head.addView(badge, bl);
            TextView title = U.ellipsized(U.tv(b.act, t.displayTitle(), 14, fg), 1);
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tl.setMarginStart(Theme.dp(8));
            head.addView(title, tl);
            ImageView x = U.ibtn(b.act, Ico.CLOSE, fg, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    b.closeTab(t);
                    if (b.hasPages()) refresh();
                }
            });
            x.setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(40), Theme.dp(40)));
            head.addView(x);
            card.addView(head);

            int h = listMode ? 130 : 210;
            FrameLayout thumbBox = new FrameLayout(b.act);
            thumbBox.setBackground(Theme.round(Theme.dark ? 0xFF120D0B : 0xFFFFFFFF, 18));
            thumbBox.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View v, Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), Theme.dp(18));
                }
            });
            thumbBox.setClipToOutline(true);
            if (t.thumb != null && !t.ntp) {
                final ImageView iv = new ImageView(b.act);
                iv.setImageBitmap(t.thumb);
                iv.setScaleType(ImageView.ScaleType.MATRIX);
                final Bitmap bmp = t.thumb;
                iv.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int l, int tp, int r, int bt, int ol, int ot, int or, int ob) {
                        int w = r - l;
                        if (w <= 0 || bmp.getWidth() <= 0) return;
                        float s = w / (float) bmp.getWidth();
                        Matrix mx = new Matrix();
                        mx.setScale(s, s);
                        iv.setImageMatrix(mx);
                    }
                });
                thumbBox.addView(iv, U.fill());
            } else {
                View big = t.ntp ? U.icon(b.act, t.incognito ? Ico.INCOGNITO : Ico.SEARCH, 44, Theme.accent)
                        : U.siteBadge(b.act, b.store, t.url, t.title, 52);
                thumbBox.setBackground(Theme.round(Theme.dark ? 0xFF120D0B : 0xFFF3E6E2, 18));
                thumbBox.addView(big, new FrameLayout.LayoutParams(big.getLayoutParams().width, big.getLayoutParams().height, Gravity.CENTER));
            }
            LinearLayout.LayoutParams thl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(h));
            thl.setMargins(Theme.dp(2), Theme.dp(4), Theme.dp(2), 0);
            card.addView(thumbBox, thl);

            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { b.switchTo(t); }
            });
            return card;
        }
    }
}
