package __APP_ID__;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import androidx.activity.OnBackPressedCallback;
import com.getcapacitor.BridgeActivity;
import java.io.File;
import org.json.JSONObject;

public class MainActivity extends BridgeActivity {
    /* 0 = يعمل التحميل، 1 = جاهز، -1 = فشل */
    private static volatile int state = 0;
    private static volatile int port = 0;
    private static volatile String token = "";
    private static volatile String error = "";
    private static boolean starting = false;

    private BrowserView browser;
    private View splashTop;
    private boolean launched = false;
    private OnBackPressedCallback backCb;

    private static void startPython(final Context ctx) {
        synchronized (MainActivity.class) {
            if (starting) return;
            starting = true;
        }
        final Context app = ctx.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!com.chaquo.python.Python.isStarted()) {
                        com.chaquo.python.Python.start(new com.chaquo.python.android.AndroidPlatform(app));
                    }
                    com.chaquo.python.PyObject mod = com.chaquo.python.Python.getInstance().getModule("seekr_android");
                    String dir = new File(app.getFilesDir(), "seekr-data").getAbsolutePath();
                    JSONObject info = new JSONObject(mod.callAttr("start", dir).toString());
                    port = info.getInt("port");
                    token = info.getString("token");
                    state = 1;
                } catch (Throwable t) {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    t.printStackTrace(new java.io.PrintWriter(sw));
                    String s = sw.toString();
                    error = s.length() > 4000 ? s.substring(s.length() - 4000) : s;
                    state = -1;
                    synchronized (MainActivity.class) { starting = false; }
                }
            }
        }, "seekr-python").start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Theme.init(this);
        browser = new BrowserView(this, new Store(this));
        ViewGroup content = (ViewGroup) findViewById(android.R.id.content);
        content.addView(browser, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        startPython(this);

        WebView wv = getBridge().getWebView();
        View top = wv;
        while (top.getParent() != null && top.getParent() != content && top.getParent() instanceof View) {
            top = (View) top.getParent();
        }
        splashTop = top;

        wv.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public String status() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("state", state == 1 ? "ready" : (state < 0 ? "error" : "starting"));
                    o.put("port", port);
                    o.put("token", token);
                    o.put("error", error);
                    return o.toString();
                } catch (Exception e) {
                    return "{\"state\":\"starting\"}";
                }
            }

            /** تستدعيها صفحة التحميل بعد انتهاء الانميشن: تُخفي الشاشة الافتتاحية وتُظهر المتصفح. */
            @JavascriptInterface
            public void launch() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        doLaunch();
                    }
                });
            }
        }, "SXApp");

        backCb = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (browser != null) browser.onBackPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCb);

        handleIntent(getIntent());
    }

    private void doLaunch() {
        if (launched || state != 1) return;
        launched = true;
        if (splashTop != null) splashTop.setVisibility(View.GONE);
        browser.start(port, token);
        backCb.setEnabled(true);
    }

    private void handleIntent(Intent i) {
        if (i == null || browser == null) return;
        String action = i.getAction();
        String target = null;
        if (Intent.ACTION_VIEW.equals(action) && i.getData() != null) {
            Uri d = i.getData();
            String sch = d.getScheme();
            if ("http".equals(sch) || "https".equals(sch)) target = d.toString();
        } else if (Intent.ACTION_WEB_SEARCH.equals(action)) {
            target = i.getStringExtra("query");
        } else if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(i.getType())) {
            target = i.getStringExtra(Intent.EXTRA_TEXT);
        }
        if (target != null && !target.trim().isEmpty()) {
            browser.openExternalUrl(target.trim());
            setIntent(new Intent(Intent.ACTION_MAIN));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onPause() {
        if (browser != null) browser.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (browser != null) browser.onResume();
    }

    @Override
    public void onDestroy() {
        if (browser != null) browser.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (browser != null && browser.onActivityResult(requestCode, resultCode, data)) return;
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (browser != null && browser.onRequestPermissionsResult(requestCode, permissions, grantResults)) return;
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
