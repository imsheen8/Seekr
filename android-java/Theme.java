package __APP_ID__;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import java.util.Locale;

/** ألوان وأبعاد ونصوص الواجهة (عربي / English). */
final class Theme {
    static boolean dark = true;
    static boolean ar = true;
    static float density = 1f;

    static int bg, surface, pill, text, dim, accent, accentBg, accentOn, divider, scrim;

    private Theme() {}

    static void init(Context c) {
        density = c.getResources().getDisplayMetrics().density;
        SharedPreferences p = c.getSharedPreferences("br", Context.MODE_PRIVATE);
        String th = p.getString("theme", "system");
        if ("dark".equals(th)) dark = true;
        else if ("light".equals(th)) dark = false;
        else dark = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        String lang = p.getString("lang", "ar");
        if ("auto".equals(lang)) ar = Locale.getDefault().getLanguage().equals("ar");
        else ar = "ar".equals(lang);
        if (dark) {
            bg = 0xFF1A1412; surface = 0xFF261E1B; pill = 0xFF3A302D; text = 0xFFF1E0DB; dim = 0xFFB0A19C;
            accent = 0xFFFFB59E; accentBg = 0xFFFFB59E; accentOn = 0xFF3B0A00; divider = 0xFF4A3F3B; scrim = 0x99000000;
        } else {
            bg = 0xFFFFF8F6; surface = 0xFFF8EDEA; pill = 0xFFEBDFDB; text = 0xFF231917; dim = 0xFF73655F;
            accent = 0xFFB3452B; accentBg = 0xFFFFDBD1; accentOn = 0xFF3B0A00; divider = 0xFFD8C2BC; scrim = 0x66000000;
        }
    }

    static int dp(float v) {
        return Math.round(v * density);
    }

    static String s(String en, String arabic) {
        return ar ? arabic : en;
    }

    static GradientDrawable round(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    static void sp(android.widget.TextView t, float size) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    static int letterColor(String key) {
        int h = key == null ? 0 : Math.abs(key.hashCode());
        float[] hsv = {h % 360, dark ? 0.35f : 0.45f, dark ? 0.75f : 0.65f};
        return Color.HSVToColor(hsv);
    }

    static void pressable(View v) {
        v.setClickable(true);
        v.setBackground(rippleBg(0, 20));
    }

    static android.graphics.drawable.Drawable rippleBg(int color, float radiusDp) {
        GradientDrawable base = round(color, radiusDp);
        android.graphics.drawable.RippleDrawable r = new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(dark ? 0x33FFFFFF : 0x22000000), base, round(0xFFFFFFFF, radiusDp));
        return r;
    }
}
