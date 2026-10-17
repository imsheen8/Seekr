package __APP_ID__;

import android.graphics.Bitmap;
import android.webkit.WebView;

/** تبويب واحد: WebView (يُنشأ عند الحاجة) + بيانات العرض. */
final class Tab {
    private static int counter = 0;

    final int id = ++counter;
    final boolean incognito;
    WebView web;
    String pendingUrl;      // يُحمَّل عند إنشاء الـ WebView (استعادة الجلسة)
    String url = "";
    String title = "";
    Bitmap icon;
    Bitmap thumb;
    int progress = 100;
    boolean loading;
    boolean ntp = true;     // true: تُعرض صفحة «علامة تبويب جديدة»
    boolean desktop;
    Tab parent;

    Tab(boolean incognito) {
        this.incognito = incognito;
    }

    boolean hasPage() {
        return web != null && url != null && !url.isEmpty();
    }

    String displayTitle() {
        if (ntp || (title.isEmpty() && url.isEmpty())) return Theme.s("New tab", "علامة تبويب جديدة");
        return title.isEmpty() ? url : title;
    }
}
