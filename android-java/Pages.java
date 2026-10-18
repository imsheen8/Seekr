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
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
        return BookmarksPages.root(b);
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
                PM pm = new PM(b.act, more);
                PM m = pm;
                if (ok) {
                    m.add(1, Theme.s("Open", "فتح"));
                    m.add(2, Theme.s("Share", "مشاركة"));
                }
                m.add(3, Theme.s("Delete", "حذف"));
                pm.setOnMenuItemClickListener(new PM.OnItem() {
                    @Override
                    public boolean onMenuItemClick(PM.Item it) {
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
    static String catLabel(String cat) {
        switch (cat) {
            case "general": return Theme.s("General", "عام");
            case "images": return Theme.s("Images", "صور");
            case "videos": return Theme.s("Videos", "فيديوهات");
            case "news": return Theme.s("News", "أخبار");
            case "map": return Theme.s("Maps", "خرائط");
            case "music": return Theme.s("Music", "موسيقى");
            case "it": return Theme.s("IT", "تقنية");
            case "science": return Theme.s("Science", "علوم");
            case "files": return Theme.s("Files", "ملفات");
            case "social media": return Theme.s("Social media", "التواصل الاجتماعي");
            default: return Theme.s("Other", "أخرى");
        }
    }

    static java.util.ArrayList<String> csv(String s) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (s == null) return out;
        for (String p : s.split(",")) if (!p.trim().isEmpty()) out.add(p.trim());
        return out;
    }

    static View tabSwitcher(final BrowserView b) {
        return new TabSwitcher(b);
    }
}
