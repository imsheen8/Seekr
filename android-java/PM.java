package __APP_ID__;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;

/** قائمة منبثقة مرسومة بالكامل بألوان التطبيق (بديلة عن PopupMenu الخاص بالنظام). */
final class PM {
    static final class Item {
        final int id;
        Item(int id) { this.id = id; }
        int getItemId() { return id; }
    }

    interface OnItem { boolean onMenuItemClick(Item it); }

    private static final class Row {
        int id;
        String title;
        boolean check, checked, divider;
    }

    private final Context ctx;
    private final View anchor;
    private final ArrayList<Row> rows = new ArrayList<>();
    private OnItem listener;

    PM(Context ctx, View anchor) {
        this.ctx = ctx;
        this.anchor = anchor;
    }

    PM add(int id, String title) {
        Row r = new Row();
        r.id = id;
        r.title = title;
        rows.add(r);
        return this;
    }

    /** عنصر بعلامة اختيار ✓ عند التفعيل. */
    PM addChecked(int id, String title, boolean checked) {
        Row r = new Row();
        r.id = id;
        r.title = title;
        r.check = true;
        r.checked = checked;
        rows.add(r);
        return this;
    }

    PM divider() {
        Row r = new Row();
        r.divider = true;
        rows.add(r);
        return this;
    }

    void setOnMenuItemClickListener(OnItem l) { listener = l; }

    void show() {
        final PopupWindow[] pw = new PopupWindow[1];
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, Theme.dp(8), 0, Theme.dp(8));
        box.setBackground(Theme.round(Theme.pill, 20));
        for (final Row r : rows) {
            if (r.divider) {
                View d = U.divider(ctx);
                LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Theme.dp(0.7f)));
                dl.setMargins(Theme.dp(8), Theme.dp(6), Theme.dp(8), Theme.dp(6));
                box.addView(d, dl);
                continue;
            }
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Theme.dp(22), 0, Theme.dp(22), 0);
            Theme.pressable(row);
            row.setBackground(Theme.rippleBg(0, 0));
            TextView t = U.tv(ctx, r.title, 16, Theme.text);
            t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (r.check) {
                View ck = U.icon(ctx, Ico.CHECK, 22, r.checked ? Theme.accent : 0x00000000);
                LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(Theme.dp(22), Theme.dp(22));
                cl.setMarginStart(Theme.dp(18));
                row.addView(ck, cl);
            }
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (pw[0] != null) pw[0].dismiss();
                    if (listener != null) listener.onMenuItemClick(new Item(r.id));
                }
            });
            box.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(50)));
        }
        ScrollView sv = new ScrollView(ctx);
        sv.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sv.setMinimumWidth(Theme.dp(210));
        PopupWindow w = new PopupWindow(sv, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        w.setBackgroundDrawable(new ColorDrawable(0));
        w.setOutsideTouchable(true);
        w.setElevation(Theme.dp(10));
        w.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        sv.setScrollBarSize(0);
        pw[0] = w;
        try {
            w.showAsDropDown(anchor, 0, 0, Gravity.END);
        } catch (Throwable t) {
            w.showAtLocation(anchor, Gravity.CENTER, 0, 0);
        }
    }
}
