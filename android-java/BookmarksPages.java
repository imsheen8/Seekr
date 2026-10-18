package __APP_ID__;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** الإشارات المرجعية بتصميم كروم: جذر («إشارات الجوال» + «قائمة القراءة»)، مجلدات، بحث، فرز، عرض مرئي/مضغوط. */
final class BookmarksPages {
    private BookmarksPages() {}

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    static View root(BrowserView b) { return new Pg(b, null); }

    static int tileColor() { return Theme.dark ? 0xFF6F3A1B : 0xFFFFDBD1; }

    static int tileFg() { return Theme.dark ? 0xFFFFDBD1 : 0xFF3B0A00; }

    /** رسم توضيحي للحالة الفارغة (دائرة بنية + أيقونة). */
    static final class Illus extends View {
        private final int kind;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Ico ico;

        Illus(Context c, int kind) {
            super(c);
            this.kind = kind;
            ico = new Ico(kind, Theme.dark ? 0xFFE9A07B : 0xFFB3452B);
            setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(170), Theme.dp(150)));
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            p.setColor(tileColor());
            c.drawCircle(w * 0.42f, h * 0.45f, Theme.dp(52), p);
            RectF r = new RectF(w * 0.42f, h * 0.38f, w * 0.42f + Theme.dp(96), h * 0.38f + Theme.dp(80));
            c.drawRoundRect(r, Theme.dp(30), Theme.dp(30), p);
            int s = Theme.dp(64);
            ico.setBounds((int) (w / 2 - s / 2), (int) (h / 2 - s / 2), (int) (w / 2 + s / 2), (int) (h / 2 + s / 2));
            ico.draw(c);
        }
    }

    static View emptyState(Context c, int iconKind, String title, String body) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Theme.dp(32), Theme.dp(90), Theme.dp(32), 0);
        box.addView(new Illus(c, iconKind));
        TextView t = U.tv(c, title, 22, Theme.text);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Theme.dp(24), 0, Theme.dp(8));
        box.addView(t);
        TextView d = U.tv(c, body, 16, Theme.dim);
        d.setGravity(Gravity.CENTER);
        box.addView(d);
        return box;
    }

    static final class Pg extends LinearLayout implements SettingsPages.Refreshable {
        final BrowserView b;
        final String folder;       // null = الجذر
        final LinearLayout list;
        final EditText search;

        Pg(final BrowserView b, final String folder) {
            super(b.act);
            this.b = b;
            this.folder = folder;
            setOrientation(VERTICAL);
            setBackgroundColor(Theme.bg);
            setClickable(true);

            LinearLayout h = new LinearLayout(b.act);
            h.setOrientation(HORIZONTAL);
            h.setGravity(Gravity.CENTER_VERTICAL);
            h.setPadding(Theme.dp(folder == null ? 20 : 8), Theme.dp(8), Theme.dp(8), Theme.dp(8));
            if (folder != null) {
                ImageView back = U.ibtn(b.act, Ico.BACK, Theme.text, new View.OnClickListener() {
                    @Override public void onClick(View v) { b.popPage(); }
                });
                if (Theme.ar) back.setScaleX(-1f);
                h.addView(back);
            }
            TextView t = U.ellipsized(U.tv(b.act, titleOf(folder), 24, Theme.text), 1);
            t.setPadding(Theme.dp(folder == null ? 0 : 12), 0, 0, 0);
            h.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));
            h.addView(U.ibtn(b.act, Ico.SORT, Theme.text, new View.OnClickListener() {
                @Override public void onClick(View v) { showSort(v); }
            }));
            boolean canNew = folder == null || folder.isEmpty();
            ImageView nf = U.ibtn(b.act, Ico.FOLDER_PLUS, canNew ? Theme.text : Theme.divider, canNew ? new View.OnClickListener() {
                @Override public void onClick(View v) { newFolderDialog(); }
            } : null);
            h.addView(nf);
            h.addView(U.ibtn(b.act, Ico.CLOSE, Theme.text, new View.OnClickListener() {
                @Override public void onClick(View v) { b.closePages(); }
            }));
            addView(h);

            LinearLayout box = new LinearLayout(b.act);
            box.setOrientation(HORIZONTAL);
            box.setGravity(Gravity.CENTER_VERTICAL);
            box.setBackground(Theme.round(Theme.surface, 26));
            search = new EditText(b.act);
            search.setBackground(null);
            search.setSingleLine(true);
            search.setHint(Theme.s("Search your bookmarks", "ابحث في إشاراتك المرجعية"));
            search.setTextColor(Theme.text);
            search.setHintTextColor(Theme.dim);
            search.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            Theme.sp(search, 17);
            search.setPadding(Theme.dp(20), 0, Theme.dp(20), 0);
            box.addView(search, new LinearLayout.LayoutParams(0, MATCH, 1f));
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(MATCH, Theme.dp(50));
            bl.setMargins(Theme.dp(12), Theme.dp(4), Theme.dp(12), Theme.dp(8));
            addView(box, bl);
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
                @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
                @Override public void afterTextChanged(Editable e) { refresh(); }
            });

            ScrollView sv = new ScrollView(b.act);
            sv.setFillViewport(true);
            list = new LinearLayout(b.act);
            list.setOrientation(VERTICAL);
            list.setPadding(0, 0, 0, Theme.dp(24));
            sv.addView(list, new FrameLayout.LayoutParams(MATCH, WRAP));
            addView(sv, new LinearLayout.LayoutParams(MATCH, 0, 1f));
            refresh();
        }

        private String titleOf(String f) {
            if (f == null) return Theme.s("Bookmarks", "الإشارات المرجعية");
            if (f.isEmpty()) return Theme.s("Mobile bookmarks", "إشارات الجوال");
            if (Store.READING.equals(f)) return Theme.s("Reading list", "قائمة القراءة");
            return f;
        }

        // ---------- الفرز والعرض ----------
        private void showSort(View anchor) {
            PopupMenu pm = new PopupMenu(b.act, anchor);
            Menu m = pm.getMenu();
            String cur = b.store.getStr("bm_sort", "manual");
            String[] keys = {"manual", "newest", "oldest", "opened", "az", "za"};
            String[] names = {Theme.s("Sort by manual order", "الترتيب اليدوي"), Theme.s("Sort by newest", "الأحدث أولًا"), Theme.s("Sort by oldest", "الأقدم أولًا"),
                    Theme.s("Sort by last opened", "آخر ما فُتح"), Theme.s("Sort by A to Z", "من أ إلى ي"), Theme.s("Sort by Z to A", "من ي إلى أ")};
            for (int i = 0; i < keys.length; i++) m.add(1, 10 + i, i, names[i]).setCheckable(true).setChecked(keys[i].equals(cur));
            m.setGroupCheckable(1, true, true);
            boolean visual = b.store.getBool("bm_visual", true);
            m.add(2, 30, 20, Theme.s("Visual view", "عرض مرئي")).setCheckable(true).setChecked(visual);
            m.add(2, 31, 21, Theme.s("Compact view", "عرض مضغوط")).setCheckable(true).setChecked(!visual);
            m.setGroupCheckable(2, true, true);
            final String[] fk = keys;
            pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem it) {
                    int id = it.getItemId();
                    if (id >= 10 && id < 20) b.store.setStr("bm_sort", fk[id - 10]);
                    else if (id == 30) b.store.setBool("bm_visual", true);
                    else if (id == 31) b.store.setBool("bm_visual", false);
                    refresh();
                    return true;
                }
            });
            pm.show();
        }

        private List<Store.Entry> sorted(List<Store.Entry> in) {
            List<Store.Entry> l = new ArrayList<>(in);
            String s = b.store.getStr("bm_sort", "manual");
            Comparator<Store.Entry> c = null;
            switch (s) {
                case "newest": c = new Comparator<Store.Entry>() { @Override public int compare(Store.Entry x, Store.Entry y) { return Long.compare(y.ts, x.ts); } }; break;
                case "oldest": c = new Comparator<Store.Entry>() { @Override public int compare(Store.Entry x, Store.Entry y) { return Long.compare(x.ts, y.ts); } }; break;
                case "opened": c = new Comparator<Store.Entry>() { @Override public int compare(Store.Entry x, Store.Entry y) { return Long.compare(y.opened, x.opened); } }; break;
                case "az": c = new Comparator<Store.Entry>() { @Override public int compare(Store.Entry x, Store.Entry y) { return x.title.compareToIgnoreCase(y.title); } }; break;
                case "za": c = new Comparator<Store.Entry>() { @Override public int compare(Store.Entry x, Store.Entry y) { return y.title.compareToIgnoreCase(x.title); } }; break;
                default: break;
            }
            if (c != null) Collections.sort(l, c);
            return l;
        }

        // ---------- البناء ----------
        @Override
        public void refresh() {
            list.removeAllViews();
            String q = search.getText().toString().trim().toLowerCase();
            if (!q.isEmpty()) {
                List<Store.Entry> all = new ArrayList<>();
                for (Store.Entry e : b.store.bookmarksIn("")) all.add(e);
                for (String f : b.store.folders()) all.addAll(b.store.bookmarksIn(f));
                all.addAll(b.store.bookmarksIn(Store.READING));
                int n = 0;
                for (Store.Entry e : sorted(all)) {
                    if (!e.title.toLowerCase().contains(q) && !e.url.toLowerCase().contains(q)) continue;
                    list.addView(bookmarkRow(e));
                    n++;
                }
                if (n == 0) {
                    TextView t = U.tv(b.act, Theme.s("No matching bookmarks", "لا توجد إشارات مطابقة"), 16, Theme.dim);
                    t.setGravity(Gravity.CENTER);
                    t.setPadding(0, Theme.dp(60), 0, 0);
                    list.addView(t);
                }
                return;
            }
            if (folder == null) {
                int mobile = b.store.bookmarksIn("").size();
                for (String f : b.store.folders()) mobile += 1;
                list.addView(tileRow(titleOf(""), mobile, Ico.FOLDER, new Runnable() {
                    @Override public void run() { b.pushPage(new Pg(b, "")); }
                }, null));
                list.addView(tileRow(titleOf(Store.READING), b.store.bookmarksIn(Store.READING).size(), Ico.READING, new Runnable() {
                    @Override public void run() { b.pushPage(new Pg(b, Store.READING)); }
                }, null));
                return;
            }
            int shown = 0;
            if (folder.isEmpty()) {
                for (final String f : b.store.folders()) {
                    list.addView(tileRow(f, b.store.bookmarksIn(f).size(), Ico.FOLDER, new Runnable() {
                        @Override public void run() { b.pushPage(new Pg(b, f)); }
                    }, f));
                    shown++;
                }
            }
            for (Store.Entry e : sorted(b.store.bookmarksIn(folder))) {
                list.addView(bookmarkRow(e));
                shown++;
            }
            if (shown == 0) {
                if (Store.READING.equals(folder)) {
                    list.addView(emptyState(b.act, Ico.READING, Theme.s("You'll find your reading list here", "ستجد قائمة القراءة هنا"),
                            Theme.s("You can save pages to read later", "يمكنك حفظ الصفحات لقراءتها لاحقًا")));
                } else {
                    list.addView(emptyState(b.act, Ico.STAR, Theme.s("You'll find your bookmarks here", "ستجد إشاراتك المرجعية هنا"),
                            Theme.s("You can get back to a page that's important to you by adding a bookmark", "يمكنك العودة إلى صفحة مهمة لك بإضافة إشارة مرجعية")));
                }
            }
        }

        private View tileRow(String title, int count, int icon, final Runnable click, final String folderName) {
            LinearLayout row = new LinearLayout(b.act);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Theme.dp(12), Theme.dp(6), Theme.dp(4), Theme.dp(6));
            Theme.pressable(row);
            row.setBackground(Theme.rippleBg(0, 0));
            FrameLayout tile = new FrameLayout(b.act);
            tile.setBackground(Theme.round(tileColor(), 26));
            ImageView ic = U.icon(b.act, icon, 26, tileFg());
            FrameLayout.LayoutParams il = new FrameLayout.LayoutParams(Theme.dp(26), Theme.dp(26), Gravity.TOP | Gravity.START);
            il.setMargins(Theme.dp(14), Theme.dp(14), 0, 0);
            tile.addView(ic, il);
            TextView cnt = U.tv(b.act, String.valueOf(count), 20, tileFg());
            FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM | Gravity.END);
            cl.setMargins(0, 0, Theme.dp(16), Theme.dp(10));
            tile.addView(cnt, cl);
            row.addView(tile, new LinearLayout.LayoutParams(Theme.dp(92), Theme.dp(92)));
            TextView t = U.ellipsized(U.tv(b.act, title, 18, Theme.text), 2);
            t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, WRAP, 1f);
            tl.setMarginStart(Theme.dp(20));
            row.addView(t, tl);
            if (folderName != null) {
                final ImageView more = U.ibtn(b.act, Ico.MENU, Theme.dim, null);
                more.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { folderMenu(more, folderName); }
                });
                row.addView(more);
            }
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { click.run(); }
            });
            return row;
        }

        private View bookmarkRow(final Store.Entry e) {
            boolean visual = b.store.getBool("bm_visual", true);
            LinearLayout row = new LinearLayout(b.act);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Theme.dp(12), Theme.dp(visual ? 6 : 2), Theme.dp(4), Theme.dp(visual ? 6 : 2));
            Theme.pressable(row);
            row.setBackground(Theme.rippleBg(0, 0));
            if (visual) {
                FrameLayout tile = new FrameLayout(b.act);
                tile.setBackground(Theme.round(Theme.surface, 26));
                View badge = U.siteBadge(b.act, b.store, e.url, e.title, 48);
                tile.addView(badge, new FrameLayout.LayoutParams(Theme.dp(48), Theme.dp(48), Gravity.CENTER));
                row.addView(tile, new LinearLayout.LayoutParams(Theme.dp(92), Theme.dp(92)));
            } else {
                row.addView(U.siteBadge(b.act, b.store, e.url, e.title, 36), new LinearLayout.LayoutParams(Theme.dp(36), Theme.dp(36)));
            }
            LinearLayout col = new LinearLayout(b.act);
            col.setOrientation(VERTICAL);
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, WRAP, 1f);
            cl.setMarginStart(Theme.dp(visual ? 20 : 16));
            row.addView(col, cl);
            TextView t = U.ellipsized(U.tv(b.act, e.title.isEmpty() ? e.url : e.title, visual ? 17 : 16, Theme.text), visual ? 2 : 1);
            t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            col.addView(t);
            TextView d = U.ellipsized(U.tv(b.act, Store.hostOf(e.url), 14, Theme.dim), 1);
            d.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            col.addView(d);
            final ImageView more = U.ibtn(b.act, Ico.MENU, Theme.dim, null);
            more.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { entryMenu(more, e); }
            });
            row.addView(more);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    b.store.touchBookmark(e);
                    b.loadInCurrent(e.url);
                }
            });
            return row;
        }

        // ---------- القوائم والحوارات ----------
        private void entryMenu(View anchor, final Store.Entry e) {
            PopupMenu pm = new PopupMenu(b.act, anchor);
            Menu m = pm.getMenu();
            m.add(0, 1, 0, Theme.s("Open in new tab", "فتح في تبويب جديد"));
            m.add(0, 2, 1, Theme.s("Open in new Incognito tab", "فتح في تبويب متخفٍّ جديد"));
            m.add(0, 3, 2, Theme.s("Edit", "تعديل"));
            m.add(0, 4, 3, Theme.s("Move to…", "نقل إلى…"));
            m.add(0, 5, 4, Theme.s("Copy link address", "نسخ عنوان الرابط"));
            m.add(0, 6, 5, Theme.s("Delete", "حذف"));
            pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem it) {
                    switch (it.getItemId()) {
                        case 1: b.store.touchBookmark(e); b.openInNewTab(e.url, false); break;
                        case 2: b.store.touchBookmark(e); b.openInNewTab(e.url, true); break;
                        case 3: editDialog(e); break;
                        case 4: moveDialog(e); break;
                        case 5: b.copyText(e.url); break;
                        default: b.store.removeBookmark(e); refresh(); break;
                    }
                    return true;
                }
            });
            pm.show();
        }

        private void folderMenu(View anchor, final String name) {
            PopupMenu pm = new PopupMenu(b.act, anchor);
            Menu m = pm.getMenu();
            m.add(0, 1, 0, Theme.s("Rename", "إعادة تسمية"));
            m.add(0, 2, 1, Theme.s("Delete", "حذف"));
            pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem it) {
                    if (it.getItemId() == 1) renameDialog(name);
                    else {
                        b.dlg().setTitle(Theme.s("Delete folder?", "حذف المجلد؟"))
                                .setMessage(Theme.s("Bookmarks inside it will be deleted too.", "ستُحذف الإشارات الموجودة بداخله أيضًا."))
                                .setPositiveButton(Theme.s("Delete", "حذف"), new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface d, int w) {
                                        b.store.deleteFolder(name);
                                        refresh();
                                    }
                                }).setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
                    }
                    return true;
                }
            });
            pm.show();
        }

        private EditText field(String hint, String value) {
            EditText in = new EditText(b.act);
            in.setSingleLine(true);
            in.setHint(hint);
            in.setText(value);
            return in;
        }

        private void newFolderDialog() {
            final EditText in = field(Theme.s("Title", "العنوان"), "");
            b.dlg().setTitle(Theme.s("Create new folder", "إنشاء مجلد جديد")).setView(in)
                    .setPositiveButton(Theme.s("Add", "إضافة"), new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            if (!b.store.addFolder(in.getText().toString())) b.toast(Theme.s("Name is empty or already used", "الاسم فارغ أو مستخدم"));
                            refresh();
                        }
                    }).setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
        }

        private void renameDialog(final String old) {
            final EditText in = field(Theme.s("Title", "العنوان"), old);
            b.dlg().setTitle(Theme.s("Rename", "إعادة تسمية")).setView(in)
                    .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            if (!b.store.renameFolder(old, in.getText().toString())) b.toast(Theme.s("Name is empty or already used", "الاسم فارغ أو مستخدم"));
                            refresh();
                        }
                    }).setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
        }

        private void editDialog(final Store.Entry e) {
            LinearLayout box = new LinearLayout(b.act);
            box.setOrientation(VERTICAL);
            box.setPadding(Theme.dp(24), Theme.dp(8), Theme.dp(24), 0);
            final EditText t = field(Theme.s("Name", "الاسم"), e.title);
            final EditText u = field("URL", e.url);
            box.addView(t);
            box.addView(u);
            b.dlg().setTitle(Theme.s("Edit bookmark", "تعديل الإشارة")).setView(box)
                    .setPositiveButton(Theme.s("Save", "حفظ"), new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            String nu = u.getText().toString().trim();
                            if (!nu.isEmpty()) e.url = nu;
                            e.title = t.getText().toString().trim();
                            b.store.saveBookmarks();
                            refresh();
                        }
                    }).setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
        }

        private void moveDialog(final Store.Entry e) {
            final List<String> folders = b.store.folders();
            final String[] names = new String[folders.size() + 1];
            final String[] ids = new String[folders.size() + 1];
            names[0] = titleOf("");
            ids[0] = "";
            for (int i = 0; i < folders.size(); i++) { names[i + 1] = folders.get(i); ids[i + 1] = folders.get(i); }
            b.dlg().setTitle(Theme.s("Move to…", "نقل إلى…")).setItems(names, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface d, int which) {
                    b.store.moveBookmark(e, ids[which]);
                    refresh();
                }
            }).show();
        }
    }
}
