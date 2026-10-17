package __APP_ID__;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** دوال مساعدة لبناء العناصر برمجيًا. */
final class U {
    private U() {}

    static TextView tv(Context c, String text, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextColor(color);
        Theme.sp(t, sp);
        return t;
    }

    static ImageView icon(Context c, int kind, int sizeDp, int color) {
        ImageView v = new ImageView(c);
        v.setImageDrawable(new Ico(kind, color));
        v.setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(sizeDp), Theme.dp(sizeDp)));
        return v;
    }

    /** زر أيقونة دائري بمساحة لمس 44dp. */
    static ImageView ibtn(Context c, int kind, int color, View.OnClickListener l) {
        ImageView v = new ImageView(c);
        v.setImageDrawable(new Ico(kind, color));
        int p = Theme.dp(10);
        v.setPadding(p, p, p, p);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(44), Theme.dp(44)));
        Theme.pressable(v);
        v.setBackground(Theme.rippleBg(0, 22));
        if (l != null) v.setOnClickListener(l);
        return v;
    }

    static void setIco(ImageView v, int kind, int color) {
        v.setImageDrawable(new Ico(kind, color));
    }

    static View divider(Context c) {
        View d = new View(c);
        d.setBackgroundColor(Theme.divider);
        d.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Theme.dp(0.7f))));
        return d;
    }

    static View space(Context c, int hDp) {
        View d = new View(c);
        d.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(hDp)));
        return d;
    }

    /** دائرة بها أيقونة الموقع أو أول حرف منه. */
    static View siteBadge(Context c, Store store, String url, String title, int sizeDp) {
        Bitmap ic = store == null ? null : store.icon(url);
        if (ic != null) {
            ImageView iv = new ImageView(c);
            iv.setImageBitmap(ic);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int p = Theme.dp(sizeDp * 0.22f);
            iv.setPadding(p, p, p, p);
            iv.setBackground(Theme.round(Theme.pill, sizeDp / 2f));
            iv.setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(sizeDp), Theme.dp(sizeDp)));
            return iv;
        }
        String key = Store.hostOf(url);
        String letter = "";
        if (key != null && !key.isEmpty()) letter = key.substring(0, 1).toUpperCase();
        else if (title != null && !title.isEmpty()) letter = title.substring(0, 1).toUpperCase();
        TextView t = tv(c, letter, sizeDp * 0.42f, Theme.dark ? 0xFF1A1412 : 0xFFFFFFFF);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setBackground(Theme.round(Theme.letterColor(key), sizeDp / 2f));
        t.setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(sizeDp), Theme.dp(sizeDp)));
        return t;
    }

    static TextView ellipsized(TextView t, int lines) {
        t.setSingleLine(lines == 1);
        if (lines > 1) t.setMaxLines(lines);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    /** عدّاد التبويبات: مربع بحواف دائرية وفيه الرقم. */
    static final class CountView extends View {
        private int count = 1;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF r = new android.graphics.RectF();

        CountView(Context c) {
            super(c);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Theme.dp(2));
            txt.setTextAlign(Paint.Align.CENTER);
            txt.setTypeface(Typeface.DEFAULT_BOLD);
            setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(44), Theme.dp(44)));
            Theme.pressable(this);
            setBackground(Theme.rippleBg(0, 22));
        }

        void setCount(int n, int color) {
            count = n;
            stroke.setColor(color);
            txt.setColor(color);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float s = Theme.dp(24);
            r.set((w - s) / 2, (h - s) / 2, (w + s) / 2, (h + s) / 2);
            c.drawRoundRect(r, Theme.dp(6), Theme.dp(6), stroke);
            txt.setTextSize(Theme.dp(count > 99 ? 9 : 13));
            String t = count > 99 ? ":D" : String.valueOf(count);
            c.drawText(t, w / 2, h / 2 + txt.getTextSize() * 0.36f, txt);
        }
    }

    static FrameLayout.LayoutParams fill() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
