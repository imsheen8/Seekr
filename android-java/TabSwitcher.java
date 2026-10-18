package __APP_ID__;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** مبدّل التبويبات بتصميم كروم: تبويبات / متخفٍّ / مجموعات، مع بطاقات المجموعات وصفحة تفاصيل المجموعة. */
final class TabSwitcher extends LinearLayout implements SettingsPages.Refreshable {
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final BrowserView b;
    private int mode;               // 0 عادي، 1 متخفٍّ، 2 مجموعات
    private boolean listMode = false;
    private final LinearLayout grid;
    private final EditText search;
    private final FrameLayout segNormal, segIncog, segGroups;
    private final U.CountView seg1Count;
    private final ImageView seg2Icon, seg3Icon;

    TabSwitcher(final BrowserView b) {
        super(b.act);
        this.b = b;
        this.mode = b.incognitoMode ? 1 : 0;
        setOrientation(VERTICAL);
        setBackgroundColor(Theme.bg);
        setClickable(true);

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
            @Override public void onClick(View v) { b.openInNewTab(null, mode == 1); }
        });
        top.addView(plus, new LinearLayout.LayoutParams(Theme.dp(60), Theme.dp(50)));

        top.addView(new View(b.act), new LinearLayout.LayoutParams(0, 1, 1f));

        LinearLayout seg = new LinearLayout(b.act);
        seg.setOrientation(HORIZONTAL);
        seg.setBackground(Theme.round(Theme.surface, 26));
        seg.setPadding(Theme.dp(4), Theme.dp(4), Theme.dp(4), Theme.dp(4));
        segNormal = new FrameLayout(b.act);
        seg1Count = new U.CountView(b.act);
        segNormal.addView(seg1Count, new FrameLayout.LayoutParams(Theme.dp(44), Theme.dp(44), Gravity.CENTER));
        segNormal.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { mode = 0; b.selectMode(false); refresh(); }
        });
        seg.addView(segNormal, new LinearLayout.LayoutParams(Theme.dp(64), Theme.dp(44)));
        segIncog = new FrameLayout(b.act);
        seg2Icon = U.icon(b.act, Ico.INCOGNITO, 24, Theme.text);
        segIncog.addView(seg2Icon, new FrameLayout.LayoutParams(Theme.dp(24), Theme.dp(24), Gravity.CENTER));
        segIncog.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { mode = 1; b.selectMode(true); refresh(); }
        });
        seg.addView(segIncog, new LinearLayout.LayoutParams(Theme.dp(64), Theme.dp(44)));
        segGroups = new FrameLayout(b.act);
        seg3Icon = U.icon(b.act, Ico.GRID, 24, Theme.text);
        segGroups.addView(seg3Icon, new FrameLayout.LayoutParams(Theme.dp(24), Theme.dp(24), Gravity.CENTER));
        segGroups.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { mode = 2; refresh(); }
        });
        seg.addView(segGroups, new LinearLayout.LayoutParams(Theme.dp(64), Theme.dp(44)));
        top.addView(seg);

        top.addView(new View(b.act), new LinearLayout.LayoutParams(0, 1, 1f));

        final ImageView more = U.ibtn(b.act, Ico.MENU, Theme.text, null);
        more.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showMore(more); }
        });
        top.addView(more);
        addView(top);

        search = new EditText(b.act);
        search.setHint(Theme.s("Search your tabs", "ابحث في تبويباتك"));
        search.setSingleLine(true);
        search.setTextColor(Theme.text);
        search.setHintTextColor(Theme.dim);
        search.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        Theme.sp(search, 17);
        search.setBackground(Theme.round(Theme.surface, 28));
        search.setPadding(Theme.dp(24), 0, Theme.dp(24), 0);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(MATCH, Theme.dp(54));
        sl.setMargins(Theme.dp(14), Theme.dp(6), Theme.dp(14), Theme.dp(10));
        addView(search, sl);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void afterTextChanged(Editable e) { refresh(); }
        });

        ScrollView sv = new ScrollView(b.act);
        sv.setFillViewport(true);
        grid = new LinearLayout(b.act);
        grid.setOrientation(VERTICAL);
        grid.setPadding(Theme.dp(8), 0, Theme.dp(8), Theme.dp(24));
        sv.addView(grid, new FrameLayout.LayoutParams(MATCH, WRAP));
        addView(sv, new LinearLayout.LayoutParams(MATCH, 0, 1f));
        refresh();
    }

    private void showMore(View anchor) {
        PopupMenu pm = new PopupMenu(b.act, anchor);
        Menu m = pm.getMenu();
        boolean incog = mode == 1;
        m.add(0, 1, 0, incog ? Theme.s("Close all Incognito tabs", "إغلاق كل التبويبات المتخفّية") : Theme.s("Close all tabs", "إغلاق كل التبويبات"));
        m.add(0, 2, 1, listMode ? Theme.s("Grid view", "عرض شبكي") : Theme.s("List view", "عرض قائمة"));
        m.add(0, 3, 2, Theme.s("New tab", "تبويب جديد"));
        m.add(0, 4, 3, Theme.s("New Incognito tab", "تبويب متخفٍّ جديد"));
        m.add(0, 5, 4, Theme.s("Settings", "الإعدادات"));
        pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem it) {
                switch (it.getItemId()) {
                    case 1: b.closeAll(mode == 1); if (b.hasPages()) refresh(); break;
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

    // =====================================================================
    //  بناء المحتوى
    // =====================================================================
    @Override
    public void refresh() {
        boolean hasIncog = b.countFor(true) > 0 || mode == 1;
        segIncog.setVisibility(hasIncog ? View.VISIBLE : View.GONE);
        segNormal.setBackground(mode == 0 ? Theme.round(Theme.accentBg, 22) : null);
        segIncog.setBackground(mode == 1 ? Theme.round(Theme.accentBg, 22) : null);
        segGroups.setBackground(mode == 2 ? Theme.round(Theme.accentBg, 22) : null);
        seg1Count.setCount(b.countFor(false), mode == 0 ? Theme.accentOn : Theme.text);
        U.setIco(seg2Icon, Ico.INCOGNITO, mode == 1 ? Theme.accentOn : Theme.text);
        U.setIco(seg3Icon, Ico.GRID, mode == 2 ? Theme.accentOn : Theme.text);
        grid.removeAllViews();
        if (mode == 2) renderGroups(); else renderTabs(mode == 1);
    }

    private boolean matches(Tab t, String q) {
        return q.isEmpty() || t.displayTitle().toLowerCase().contains(q) || (t.url != null && t.url.toLowerCase().contains(q));
    }

    private void renderTabs(boolean incog) {
        String q = search.getText().toString().trim().toLowerCase();
        List<View> cells = new ArrayList<>();
        List<TabGroup> emitted = new ArrayList<>();
        for (Tab t : b.tabs) {
            if (t.incognito != incog) continue;
            if (t.group != null && !incog) {
                final TabGroup g = t.group;
                if (emitted.contains(g)) continue;
                boolean any = q.isEmpty() || groupLabel(b, g).toLowerCase().contains(q);
                if (!any) for (Tab m : b.tabsOf(g)) if (matches(m, q)) { any = true; break; }
                if (!any) continue;
                emitted.add(g);
                cells.add(groupCard(g));
                continue;
            }
            if (!matches(t, q)) continue;
            cells.add(tabCard(b, t, listMode, new Runnable() { @Override public void run() { refresh(); } }));
        }
        layoutCells(grid, cells, listMode ? 1 : 2);
        if (cells.isEmpty()) {
            TextView empty = U.tv(b.act, incog ? Theme.s("No Incognito tabs", "لا توجد تبويبات متخفّية") : Theme.s("No tabs", "لا توجد تبويبات"), 16, Theme.dim);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Theme.dp(60), 0, 0);
            grid.addView(empty);
        }
    }

    static void layoutCells(LinearLayout parent, List<View> cells, int cols) {
        LinearLayout row = null;
        for (int i = 0; i < cells.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(parent.getContext());
                row.setOrientation(HORIZONTAL);
                parent.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
            }
            row.addView(cells.get(i));
        }
        if (row != null && cols == 2 && cells.size() % 2 == 1) row.addView(new View(parent.getContext()), new LinearLayout.LayoutParams(0, 1, 1f));
    }

    // =====================================================================
    //  المجموعات
    // =====================================================================
    static String groupLabel(BrowserView b, TabGroup g) {
        if (!g.title.isEmpty()) return g.title;
        int n = b.groupSize(g);
        return n == 1 ? Theme.s("1 tab", "علامة تبويب واحدة") : Theme.s(n + " tabs", n + " علامات تبويب");
    }

    static String ago(long ts) {
        long d = Math.max(0, System.currentTimeMillis() - ts) / 1000;
        if (d < 60) return Theme.s("Updated just now", "حُدّثت الآن");
        if (d < 3600) return Theme.s("Updated " + (d / 60) + " min ago", "حُدّثت منذ " + (d / 60) + " د");
        if (d < 86400) return Theme.s("Updated " + (d / 3600) + " hr ago", "حُدّثت منذ " + (d / 3600) + " س");
        return Theme.s("Updated " + (d / 86400) + " days ago", "حُدّثت منذ " + (d / 86400) + " يوم");
    }

    static View dot(Context c, int color, int sizeDp) {
        View v = new View(c);
        v.setBackground(Theme.round(color, sizeDp / 2f));
        v.setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(sizeDp), Theme.dp(sizeDp)));
        return v;
    }

    /** مصغّرات 2×2 لأول أربعة تبويبات في المجموعة. */
    static View preview(BrowserView b, TabGroup g, int heightDp, boolean round) {
        LinearLayout box = new LinearLayout(b.act);
        box.setOrientation(VERTICAL);
        box.setPadding(Theme.dp(3), Theme.dp(3), Theme.dp(3), Theme.dp(3));
        box.setBackground(Theme.round(Theme.dark ? 0xFF120D0B : 0xFFF3E6E2, round ? 18 : 14));
        List<Tab> ts = g.closed ? new ArrayList<Tab>() : b.tabsOf(g);
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(b.act);
            row.setOrientation(HORIZONTAL);
            for (int c = 0; c < 2; c++) {
                int i = r * 2 + c;
                FrameLayout cell = new FrameLayout(b.act);
                cell.setBackground(Theme.round(Theme.dark ? 0xFF261E1B : 0xFFFFFFFF, 10));
                LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, MATCH, 1f);
                cl.setMargins(Theme.dp(3), Theme.dp(3), Theme.dp(3), Theme.dp(3));
                String url = null, title = null;
                Bitmap thumb = null;
                if (g.closed) {
                    if (i < g.saved.size()) { title = g.saved.get(i)[0]; url = g.saved.get(i)[1]; }
                } else if (i < ts.size()) {
                    Tab t = ts.get(i);
                    url = t.ntp ? null : t.url;
                    title = t.title;
                    thumb = t.thumb;
                    if (t.ntp) { ImageView ic = U.icon(b.act, Ico.SEARCH, 18, Theme.accent); cell.addView(ic, new FrameLayout.LayoutParams(Theme.dp(18), Theme.dp(18), Gravity.CENTER)); }
                }
                if (thumb != null) {
                    ImageView iv = new ImageView(b.act);
                    iv.setImageBitmap(thumb);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    cell.addView(iv, U.fill());
                    cell.setClipToOutline(true);
                    cell.setOutlineProvider(new ViewOutlineProvider() {
                        @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), Theme.dp(10)); }
                    });
                } else if (url != null) {
                    cell.addView(U.siteBadge(b.act, b.store, url, title, 26), new FrameLayout.LayoutParams(Theme.dp(26), Theme.dp(26), Gravity.CENTER));
                }
                row.addView(cell, cl);
            }
            box.addView(row, new LinearLayout.LayoutParams(MATCH, 0, 1f));
        }
        box.setLayoutParams(new LinearLayout.LayoutParams(MATCH, Theme.dp(heightDp)));
        return box;
    }

    private View groupCard(final TabGroup g) {
        boolean sel = b.cur != null && b.cur.group == g;
        int fg = sel ? Theme.accentOn : Theme.text;
        LinearLayout card = new LinearLayout(b.act);
        card.setOrientation(VERTICAL);
        card.setBackground(Theme.rippleBg(sel ? Theme.accentBg : Theme.surface, 26));
        card.setClickable(true);
        card.setPadding(Theme.dp(6), Theme.dp(4), Theme.dp(6), Theme.dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        lp.setMargins(Theme.dp(6), Theme.dp(6), Theme.dp(6), Theme.dp(6));
        card.setLayoutParams(lp);

        LinearLayout head = new LinearLayout(b.act);
        head.setOrientation(HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(Theme.dp(22), Theme.dp(22));
        dl.setMarginStart(Theme.dp(8));
        head.addView(dot(b.act, g.color, 22), dl);
        TextView title = U.ellipsized(U.tv(b.act, groupLabel(b, g), 15, fg), 1);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, WRAP, 1f);
        tl.setMarginStart(Theme.dp(10));
        head.addView(title, tl);
        final ImageView more = U.ibtn(b.act, Ico.MENU, fg, null);
        more.setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(40), Theme.dp(40)));
        more.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { groupMenu(more, g); }
        });
        head.addView(more);
        card.addView(head);

        View pv = preview(b, g, listMode ? 130 : 210, true);
        LinearLayout.LayoutParams pl = (LinearLayout.LayoutParams) pv.getLayoutParams();
        pl.setMargins(Theme.dp(2), Theme.dp(4), Theme.dp(2), 0);
        card.addView(pv, pl);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openGroup(g); }
        });
        return card;
    }

    private void renderGroups() {
        List<TabGroup> gs = new ArrayList<>(b.groups);
        Collections.sort(gs, new Comparator<TabGroup>() {
            @Override public int compare(TabGroup x, TabGroup y) { return Long.compare(y.updated, x.updated); }
        });
        String q = search.getText().toString().trim().toLowerCase();
        int n = 0;
        for (final TabGroup g : gs) {
            if (!q.isEmpty() && !groupLabel(b, g).toLowerCase().contains(q)) continue;
            n++;
            LinearLayout row = new LinearLayout(b.act);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Theme.dp(12), Theme.dp(12), Theme.dp(4), Theme.dp(12));
            row.setBackground(Theme.rippleBg(Theme.surface, 26));
            row.setClickable(true);
            View pv = preview(b, g, 86, false);
            row.addView(pv, new LinearLayout.LayoutParams(Theme.dp(86), Theme.dp(86)));
            LinearLayout col = new LinearLayout(b.act);
            col.setOrientation(VERTICAL);
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, WRAP, 1f);
            cl.setMarginStart(Theme.dp(18));
            row.addView(col, cl);
            LinearLayout line = new LinearLayout(b.act);
            line.setOrientation(HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(Theme.dp(16), Theme.dp(16));
            dl.setMarginEnd(Theme.dp(10));
            line.addView(dot(b.act, g.color, 16), dl);
            line.addView(U.ellipsized(U.tv(b.act, groupLabel(b, g), 19, Theme.text), 1));
            col.addView(line);
            TextView sub = U.tv(b.act, ago(g.updated), 15, Theme.dim);
            sub.setPadding(0, Theme.dp(6), 0, 0);
            sub.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            col.addView(sub);
            final ImageView more = U.ibtn(b.act, Ico.MENU, Theme.text, null);
            more.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { groupMenu(more, g); }
            });
            row.addView(more);
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { openGroup(g); }
            });
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(MATCH, WRAP);
            rl.setMargins(Theme.dp(6), Theme.dp(5), Theme.dp(6), Theme.dp(5));
            grid.addView(row, rl);
        }
        if (n == 0) {
            grid.addView(BookmarksPages.emptyState(b.act, Ico.GRID, Theme.s("You'll find your tab groups here", "ستجد مجموعات التبويبات هنا"),
                    Theme.s("Create groups to organize your tabs.", "أنشئ مجموعات لتنظيم تبويباتك.")));
        }
    }

    private void openGroup(TabGroup g) {
        if (g.closed) { b.restoreGroup(g); return; }
        b.pushPage(new GroupPage(b, g));
    }

    private void groupMenu(View anchor, final TabGroup g) {
        PopupMenu pm = new PopupMenu(b.act, anchor);
        Menu m = pm.getMenu();
        if (g.closed) m.add(0, 5, 0, Theme.s("Open", "فتح"));
        else m.add(0, 1, 0, Theme.s("Close", "إغلاق"));
        m.add(0, 2, 1, Theme.s("Rename", "إعادة تسمية"));
        if (!g.closed) m.add(0, 3, 2, Theme.s("Ungroup", "فصل المجموعة"));
        m.add(0, 4, 3, Theme.s("Delete", "حذف"));
        pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem it) {
                switch (it.getItemId()) {
                    case 1: b.closeGroup(g); refresh(); break;
                    case 2: renameDialog(b, g, new Runnable() { @Override public void run() { refresh(); } }); break;
                    case 3: b.ungroup(g); refresh(); break;
                    case 5: b.restoreGroup(g); break;
                    default: b.deleteGroup(g); refresh(); break;
                }
                return true;
            }
        });
        pm.show();
    }

    static void renameDialog(final BrowserView b, final TabGroup g, final Runnable after) {
        final EditText in = new EditText(b.act);
        in.setSingleLine(true);
        in.setText(g.title);
        in.setHint(Theme.s("Group name", "اسم المجموعة"));
        b.dlg().setTitle(Theme.s("Rename", "إعادة تسمية")).setView(in)
                .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        g.title = in.getText().toString().trim();
                        g.updated = System.currentTimeMillis();
                        after.run();
                    }
                }).setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
    }

    static void colorDialog(final BrowserView b, final TabGroup g, final Runnable after) {
        final android.app.AlertDialog[] dlg = new android.app.AlertDialog[1];
        LinearLayout box = new LinearLayout(b.act);
        box.setOrientation(VERTICAL);
        box.setPadding(Theme.dp(20), Theme.dp(16), Theme.dp(20), Theme.dp(8));
        LinearLayout row = null;
        for (int i = 0; i < TabGroup.PALETTE.length; i++) {
            if (i % 5 == 0) {
                row = new LinearLayout(b.act);
                row.setOrientation(HORIZONTAL);
                box.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
            }
            final int col = TabGroup.PALETTE[i];
            FrameLayout cell = new FrameLayout(b.act);
            View d = dot(b.act, col, 38);
            FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(Theme.dp(38), Theme.dp(38), Gravity.CENTER);
            cell.addView(d, dl);
            if (col == g.color) {
                ImageView ck = U.icon(b.act, Ico.CHECK, 22, 0xFF1A1412);
                cell.addView(ck, new FrameLayout.LayoutParams(Theme.dp(22), Theme.dp(22), Gravity.CENTER));
            }
            cell.setContentDescription(Theme.s(TabGroup.COLOR_NAMES_EN[i], TabGroup.COLOR_NAMES_AR[i]));
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    g.color = col;
                    if (dlg[0] != null) dlg[0].dismiss();
                    after.run();
                }
            });
            row.addView(cell, new LinearLayout.LayoutParams(Theme.dp(54), Theme.dp(54)));
        }
        dlg[0] = b.dlg().setTitle(Theme.s("Group color", "لون المجموعة")).setView(box).show();
    }

    // =====================================================================
    //  بطاقة التبويب
    // =====================================================================
    static View tabCard(final BrowserView b, final Tab t, boolean listMode, final Runnable rerender) {
        boolean sel = t == b.cur;
        int fg = sel ? Theme.accentOn : Theme.text;
        LinearLayout card = new LinearLayout(b.act);
        card.setOrientation(VERTICAL);
        card.setBackground(Theme.rippleBg(sel ? Theme.accentBg : Theme.surface, 26));
        card.setClickable(true);
        card.setPadding(Theme.dp(6), Theme.dp(4), Theme.dp(6), Theme.dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
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
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, WRAP, 1f);
        tl.setMarginStart(Theme.dp(8));
        head.addView(title, tl);
        ImageView x = U.ibtn(b.act, Ico.CLOSE, fg, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                b.closeTab(t);
                if (b.hasPages()) rerender.run();
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
        LinearLayout.LayoutParams thl = new LinearLayout.LayoutParams(MATCH, Theme.dp(h));
        thl.setMargins(Theme.dp(2), Theme.dp(4), Theme.dp(2), 0);
        card.addView(thumbBox, thl);

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { b.switchTo(t); }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                tabMenu(b, v, t, rerender);
                return true;
            }
        });
        return card;
    }

    private static void tabMenu(final BrowserView b, View anchor, final Tab t, final Runnable rerender) {
        PopupMenu pm = new PopupMenu(b.act, anchor);
        Menu m = pm.getMenu();
        final List<TabGroup> open = new ArrayList<>();
        for (TabGroup g : b.groups) if (!g.closed && g != t.group) open.add(g);
        if (!t.incognito) {
            m.add(0, 1, 0, Theme.s("Add to new group", "إضافة إلى مجموعة جديدة"));
            if (!open.isEmpty()) m.add(0, 2, 1, Theme.s("Add to group…", "إضافة إلى مجموعة…"));
            if (t.group != null) m.add(0, 3, 2, Theme.s("Remove from group", "إزالة من المجموعة"));
        }
        m.add(0, 4, 3, Theme.s("Close tab", "إغلاق التبويب"));
        pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem it) {
                switch (it.getItemId()) {
                    case 1: b.newGroupFor(t); rerender.run(); break;
                    case 2: {
                        String[] names = new String[open.size()];
                        for (int i = 0; i < names.length; i++) names[i] = groupLabel(b, open.get(i));
                        b.dlg().setTitle(Theme.s("Add to group", "إضافة إلى مجموعة")).setItems(names, new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                b.addToGroup(t, open.get(which));
                                rerender.run();
                            }
                        }).show();
                        break;
                    }
                    case 3: b.removeFromGroup(t); rerender.run(); break;
                    default: b.closeTab(t); if (b.hasPages()) rerender.run(); break;
                }
                return true;
            }
        });
        pm.show();
    }

    // =====================================================================
    //  صفحة تفاصيل المجموعة
    // =====================================================================
    static final class GroupPage extends LinearLayout implements SettingsPages.Refreshable {
        private final BrowserView b;
        private final TabGroup g;
        private final LinearLayout cells;
        private final LinearLayout dotHolder;

        GroupPage(final BrowserView b, final TabGroup g) {
            super(b.act);
            this.b = b;
            this.g = g;
            setOrientation(VERTICAL);
            setBackgroundColor(Theme.bg);
            setClickable(true);

            LinearLayout h = new LinearLayout(b.act);
            h.setOrientation(HORIZONTAL);
            h.setGravity(Gravity.CENTER_VERTICAL);
            h.setPadding(Theme.dp(8), Theme.dp(8), Theme.dp(8), Theme.dp(4));
            ImageView back = U.ibtn(b.act, Ico.BACK, Theme.text, new View.OnClickListener() {
                @Override public void onClick(View v) { b.popPage(); }
            });
            if (Theme.ar) back.setScaleX(-1f);
            h.addView(back);
            h.addView(new View(b.act), new LinearLayout.LayoutParams(0, 1, 1f));
            h.addView(U.ibtn(b.act, Ico.PLUS, Theme.text, new View.OnClickListener() {
                @Override public void onClick(View v) { b.addTabToGroup(g); }
            }));
            final ImageView more = U.ibtn(b.act, Ico.MENU, Theme.text, null);
            more.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { menu(more); }
            });
            h.addView(more);
            addView(h);

            LinearLayout tr = new LinearLayout(b.act);
            tr.setOrientation(HORIZONTAL);
            tr.setGravity(Gravity.CENTER_VERTICAL);
            tr.setPadding(Theme.dp(24), Theme.dp(8), Theme.dp(24), Theme.dp(12));
            dotHolder = new LinearLayout(b.act);
            dotHolder.setGravity(Gravity.CENTER);
            dotHolder.setClickable(true);
            dotHolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    colorDialog(b, g, new Runnable() { @Override public void run() { refresh(); } });
                }
            });
            tr.addView(dotHolder, new LinearLayout.LayoutParams(Theme.dp(44), Theme.dp(44)));
            final EditText name = new EditText(b.act);
            name.setSingleLine(true);
            name.setText(g.title);
            name.setHint(groupLabel(b, g));
            name.setTextColor(Theme.text);
            name.setHintTextColor(Theme.text);
            name.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            Theme.sp(name, 26);
            name.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.divider));
            name.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
                @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
                @Override
                public void afterTextChanged(Editable e) {
                    g.title = e.toString().trim();
                    g.updated = System.currentTimeMillis();
                }
            });
            LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(0, WRAP, 1f);
            nl.setMarginStart(Theme.dp(12));
            tr.addView(name, nl);
            addView(tr);

            ScrollView sv = new ScrollView(b.act);
            sv.setFillViewport(true);
            cells = new LinearLayout(b.act);
            cells.setOrientation(VERTICAL);
            cells.setPadding(Theme.dp(8), 0, Theme.dp(8), Theme.dp(24));
            sv.addView(cells, new FrameLayout.LayoutParams(MATCH, WRAP));
            addView(sv, new LinearLayout.LayoutParams(MATCH, 0, 1f));
            refresh();
        }

        private void menu(View anchor) {
            PopupMenu pm = new PopupMenu(b.act, anchor);
            Menu m = pm.getMenu();
            m.add(0, 1, 0, Theme.s("Close group", "إغلاق المجموعة"));
            m.add(0, 2, 1, Theme.s("Ungroup", "فصل المجموعة"));
            m.add(0, 3, 2, Theme.s("Delete group", "حذف المجموعة"));
            pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem it) {
                    switch (it.getItemId()) {
                        case 1: b.closeGroup(g); break;
                        case 2: b.ungroup(g); break;
                        default: b.deleteGroup(g); break;
                    }
                    if (b.hasPages()) b.popPage();
                    return true;
                }
            });
            pm.show();
        }

        @Override
        public void refresh() {
            dotHolder.removeAllViews();
            dotHolder.addView(dot(b.act, g.color, 30));
            cells.removeAllViews();
            List<View> list = new ArrayList<>();
            for (Tab t : b.tabsOf(g)) {
                list.add(tabCard(b, t, false, new Runnable() {
                    @Override
                    public void run() {
                        if (!b.groups.contains(g)) { if (b.hasPages()) b.popPage(); return; }
                        refresh();
                    }
                }));
            }
            layoutCells(cells, list, 2);
        }
    }
}
