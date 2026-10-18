package __APP_ID__;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** واجهة المتصفح كاملة: شريط العنوان، التبويبات، القائمة، التنزيلات، البحث في الصفحة… */
final class BrowserView extends FrameLayout {
    static final int REQ_FILE = 7001, REQ_VOICE = 7002, REQ_PERM = 7101, REQ_GEO = 7102, REQ_NOTIF = 7103;
    static final String COOKIE = "sxng_app";

    final Activity act;
    final Store store;
    final Handler ui = new Handler(Looper.getMainLooper());
    String base = "";
    String token = "";
    boolean started = false;
    String pendingOpen = null;

    final List<Tab> tabs = new ArrayList<>();
    Tab cur;
    boolean incognitoMode = false;
    final ArrayList<String[]> closed = new ArrayList<>();
    String mobileUA = "";
    String desktopUA = "";

    // واجهة
    private LinearLayout main, toolbar, omniBox, findBar;
    private View ntpSpacer;
    private ImageView homeBtn, siteIcon, actionBtn, menuBtn;
    private U.CountView tabCount;
    private EditText omni, findInput;
    private TextView findCount;
    private FrameLayout progressHolder, content, webHolder, pageLayer, fullscreenHolder;
    private View progressBar;
    private NewTabView ntpView;
    private ScrollView suggestScroll;
    private LinearLayout suggestList;
    private final ArrayList<View> pages = new ArrayList<>();
    private boolean settingText = false;
    private int actionMode = 0; // 0 تحديث، 1 إيقاف، 2 مسح، 3 ميكروفون
    private int suggestSeq = 0;
    private Runnable suggestRunnable;

    // حالات معلّقة
    private ValueCallback<Uri[]> fileCb;
    private PermissionRequest pendingPerm;
    private String[] pendingPermRes;
    private GeolocationPermissions.Callback geoCb;
    private String geoOrigin;
    private View customView;
    private WebChromeClient.CustomViewCallback customCb;
    private BroadcastReceiver dlReceiver;

    BrowserView(Activity a, Store s) {
        super(a);
        act = a;
        store = s;
        setBackgroundColor(Theme.bg);
        setLayoutDirection(Theme.ar ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        buildUi();
        setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets in) {
                setPadding(0, in.getSystemWindowInsetTop(), 0, in.getSystemWindowInsetBottom());
                return in;
            }
        });
    }

    // =====================================================================
    //  بناء الواجهة
    // =====================================================================
    private void buildUi() {
        main = new LinearLayout(act);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setFocusable(true);
        main.setFocusableInTouchMode(true);
        addView(main, U.fill());

        toolbar = new LinearLayout(act);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(Theme.dp(4), Theme.dp(6), Theme.dp(4), Theme.dp(6));
        toolbar.setBackgroundColor(Theme.bg);
        main.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        homeBtn = U.ibtn(act, Ico.HOME, Theme.text, new View.OnClickListener() {
            @Override public void onClick(View v) { goHome(); }
        });
        homeBtn.setContentDescription(Theme.s("Home", "الرئيسية"));
        toolbar.addView(homeBtn);

        omniBox = new LinearLayout(act);
        omniBox.setOrientation(LinearLayout.HORIZONTAL);
        omniBox.setGravity(Gravity.CENTER_VERTICAL);
        omniBox.setBackground(Theme.round(Theme.pill, 24));
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(0, Theme.dp(46), 1f);
        olp.setMargins(Theme.dp(2), 0, Theme.dp(2), 0);
        toolbar.addView(omniBox, olp);
        ntpSpacer = new View(act);
        ntpSpacer.setVisibility(View.GONE);
        toolbar.addView(ntpSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        siteIcon = U.icon(act, Ico.SEARCH, 20, Theme.dim);
        ((LinearLayout.LayoutParams) siteIcon.getLayoutParams()).setMarginStart(Theme.dp(14));
        omniBox.addView(siteIcon);

        omni = new EditText(act);
        omni.setBackground(null);
        omni.setSingleLine(true);
        omni.setTextColor(Theme.text);
        omni.setHintTextColor(Theme.dim);
        omni.setHint(Theme.s("Search Seekr or type URL", "ابحث في Seekr أو اكتب عنوانًا"));
        Theme.sp(omni, 15);
        omni.setPadding(Theme.dp(10), 0, Theme.dp(6), 0);
        omni.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        omni.setImeOptions(EditorInfo.IME_ACTION_GO | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        omni.setHighlightColor(0x66FFB59E);
        omniBox.addView(omni, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        actionBtn = U.ibtn(act, Ico.REFRESH, Theme.text, new View.OnClickListener() {
            @Override public void onClick(View v) { onAction(); }
        });
        omniBox.addView(actionBtn, new LinearLayout.LayoutParams(Theme.dp(40), Theme.dp(40)));

        tabCount = new U.CountView(act);
        tabCount.setContentDescription(Theme.s("Tabs", "التبويبات"));
        tabCount.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showSwitcher(); }
        });
        toolbar.addView(tabCount);

        menuBtn = U.ibtn(act, Ico.MENU, Theme.text, new View.OnClickListener() {
            @Override public void onClick(View v) { showMenu(v); }
        });
        menuBtn.setContentDescription(Theme.s("More options", "المزيد"));
        toolbar.addView(menuBtn);

        progressHolder = new FrameLayout(act);
        progressBar = new View(act);
        progressBar.setBackgroundColor(Theme.accent);
        progressHolder.addView(progressBar, U.fill());
        progressHolder.setVisibility(View.INVISIBLE);
        main.addView(progressHolder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(3)));

        buildFindBar();
        // يُطبَّق موضع الشريط بعد بناء بقية العناصر
        post(new Runnable() { @Override public void run() { applyToolbarPrefs(); } });

        content = new FrameLayout(act);
        main.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        webHolder = new FrameLayout(act);
        content.addView(webHolder, U.fill());
        ntpView = new NewTabView(this);
        content.addView(ntpView, U.fill());

        suggestScroll = new ScrollView(act);
        suggestScroll.setBackgroundColor(Theme.bg);
        suggestScroll.setVisibility(View.GONE);
        suggestScroll.setFillViewport(true);
        suggestList = new LinearLayout(act);
        suggestList.setOrientation(LinearLayout.VERTICAL);
        suggestScroll.addView(suggestList, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(suggestScroll, U.fill());

        pageLayer = new FrameLayout(act);
        pageLayer.setVisibility(View.GONE);
        pageLayer.setBackgroundColor(Theme.bg);
        pageLayer.setClickable(true);
        addView(pageLayer, U.fill());

        fullscreenHolder = new FrameLayout(act);
        fullscreenHolder.setBackgroundColor(0xFF000000);
        fullscreenHolder.setVisibility(View.GONE);
        addView(fullscreenHolder, U.fill());

        // سلوك شريط العنوان
        omni.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean has) {
                if (has) {
                    setOmniText(editableText());
                    omni.post(new Runnable() { @Override public void run() { omni.selectAll(); } });
                    suggestScroll.setVisibility(View.VISIBLE);
                    updateSuggest(omni.getText().toString());
                } else {
                    suggestScroll.setVisibility(View.GONE);
                    hideIme();
                }
                refreshUi();
            }
        });
        omni.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable e) {
                if (settingText) return;
                if (omni.hasFocus()) {
                    updateSuggest(e.toString());
                    refreshActionIcon();
                }
            }
        });
        omni.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent ev) {
                boolean enter = ev != null && ev.getKeyCode() == KeyEvent.KEYCODE_ENTER && ev.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                    go(omni.getText().toString());
                    return true;
                }
                return false;
            }
        });
    }

    private void buildFindBar() {
        findBar = new LinearLayout(act);
        findBar.setOrientation(LinearLayout.HORIZONTAL);
        findBar.setGravity(Gravity.CENTER_VERTICAL);
        findBar.setBackgroundColor(Theme.surface);
        findBar.setPadding(Theme.dp(8), Theme.dp(4), Theme.dp(8), Theme.dp(4));
        findBar.setVisibility(View.GONE);
        findInput = new EditText(act);
        findInput.setBackground(null);
        findInput.setSingleLine(true);
        findInput.setTextColor(Theme.text);
        findInput.setHintTextColor(Theme.dim);
        findInput.setHint(Theme.s("Find in page", "بحث في الصفحة"));
        Theme.sp(findInput, 15);
        findInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        findBar.addView(findInput, new LinearLayout.LayoutParams(0, Theme.dp(44), 1f));
        findCount = U.tv(act, "", 13, Theme.dim);
        findBar.addView(findCount);
        findBar.addView(U.ibtn(act, Ico.BACK, Theme.text, new View.OnClickListener() {
            @Override public void onClick(View v) { if (cur != null && cur.web != null) cur.web.findNext(false); }
        }));
        findBar.addView(U.ibtn(act, Ico.FORWARD, Theme.text, new View.OnClickListener() {
            @Override public void onClick(View v) { if (cur != null && cur.web != null) cur.web.findNext(true); }
        }));
        findBar.addView(U.ibtn(act, Ico.CLOSE, Theme.text, new View.OnClickListener() {
            @Override public void onClick(View v) { hideFind(); }
        }));
        findInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable e) {
                if (cur == null || cur.web == null) return;
                if (e.length() == 0) { cur.web.clearMatches(); findCount.setText(""); }
                else cur.web.findAllAsync(e.toString());
            }
        });
        findInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent ev) {
                if (cur != null && cur.web != null) cur.web.findNext(true);
                hideIme();
                return true;
            }
        });
        main.addView(findBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    void showFind() {
        if (cur == null || cur.ntp || cur.web == null) return;
        findBar.setVisibility(View.VISIBLE);
        findInput.setText("");
        findInput.requestFocus();
        showIme(findInput);
    }

    void hideFind() {
        findBar.setVisibility(View.GONE);
        if (cur != null && cur.web != null) cur.web.clearMatches();
        hideIme();
        main.requestFocus();
    }

    // =====================================================================
    //  بدء التشغيل
    // =====================================================================
    @SuppressLint("SetJavaScriptEnabled")
    void start(int port, String tok) {
        if (started) return;
        started = true;
        base = "http://127.0.0.1:" + port;
        token = tok;
        applySystemUi();
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setCookie(base, COOKIE + "=" + token + "; Path=/; HttpOnly");
        // ألوان ولغة صفحة البحث تتبع إعدادات التطبيق
        cm.setCookie(base, "app_theme=" + (Theme.dark ? "dark" : "light") + "; Path=/; Max-Age=31536000");
        if (!"auto".equals(store.getStr("lang", "ar"))) cm.setCookie(base, "locale=" + (Theme.ar ? "ar" : "en") + "; Path=/; Max-Age=31536000");
        cm.flush();
        String ua = WebSettings.getDefaultUserAgent(act);
        mobileUA = ua.replace("; wv", "").replaceAll("Version/\\d+\\.\\d+ ", "");
        desktopUA = mobileUA.replaceAll("\\(Linux; Android[^)]*\\)", "(X11; Linux x86_64)").replace(" Mobile", "");

        dlReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                toast(Theme.s("Download complete", "اكتمل التنزيل"));
            }
        };
        try {
            act.registerReceiver(dlReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } catch (Throwable ignored) {
            dlReceiver = null;
        }
        if (Build.VERSION.SDK_INT >= 33 && act.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            try { act.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF); } catch (Throwable ignored) { }
        }

        restoreSession();
        if (tabs.isEmpty()) addTab(false, null, true);
        if (pendingOpen != null) {
            String u = pendingOpen;
            pendingOpen = null;
            addTab(false, resolve(u), true);
        }
        refreshUi();
    }

    void openExternalUrl(String url) {
        if (!started) { pendingOpen = url; return; }
        addTab(false, resolve(url), true);
    }

    void applySystemUi() {
        try {
            act.getWindow().setStatusBarColor(Theme.bg);
            act.getWindow().setNavigationBarColor(Theme.bg);
            int f = 0;
            if (!Theme.dark) {
                f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            act.getWindow().getDecorView().setSystemUiVisibility(f);
        } catch (Throwable ignored) { }
    }

    // =====================================================================
    //  الجلسة
    // =====================================================================
    void saveSession() {
        try {
            JSONArray arr = new JSONArray();
            int idx = 0, n = 0;
            for (Tab t : tabs) {
                if (t.incognito) continue;
                JSONObject o = new JSONObject();
                o.put("u", t.url);
                o.put("t", t.title);
                o.put("n", t.ntp);
                if (t.group != null) o.put("g", t.group.id);
                if (t == cur) idx = n;
                arr.put(o);
                n++;
            }
            JSONObject root = new JSONObject();
            root.put("cur", idx);
            root.put("tabs", arr);
            JSONArray ga = new JSONArray();
            for (TabGroup g : groups) {
                JSONObject go = new JSONObject();
                go.put("id", g.id);
                go.put("t", g.title);
                go.put("c", g.color);
                go.put("u", g.updated);
                go.put("x", g.closed);
                JSONArray sv = new JSONArray();
                for (String[] s : g.saved) sv.put(new JSONArray().put(s[0]).put(s[1]));
                go.put("s", sv);
                ga.put(go);
            }
            root.put("groups", ga);
            store.saveSession(root.toString());
        } catch (Throwable ignored) { }
    }

    private void restoreSession() {
        try {
            if (!store.getBool("restore_tabs", true)) return;
            String s = store.loadSession();
            if (s == null) return;
            JSONObject root = new JSONObject(s);
            JSONArray arr = root.getJSONArray("tabs");
            int idx = root.optInt("cur", 0);
            java.util.HashMap<Integer, TabGroup> gmap = new java.util.HashMap<>();
            JSONArray ga = root.optJSONArray("groups");
            if (ga != null) {
                for (int i = 0; i < ga.length(); i++) {
                    JSONObject go = ga.getJSONObject(i);
                    TabGroup g = new TabGroup();
                    g.id = go.optInt("id", g.id);
                    TabGroup.bump(g.id);
                    g.title = go.optString("t", "");
                    g.color = go.optInt("c", TabGroup.PALETTE[0]);
                    g.updated = go.optLong("u", g.updated);
                    g.closed = go.optBoolean("x", false);
                    JSONArray sv = go.optJSONArray("s");
                    if (sv != null) for (int k = 0; k < sv.length(); k++) {
                        JSONArray e = sv.getJSONArray(k);
                        g.saved.add(new String[]{e.optString(0), e.optString(1)});
                    }
                    groups.add(g);
                    gmap.put(g.id, g);
                }
            }
            Tab sel = null;
            for (int i = 0; i < arr.length() && i < 40; i++) {
                JSONObject o = arr.getJSONObject(i);
                Tab t = new Tab(false);
                t.url = o.optString("u");
                t.title = o.optString("t");
                t.ntp = o.optBoolean("n", true) || t.url.isEmpty();
                if (!t.ntp) t.pendingUrl = fixLocal(t.url);
                if (o.has("g")) t.group = gmap.get(o.optInt("g"));
                tabs.add(t);
                if (i == idx) sel = t;
            }
            if (!tabs.isEmpty()) switchTo(sel != null ? sel : tabs.get(0));
        } catch (Throwable ignored) { }
    }

    private String fixLocal(String u) {
        return u == null ? null : u.replaceFirst("^http://127\\.0\\.0\\.1:\\d+", base);
    }

    // =====================================================================
    //  التبويبات
    // =====================================================================
    Tab addTab(boolean incog, String url, boolean select) {
        Tab t = new Tab(incog);
        t.parent = cur;
        t.desktop = store.getBool("desktop", false);
        if (url != null) {
            t.ntp = false;
            t.url = url;
            t.title = Store.hostOf(url);
            t.pendingUrl = url;
        }
        if (!select && !incog && t.parent != null && !t.parent.incognito) t.group = t.parent.group;
        tabs.add(t);
        if (select) switchTo(t);
        else {
            if (!quietAdd) toast(Theme.s("Opened in a new tab", "فُتحت في تبويب جديد"));
            refreshUi();
        }
        return t;
    }

    private boolean quietAdd;

    // =====================================================================
    //  مجموعات التبويبات
    // =====================================================================
    final ArrayList<TabGroup> groups = new ArrayList<>();

    List<Tab> tabsOf(TabGroup g) {
        List<Tab> out = new ArrayList<>();
        for (Tab t : tabs) if (t.group == g) out.add(t);
        return out;
    }

    int groupSize(TabGroup g) {
        return g.closed ? g.saved.size() : tabsOf(g).size();
    }

    TabGroup newGroupFor(Tab t) {
        TabGroup g = new TabGroup();
        g.color = TabGroup.PALETTE[groups.size() % TabGroup.PALETTE.length];
        groups.add(g);
        addToGroup(t, g);
        return g;
    }

    void addToGroup(Tab t, TabGroup g) {
        if (t == null || g == null || t.incognito) return;
        TabGroup old = t.group;
        t.group = g;
        g.updated = System.currentTimeMillis();
        g.closed = false;
        if (old != null && old != g) cleanupGroup(old);
    }

    void removeFromGroup(Tab t) {
        TabGroup g = t.group;
        t.group = null;
        if (g != null) { g.updated = System.currentTimeMillis(); cleanupGroup(g); }
    }

    void cleanupGroup(TabGroup g) {
        if (!g.closed && tabsOf(g).isEmpty()) groups.remove(g);
    }

    void ungroup(TabGroup g) {
        for (Tab t : tabsOf(g)) t.group = null;
        groups.remove(g);
    }

    void deleteGroup(TabGroup g) {
        List<Tab> ts = tabsOf(g);
        groups.remove(g);
        for (Tab t : ts) closeTab(t);
    }

    /** «إغلاق» المجموعة: تُغلق تبويباتها وتبقى محفوظة في قائمة المجموعات. */
    void closeGroup(TabGroup g) {
        g.saved.clear();
        List<Tab> ts = tabsOf(g);
        for (Tab t : ts) if (!t.ntp && t.url != null && !t.url.isEmpty()) g.saved.add(new String[]{t.title, t.url});
        g.closed = true;
        g.updated = System.currentTimeMillis();
        for (Tab t : ts) closeTab(t);
    }

    void restoreGroup(TabGroup g) {
        g.closed = false;
        g.updated = System.currentTimeMillis();
        Tab first = null;
        quietAdd = true;
        try {
            for (String[] s : g.saved) {
                Tab t = addTab(false, s[1], false);
                t.title = s[0] == null ? "" : s[0];
                t.group = g;
                if (first == null) first = t;
            }
            if (first == null) { first = addTab(false, null, false); first.group = g; }
        } finally {
            quietAdd = false;
        }
        g.saved.clear();
        switchTo(first);
    }

    Tab addTabToGroup(TabGroup g) {
        Tab t = addTab(false, null, true);
        addToGroup(t, g);
        return t;
    }

    void switchTo(Tab t) {
        if (cur != null && cur != t) captureThumb(cur);
        hideFind();
        cur = t;
        incognitoMode = t.incognito;
        if (t.incognito) lastIncog = t; else lastNormal = t;
        closePages();
        omni.clearFocus();
        showCurrent();
        refreshUi();
    }

    private Tab lastNormal, lastIncog;

    /** من مبدّل التبويبات: ينقل التبويب النشط إلى آخر تبويب من النوع المختار (عادي/متخفٍّ). */
    void selectMode(boolean incog) {
        if (cur != null && cur.incognito == incog) return;
        Tab target = incog ? lastIncog : lastNormal;
        if (target == null || !tabs.contains(target)) {
            target = null;
            for (Tab o : tabs) if (o.incognito == incog) target = o;
        }
        if (target == null) return;
        if (cur != null) captureThumb(cur);
        cur = target;
        incognitoMode = incog;
        if (incog) lastIncog = target; else lastNormal = target;
        showCurrent();
        refreshUi();
    }

    int countFor(boolean incog) {
        int n = 0;
        for (Tab t : tabs) if (t.incognito == incog) n++;
        return n;
    }

    void closeTab(Tab t) {
        int i = tabs.indexOf(t);
        if (i < 0) return;
        tabs.remove(i);
        if (t.group != null) { t.group.updated = System.currentTimeMillis(); cleanupGroup(t.group); }
        if (!t.incognito && t.url != null && !t.url.isEmpty() && !t.ntp) {
            closed.add(0, new String[]{t.title, t.url});
            while (closed.size() > 15) closed.remove(closed.size() - 1);
        }
        destroyWeb(t);
        if (t == cur) {
            Tab next = null;
            if (t.parent != null && tabs.contains(t.parent)) next = t.parent;
            if (next == null) {
                for (int k = Math.min(i, tabs.size() - 1); k >= 0 && next == null; k--) {
                    if (tabs.get(k).incognito == t.incognito) next = tabs.get(k);
                }
            }
            if (next == null) for (Tab o : tabs) if (o.incognito == t.incognito) { next = o; break; }
            if (next == null && !tabs.isEmpty()) next = tabs.get(tabs.size() - 1);
            cur = null;
            if (next == null) {
                addTab(false, null, true);
            } else {
                cur = next;
                incognitoMode = next.incognito;
                showCurrent();
                refreshUi();
            }
        } else {
            refreshUi();
        }
    }

    void closeAllTabs() {
        closeAll(false);
        closeAll(true);
    }

    void closeAll(boolean incog) {
        List<Tab> list = new ArrayList<>(tabs);
        for (Tab t : list) {
            if (t.incognito == incog) {
                tabs.remove(t);
                destroyWeb(t);
            }
        }
        if (cur != null && !tabs.contains(cur)) {
            cur = null;
            Tab next = null;
            for (Tab o : tabs) { next = o; break; }
            if (next == null) addTab(false, null, true);
            else { cur = next; incognitoMode = next.incognito; showCurrent(); refreshUi(); }
        }
        refreshUi();
    }

    private void destroyWeb(Tab t) {
        if (t.web == null) return;
        try {
            webHolder.removeView(t.web);
            t.web.stopLoading();
            if (t.incognito) {
                t.web.clearHistory();
                t.web.clearCache(true);
                t.web.clearFormData();
            }
            t.web.setWebViewClient(new WebViewClient());
            t.web.setWebChromeClient(new WebChromeClient());
            t.web.destroy();
        } catch (Throwable ignored) { }
        t.web = null;
    }

    private void showCurrent() {
        webHolder.removeAllViews();
        if (cur == null) return;
        if (cur.ntp) {
            webHolder.setVisibility(View.GONE);
            ntpView.setVisibility(View.VISIBLE);
            ntpView.bind(cur);
        } else {
            ensureWeb(cur);
            ntpView.setVisibility(View.GONE);
            webHolder.setVisibility(View.VISIBLE);
            webHolder.addView(cur.web, U.fill());
            cur.web.onResume();
        }
    }

    private void ensureWeb(Tab t) {
        if (t.web != null) return;
        t.web = createWebView(t);
        if (t.pendingUrl != null) {
            String u = t.pendingUrl;
            t.pendingUrl = null;
            t.web.loadUrl(u);
        }
    }

    void captureThumb(Tab t) {
        try {
            if (t == null || t.ntp || t.web == null) return;
            int w = t.web.getWidth(), h = t.web.getHeight();
            if (w <= 0 || h <= 0) return;
            float sc = Theme.dp(190f) / (float) w;
            Bitmap b = Bitmap.createBitmap(Math.max(1, (int) (w * sc)), Math.max(1, (int) (h * sc)), Bitmap.Config.RGB_565);
            Canvas c = new Canvas(b);
            c.scale(sc, sc);
            t.web.draw(c);
            t.thumb = b;
        } catch (Throwable ignored) { }
    }

    // =====================================================================
    //  التنقّل
    // =====================================================================
    String resolve(String input) {
        String s = input == null ? "" : input.trim();
        if (s.isEmpty()) return null;
        String low = s.toLowerCase();
        if (low.startsWith("http://") || low.startsWith("https://")) return s;
        if (low.startsWith("about:")) return s;
        if (low.startsWith("javascript:") || low.startsWith("file:") || low.startsWith("content:")) return null;
        if (s.indexOf(' ') < 0) {
            String hostPart = s.split("[/?#]", 2)[0];
            if (hostPart.matches("^(localhost|(\\d{1,3}\\.){3}\\d{1,3})(:\\d+)?$") || hostPart.endsWith(".local")) return "http://" + s;
            if (hostPart.matches("^[\\p{L}\\p{N}-]+(\\.[\\p{L}\\p{N}-]+)+(:\\d+)?$")) return "https://" + s;
        }
        try {
            return base + "/search?q=" + URLEncoder.encode(s, "UTF-8");
        } catch (Throwable t) {
            return base + "/search?q=" + s;
        }
    }

    void go(String input) {
        String url = resolve(input);
        omni.clearFocus();
        main.requestFocus();
        if (url == null) {
            if (input != null && !input.trim().isEmpty()) toast(Theme.s("This address is not supported", "هذا العنوان غير مدعوم"));
            return;
        }
        loadInCurrent(url);
    }

    void loadInCurrent(String url) {
        if (cur == null) { addTab(false, url, true); return; }
        closePages();
        cur.ntp = false;
        cur.pendingUrl = null;
        cur.url = url;
        if (cur.title.isEmpty()) cur.title = Store.hostOf(url);
        boolean fresh = cur.web == null;
        ensureWeb(cur);
        showCurrent();
        cur.web.loadUrl(url);
        refreshUi();
        if (fresh) cur.web.requestFocus();
    }

    void openInNewTab(String url, boolean incog) {
        closePages();
        addTab(incog, url, true);
    }

    void goHome() {
        if (cur == null) return;
        closePages();
        omni.clearFocus();
        String hp = store.getStr("homepage_url", "");
        if (!hp.isEmpty()) { go(hp); return; }
        cur.ntp = true;
        showCurrent();
        refreshUi();
    }

    boolean canBack() {
        return cur != null && ((!cur.ntp && cur.web != null && cur.web.canGoBack()) || (cur.ntp && cur.hasPage()));
    }

    boolean canForward() {
        return cur != null && !cur.ntp && cur.web != null && cur.web.canGoForward();
    }

    void back() {
        if (cur == null) return;
        if (!cur.ntp && cur.web != null && cur.web.canGoBack()) cur.web.goBack();
        else if (cur.ntp && cur.hasPage()) { cur.ntp = false; showCurrent(); refreshUi(); }
    }

    void forward() {
        if (canForward()) cur.web.goForward();
    }

    void reload() {
        if (cur != null && !cur.ntp && cur.web != null) cur.web.reload();
    }

    /** زر الرجوع في الجهاز. يعيد true إن تمت معالجته. */
    boolean onBackPressed() {
        if (customView != null) { hideCustomView(); return true; }
        if (!pages.isEmpty()) { popPage(); return true; }
        if (omni.hasFocus()) { omni.clearFocus(); main.requestFocus(); return true; }
        if (findBar.getVisibility() == View.VISIBLE) { hideFind(); return true; }
        if (cur != null) {
            if (canBack()) { back(); return true; }
            if (cur.parent != null && tabs.contains(cur.parent)) {
                Tab p = cur.parent;
                closeTab(cur);
                if (cur != p) switchTo(p);
                return true;
            }
        }
        act.moveTaskToBack(true);
        return true;
    }

    private boolean isSeekrHome(String u) {
        return u != null && (u.equals(base) || u.equals(base + "/"));
    }

    // =====================================================================
    //  الصفحات الكاملة (سجل، تنزيلات…)
    // =====================================================================
    void pushPage(View v) {
        omni.clearFocus();
        hideIme();
        pages.add(v);
        pageLayer.addView(v, U.fill());
        pageLayer.setVisibility(View.VISIBLE);
        v.setAlpha(0f);
        v.setTranslationY(Theme.dp(24));
        v.animate().alpha(1f).translationY(0).setDuration(180).start();
    }

    void popPage() {
        if (pages.isEmpty()) return;
        View v = pages.remove(pages.size() - 1);
        pageLayer.removeView(v);
        if (pages.isEmpty()) pageLayer.setVisibility(View.GONE);
        else {
            View top = pages.get(pages.size() - 1);
            if (top instanceof SettingsPages.Refreshable) ((SettingsPages.Refreshable) top).refresh();
        }
        refreshUi();
    }

    /** يطبّق تفضيلات الشريط: زر الرئيسية وموضع شريط العنوان (أعلى/أسفل). */
    void applyToolbarPrefs() {
        homeBtn.setVisibility(store.getBool("homepage_on", true) ? View.VISIBLE : View.GONE);
        boolean bottom = "bottom".equals(store.getStr("addr_pos", "top"));
        int idx = main.indexOfChild(toolbar);
        if (bottom && idx != main.getChildCount() - 1) {
            main.removeView(toolbar);
            main.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else if (!bottom && idx != 0) {
            main.removeView(toolbar);
            main.addView(toolbar, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    interface ApiCb { void done(String body, String err); }

    /** طلب GET إلى واجهة الخادم المحلي؛ يُنفَّذ النداء على خيط الواجهة. */
    void api(final String path, final ApiCb cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String body = null, err = null;
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(base + path).openConnection();
                    c.setRequestProperty("Cookie", COOKIE + "=" + token);
                    c.setConnectTimeout(4000);
                    c.setReadTimeout(8000);
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();
                    body = sb.toString().trim();
                    if (body.startsWith("{")) { err = new JSONObject(body).optString("error", "?"); body = null; }
                } catch (Throwable t) {
                    err = String.valueOf(t);
                }
                final String fb = body, fe = err;
                ui.post(new Runnable() { @Override public void run() { cb.done(fb, fe); } });
            }
        }, "seekr-api").start();
    }

    void closePages() {
        pages.clear();
        pageLayer.removeAllViews();
        pageLayer.setVisibility(View.GONE);
    }

    boolean hasPages() {
        return !pages.isEmpty();
    }

    void showSwitcher() {
        captureThumb(cur);
        pushPage(Pages.tabSwitcher(this));
    }

    void showHistory() { pushPage(Pages.history(this)); }
    void showBookmarks() { pushPage(Pages.bookmarks(this)); }
    void showDownloads() { pushPage(Pages.downloads(this)); }
    void showSettings() { pushPage(SettingsPages.main(this)); }

    // =====================================================================
    //  تحديث الواجهة
    // =====================================================================
    void refreshUi() {
        if (cur == null) return;
        tabCount.setCount(countFor(incognitoMode), Theme.text);
        boolean f = omni.hasFocus();
        // في صفحة «علامة تبويب جديدة» يكون مربع البحث الكبير هو الوحيد (مثل كروم)
        boolean hideBar = cur.ntp && !f;
        omniBox.setVisibility(hideBar ? View.GONE : View.VISIBLE);
        if (ntpSpacer != null) ntpSpacer.setVisibility(hideBar ? View.VISIBLE : View.GONE);
        if (!f) setOmniText(displayText(cur));
        if (f) U.setIco(siteIcon, Ico.SEARCH, Theme.dim);
        else if (cur.ntp) U.setIco(siteIcon, cur.incognito ? Ico.INCOGNITO : Ico.SEARCH, Theme.dim);
        else if (cur.url != null && cur.url.startsWith("https://")) U.setIco(siteIcon, Ico.LOCK, Theme.dim);
        else if (cur.url != null && cur.url.startsWith(base)) U.setIco(siteIcon, Ico.SEARCH, Theme.dim);
        else U.setIco(siteIcon, Ico.INFO, Theme.dim);
        refreshActionIcon();
        updateProgress();
        if (cur.ntp) ntpView.bind(cur);
    }

    private void refreshActionIcon() {
        boolean f = omni.hasFocus();
        if (f) {
            if (omni.getText().length() > 0) { actionMode = 2; U.setIco(actionBtn, Ico.CLOSE, Theme.text); }
            else { actionMode = 3; U.setIco(actionBtn, Ico.MIC, Theme.text); }
        } else if (cur == null || cur.ntp) {
            actionMode = 3;
            U.setIco(actionBtn, Ico.MIC, Theme.text);
        } else if (cur.loading) {
            actionMode = 1;
            U.setIco(actionBtn, Ico.CLOSE, Theme.text);
        } else {
            actionMode = 0;
            U.setIco(actionBtn, Ico.REFRESH, Theme.text);
        }
    }

    private void updateProgress() {
        if (cur == null) return;
        boolean show = cur.loading && !cur.ntp;
        progressHolder.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        progressBar.setPivotX(Theme.ar ? progressHolder.getWidth() : 0);
        progressBar.setScaleX(Math.max(0.04f, cur.progress / 100f));
    }

    private void setOmniText(String s) {
        settingText = true;
        omni.setText(s);
        settingText = false;
    }

    private String editableText() {
        if (cur == null || cur.ntp) return "";
        String q = searchQuery(cur.url);
        return q != null ? q : cur.url;
    }

    private String displayText(Tab t) {
        if (t.ntp || t.url == null || t.url.isEmpty()) return "";
        String q = searchQuery(t.url);
        if (q != null) return q;
        if (t.url.startsWith(base)) return "Seekr";
        return Store.hostOf(t.url);
    }

    private String searchQuery(String url) {
        try {
            if (url != null && url.startsWith(base + "/search")) return Uri.parse(url).getQueryParameter("q");
        } catch (Throwable ignored) { }
        return null;
    }

    private void onAction() {
        switch (actionMode) {
            case 0: reload(); break;
            case 1: if (cur != null && cur.web != null) cur.web.stopLoading(); break;
            case 2: omni.setText(""); break;
            default: startVoice(); break;
        }
    }

    void focusOmnibox() {
        omniBox.setVisibility(View.VISIBLE);
        if (ntpSpacer != null) ntpSpacer.setVisibility(View.GONE);
        omni.requestFocus();
        showIme(omni);
    }

    void showIme(View v) {
        try {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(v, 0);
        } catch (Throwable ignored) { }
    }

    void hideIme() {
        try {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        } catch (Throwable ignored) { }
    }

    void toast(String s) {
        Toast.makeText(act, s, Toast.LENGTH_SHORT).show();
    }

    AlertDialog.Builder dlg() {
        return new AlertDialog.Builder(act, Theme.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
    }

    // =====================================================================
    //  الاقتراحات
    // =====================================================================
    private void updateSuggest(final String q) {
        final String t = q == null ? "" : q.trim();
        suggestList.removeAllViews();
        suggestSeq++;
        final int seq = suggestSeq;
        if (suggestRunnable != null) ui.removeCallbacks(suggestRunnable);
        if (t.isEmpty()) {
            List<Store.Entry> h = store.history();
            int n = 0;
            for (Store.Entry e : h) {
                if (e.url.startsWith(base)) {
                    String qq = searchQuery(e.url);
                    if (qq == null) continue;
                    addSuggestRow(Ico.HISTORY, qq, null, e.url);
                } else {
                    addSuggestRow(Ico.HISTORY, e.title.isEmpty() ? e.url : e.title, Store.hostOf(e.url), e.url);
                }
                if (++n >= 8) break;
            }
            return;
        }
        String direct = resolve(t);
        boolean isUrl = direct != null && !direct.startsWith(base + "/search");
        addSuggestRow(isUrl ? Ico.GLOBE : Ico.SEARCH, t, isUrl ? direct : null, direct == null ? null : direct);
        String lq = t.toLowerCase();
        int n = 0;
        ArrayList<String> seen = new ArrayList<>();
        for (Store.Entry e : store.bookmarks()) {
            if (n >= 3) break;
            if (e.title.toLowerCase().contains(lq) || e.url.toLowerCase().contains(lq)) {
                addSuggestRow(Ico.STAR, e.title, Store.hostOf(e.url), e.url);
                seen.add(e.url);
                n++;
            }
        }
        for (Store.Entry e : store.history()) {
            if (n >= 6) break;
            if (e.url.startsWith(base + "/search") || seen.contains(e.url)) continue;
            if (e.title.toLowerCase().contains(lq) || e.url.toLowerCase().contains(lq)) {
                addSuggestRow(Ico.HISTORY, e.title.isEmpty() ? e.url : e.title, Store.hostOf(e.url), e.url);
                seen.add(e.url);
                n++;
            }
        }
        suggestRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final List<String> res = fetchSuggestions(t);
                        ui.post(new Runnable() {
                            @Override
                            public void run() {
                                if (seq != suggestSeq || !omni.hasFocus()) return;
                                for (String r : res) {
                                    if (r.equalsIgnoreCase(t)) continue;
                                    String u = resolveSearch(r);
                                    addSuggestRow(Ico.SEARCH, r, null, u);
                                }
                            }
                        });
                    }
                }, "seekr-suggest").start();
            }
        };
        ui.postDelayed(suggestRunnable, 160);
    }

    private String resolveSearch(String q) {
        try {
            return base + "/search?q=" + URLEncoder.encode(q, "UTF-8");
        } catch (Throwable t) {
            return base + "/search?q=" + q;
        }
    }

    private List<String> fetchSuggestions(String q) {
        List<String> out = new ArrayList<>();
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(base + "/autocompleter?q=" + URLEncoder.encode(q, "UTF-8")).openConnection();
            c.setRequestProperty("Cookie", COOKIE + "=" + token);
            c.setConnectTimeout(2500);
            c.setReadTimeout(4000);
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            JSONArray a = new JSONArray(sb.toString());
            JSONArray list = a.getJSONArray(1);
            for (int i = 0; i < list.length() && i < 6; i++) out.add(list.getString(i));
        } catch (Throwable ignored) { }
        return out;
    }

    private void addSuggestRow(int ico, String primary, String secondary, final String url) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Theme.dp(16), Theme.dp(10), Theme.dp(16), Theme.dp(10));
        Theme.pressable(row);
        row.setBackground(Theme.rippleBg(0, 0));
        ImageView iv = U.icon(act, ico, 22, Theme.dim);
        row.addView(iv);
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.setMarginStart(Theme.dp(16));
        row.addView(col, clp);
        col.addView(U.ellipsized(U.tv(act, primary, 16, Theme.text), 1));
        if (secondary != null && !secondary.isEmpty()) col.addView(U.ellipsized(U.tv(act, secondary, 13, Theme.dim), 1));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (url == null) return;
                omni.clearFocus();
                main.requestFocus();
                loadInCurrent(url);
            }
        });
        suggestList.addView(row);
    }

    // =====================================================================
    //  القائمة (على طراز كروم)
    // =====================================================================
    void showMenu(View anchor) {
        omni.clearFocus();
        hideIme();
        final PopupWindow[] holder = new PopupWindow[1];
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(Theme.ar ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        box.setPadding(0, Theme.dp(8), 0, Theme.dp(8));

        LinearLayout top = new LinearLayout(act);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER);
        top.setPadding(Theme.dp(8), 0, Theme.dp(8), Theme.dp(4));
        boolean cb = canBack(), cf = canForward();
        boolean page = cur != null && !cur.ntp && cur.url != null && !cur.url.isEmpty();
        boolean bm = page && store.isBookmarked(cur.url);
        top.addView(menuTopBtn(holder, Ico.BACK, cb, new Runnable() { @Override public void run() { back(); } }));
        top.addView(menuTopBtn(holder, Ico.FORWARD, cf, new Runnable() { @Override public void run() { forward(); } }));
        top.addView(menuTopBtn(holder, bm ? Ico.STAR_FILLED : Ico.STAR, page, new Runnable() {
            @Override
            public void run() {
                boolean added = store.toggleBookmark(cur.url, cur.title);
                toast(added ? Theme.s("Bookmark added", "أُضيفت الإشارة المرجعية") : Theme.s("Bookmark removed", "أُزيلت الإشارة المرجعية"));
            }
        }));
        top.addView(menuTopBtn(holder, Ico.SHARE, page, new Runnable() {
            @Override public void run() { share(cur.url, cur.title); }
        }));
        top.addView(menuTopBtn(holder, Ico.REFRESH, page, new Runnable() { @Override public void run() { reload(); } }));
        box.addView(top);

        box.addView(menuRow(holder, Ico.NEWTAB, Theme.s("New tab", "تبويب جديد"), false, new Runnable() {
            @Override public void run() { openInNewTab(null, incognitoMode); }
        }));
        box.addView(menuRow(holder, Ico.INCOGNITO, Theme.s("New Incognito tab", "تبويب متخفٍّ جديد"), false, new Runnable() {
            @Override public void run() { openInNewTab(null, true); }
        }));
        box.addView(U.divider(act));
        box.addView(menuRow(holder, Ico.HISTORY, Theme.s("History", "السجل"), false, new Runnable() {
            @Override public void run() { showHistory(); }
        }));
        box.addView(menuRow(holder, Ico.TRASH, Theme.s("Delete browsing data", "مسح بيانات التصفح"), false, new Runnable() {
            @Override public void run() { showClearDialog(); }
        }));
        box.addView(U.divider(act));
        box.addView(menuRow(holder, Ico.DOWNLOAD, Theme.s("Downloads", "التنزيلات"), false, new Runnable() {
            @Override public void run() { showDownloads(); }
        }));
        box.addView(menuRow(holder, Ico.STAR, Theme.s("Bookmarks", "الإشارات المرجعية"), false, new Runnable() {
            @Override public void run() { showBookmarks(); }
        }));
        if (page) {
            final boolean inRl = store.isInReading(cur.url);
            box.addView(menuRow(holder, Ico.READING, inRl ? Theme.s("Remove from reading list", "إزالة من قائمة القراءة") : Theme.s("Add to reading list", "إضافة إلى قائمة القراءة"), false, new Runnable() {
                @Override
                public void run() {
                    boolean added = store.toggleReading(cur.url, cur.title);
                    toast(added ? Theme.s("Added to reading list", "أُضيفت إلى قائمة القراءة") : Theme.s("Removed from reading list", "أُزيلت من قائمة القراءة"));
                }
            }));
        }
        box.addView(menuRow(holder, Ico.READING, Theme.s("Reading list", "قائمة القراءة"), false, new Runnable() {
            @Override public void run() { pushPage(BookmarksPages.root(BrowserView.this)); pushPage(new BookmarksPages.Pg(BrowserView.this, Store.READING)); }
        }));
        box.addView(menuRow(holder, Ico.RECENT, Theme.s("Recent tabs", "علامات التبويب الأخيرة"), false, new Runnable() {
            @Override public void run() { showRecent(); }
        }));
        box.addView(U.divider(act));
        if (page) {
            box.addView(menuRow(holder, Ico.SEARCH, Theme.s("Find in page", "بحث في الصفحة"), false, new Runnable() {
                @Override public void run() { showFind(); }
            }));
            box.addView(menuRow(holder, Ico.DESKTOP, Theme.s("Desktop site", "نسخة سطح المكتب"), cur.desktop, new Runnable() {
                @Override
                public void run() {
                    cur.desktop = !cur.desktop;
                    applyUA(cur);
                    cur.web.reload();
                }
            }));
        }
        box.addView(U.divider(act));
        box.addView(menuRow(holder, Ico.SETTINGS, Theme.s("Settings", "الإعدادات"), false, new Runnable() {
            @Override public void run() { showSettings(); }
        }));
        box.addView(menuRow(holder, Ico.HELP, Theme.s("Help & about", "مساعدة وحول التطبيق"), false, new Runnable() {
            @Override public void run() { showAbout(); }
        }));

        ScrollView sv = new ScrollView(act);
        sv.addView(box);
        PopupWindow pw = new PopupWindow(sv, Theme.dp(280), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        pw.setBackgroundDrawable(Theme.round(Theme.pill, 20));
        pw.setElevation(Theme.dp(10));
        pw.setOutsideTouchable(true);
        holder[0] = pw;
        int gx = Theme.ar ? Gravity.LEFT : Gravity.RIGHT;
        pw.showAtLocation(this, Gravity.TOP | gx, Theme.dp(8), getPaddingTop() + Theme.dp(8));
    }

    private View menuTopBtn(final PopupWindow[] h, int ico, boolean enabled, final Runnable r) {
        ImageView v = U.ibtn(act, ico, enabled ? Theme.text : Theme.dim, null);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, Theme.dp(48), 1f));
        v.setAlpha(enabled ? 1f : 0.45f);
        if (enabled) {
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View x) {
                    if (h[0] != null) h[0].dismiss();
                    r.run();
                }
            });
        }
        return v;
    }

    private View menuRow(final PopupWindow[] h, int ico, String label, boolean checked, final Runnable r) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Theme.dp(20), 0, Theme.dp(20), 0);
        Theme.pressable(row);
        row.setBackground(Theme.rippleBg(0, 0));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(50)));
        row.addView(U.icon(act, ico, 22, Theme.text));
        TextView t = U.tv(act, label, 16, Theme.text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(Theme.dp(18));
        row.addView(U.ellipsized(t, 1), lp);
        if (checked) row.addView(U.icon(act, Ico.CHECK, 20, Theme.accent));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (h[0] != null) h[0].dismiss();
                r.run();
            }
        });
        return row;
    }

    void share(String url, String title) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, url);
            if (title != null) i.putExtra(Intent.EXTRA_SUBJECT, title);
            act.startActivity(Intent.createChooser(i, null));
        } catch (Throwable ignored) { }
    }

    void copyText(String s) {
        try {
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("url", s));
            toast(Theme.s("Copied", "تم النسخ"));
        } catch (Throwable ignored) { }
    }

    void showRecent() {
        if (closed.isEmpty()) { toast(Theme.s("No recently closed tabs", "لا توجد تبويبات أُغلقت مؤخرًا")); return; }
        final String[] names = new String[closed.size()];
        for (int i = 0; i < names.length; i++) {
            String t = closed.get(i)[0];
            names[i] = (t == null || t.isEmpty()) ? closed.get(i)[1] : t;
        }
        dlg().setTitle(Theme.s("Recent tabs", "علامات التبويب الأخيرة")).setItems(names, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int which) {
                String[] e = closed.remove(which);
                addTab(false, e[1], true);
            }
        }).show();
    }

    void showAbout() {
        dlg().setTitle("Seekr")
                .setMessage(Theme.s("A private browser with a built-in search engine (SearXNG). Pages open inside the app; history, bookmarks and downloads are stored only on this device.",
                        "متصفح خاص بمحرك بحث مدمج (SearXNG). تُفتح الصفحات داخل التطبيق، ويُحفظ السجل والإشارات والتنزيلات على هذا الجهاز فقط."))
                .setPositiveButton("OK", null).show();
    }

    // =====================================================================
    //  مسح بيانات التصفح
    // =====================================================================
    void showClearDialog() {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Theme.dp(24), Theme.dp(8), Theme.dp(24), 0);
        final Spinner range = new Spinner(act);
        String[] opts = {Theme.s("Last hour", "آخر ساعة"), Theme.s("Last 24 hours", "آخر 24 ساعة"), Theme.s("All time", "كل الأوقات")};
        range.setAdapter(new ArrayAdapter<String>(act, android.R.layout.simple_spinner_dropdown_item, opts));
        range.setSelection(2);
        box.addView(range);
        final CheckBox cHist = new CheckBox(act);
        cHist.setText(Theme.s("Browsing history", "سجل التصفح"));
        cHist.setChecked(true);
        final CheckBox cCookies = new CheckBox(act);
        cCookies.setText(Theme.s("Cookies and site data", "ملفات تعريف الارتباط وبيانات المواقع"));
        final CheckBox cCache = new CheckBox(act);
        cCache.setText(Theme.s("Cached images and files", "الصور والملفات المخزّنة مؤقتًا"));
        final CheckBox cDl = new CheckBox(act);
        cDl.setText(Theme.s("Downloads list", "قائمة التنزيلات"));
        box.addView(cHist);
        box.addView(cCookies);
        box.addView(cCache);
        box.addView(cDl);
        dlg().setTitle(Theme.s("Delete browsing data", "مسح بيانات التصفح")).setView(box)
                .setPositiveButton(Theme.s("Delete", "مسح"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        long since = 0;
                        int sel = range.getSelectedItemPosition();
                        if (sel == 0) since = System.currentTimeMillis() - 3600_000L;
                        else if (sel == 1) since = System.currentTimeMillis() - 86400_000L;
                        clearData(cHist.isChecked(), cDl.isChecked(), cCookies.isChecked(), cCache.isChecked(), since);
                    }
                })
                .setNegativeButton(Theme.s("Cancel", "إلغاء"), null).show();
    }

    void clearData(boolean hist, boolean dls, boolean cookies, boolean cache, long since) {
        if (hist) {
            store.clearHistory(since);
            if (since == 0) store.clearIcons();
        }
        if (dls) store.clearDownloads();
        if (cache) {
            for (Tab t : tabs) if (t.web != null) t.web.clearCache(true);
        }
        if (cookies) {
            final CookieManager cm = CookieManager.getInstance();
            final String keep = cm.getCookie(base);
            cm.removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean ok) {
                    // نُبقي كوكيز خادم Seekr المحلي (رمز الحماية + تفضيلات البحث)
                    if (keep != null) {
                        for (String part : keep.split(";")) {
                            String p = part.trim();
                            if (!p.isEmpty()) cm.setCookie(base, p + "; Path=/");
                        }
                    }
                    cm.setCookie(base, COOKIE + "=" + token + "; Path=/; HttpOnly");
                    cm.flush();
                }
            });
            WebStorage.getInstance().deleteAllData();
        }
        toast(Theme.s("Browsing data deleted", "تم مسح البيانات"));
    }


    // =====================================================================
    //  إعدادات البحث (Seekr) تُحفظ في كوكيز الخادم المحلي — بلا مغادرة صفحة الإعدادات
    // =====================================================================
    String prefGet(String name, String def) {
        String all = CookieManager.getInstance().getCookie(base);
        if (all == null) return def;
        for (String part : all.split(";")) {
            String p = part.trim();
            int i = p.indexOf('=');
            if (i > 0 && p.substring(0, i).equals(name)) {
                String v = p.substring(i + 1);
                if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                return v;
            }
        }
        return def;
    }

    void prefSet(String name, String value) {
        String v = (value.indexOf(' ') >= 0 || value.indexOf(',') >= 0) ? "\"" + value + "\"" : value;
        CookieManager cm = CookieManager.getInstance();
        cm.setCookie(base, name + "=" + v + "; Path=/; Max-Age=31536000");
        cm.flush();
    }

    void resetSearchPrefs() {
        CookieManager cm = CookieManager.getInstance();
        String[] names = {"language", "safesearch", "autocomplete", "image_proxy", "disabled_engines", "enabled_engines",
                "categories", "method", "favicon_resolver", "preferences"};
        for (String n : names) cm.setCookie(base, n + "=; Path=/; Max-Age=0");
        cm.flush();
    }

    List<String[]> engineCache;
    String engineError = "";

    /** يجلب قائمة المحركات من الخادم المحلي ثم ينفّذ onDone على خيط الواجهة. */
    void loadEngines(final Runnable onDone) {
        if (engineCache != null) { onDone.run(); return; }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<String[]> out = new ArrayList<>();
                String err = "";
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(base + "/__api/engines").openConnection();
                    c.setRequestProperty("Cookie", COOKIE + "=" + token);
                    c.setConnectTimeout(4000);
                    c.setReadTimeout(8000);
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();
                    String body = sb.toString().trim();
                    if (body.startsWith("{")) {
                        err = new JSONObject(body).optString("error", "?");
                    } else {
                        JSONArray a = new JSONArray(body);
                        for (int i = 0; i < a.length(); i++) {
                            JSONObject o = a.getJSONObject(i);
                            out.add(new String[]{o.getString("key"), o.getString("name"), o.getString("cat"), o.getBoolean("on") ? "1" : "0"});
                        }
                    }
                } catch (Throwable t) {
                    err = String.valueOf(t);
                }
                final String errFinal = err;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!out.isEmpty()) engineCache = out;
                        engineError = out.isEmpty() ? (errFinal.isEmpty() ? "empty" : errFinal) : "";
                        onDone.run();
                    }
                });
            }
        }, "seekr-engines").start();
    }

    void showEngines() { pushPage(SettingsPages.engines(this)); }

    // =====================================================================
    //  التنزيلات
    // =====================================================================
    void download(String url, String ua, String cd, String mime) {
        try {
            if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
                toast(Theme.s("This download type is not supported", "نوع التنزيل هذا غير مدعوم"));
                return;
            }
            String name = URLUtil.guessFileName(url, cd, mime);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            if (mime != null && !mime.isEmpty()) r.setMimeType(mime);
            String ck = CookieManager.getInstance().getCookie(url);
            if (ck != null) r.addRequestHeader("Cookie", ck);
            if (ua != null) r.addRequestHeader("User-Agent", ua);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            DownloadManager dm = (DownloadManager) act.getSystemService(Context.DOWNLOAD_SERVICE);
            long id = dm.enqueue(r);
            store.addDownload(id, name, url, mime);
            toast(Theme.s("Downloading: ", "جارٍ التنزيل: ") + name);
        } catch (Throwable e) {
            toast(Theme.s("Download failed", "تعذّر التنزيل"));
        }
    }

    // =====================================================================
    //  البحث الصوتي
    // =====================================================================
    void startVoice() {
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Theme.ar ? "ar" : "en");
            act.startActivityForResult(i, REQ_VOICE);
        } catch (Throwable t) {
            toast(Theme.s("Voice search is not available", "البحث الصوتي غير متاح"));
        }
    }

    boolean onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE) {
            if (fileCb != null) {
                Uri[] r = null;
                try { r = WebChromeClient.FileChooserParams.parseResult(res, data); } catch (Throwable ignored) { }
                fileCb.onReceiveValue(r);
                fileCb = null;
            }
            return true;
        }
        if (req == REQ_VOICE) {
            if (res == Activity.RESULT_OK && data != null) {
                ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (r != null && !r.isEmpty()) go(r.get(0));
            }
            return true;
        }
        return false;
    }

    boolean onRequestPermissionsResult(int code, String[] perms, int[] results) {
        boolean ok = results.length > 0;
        for (int r : results) if (r != PackageManager.PERMISSION_GRANTED) ok = false;
        if (code == REQ_PERM) {
            if (pendingPerm != null) {
                try {
                    if (ok) pendingPerm.grant(pendingPermRes); else pendingPerm.deny();
                } catch (Throwable ignored) { }
                pendingPerm = null;
            }
            return true;
        }
        if (code == REQ_GEO) {
            if (geoCb != null) { geoCb.invoke(geoOrigin, ok, false); geoCb = null; }
            return true;
        }
        return code == REQ_NOTIF;
    }

    // =====================================================================
    //  WebView
    // =====================================================================
    void applyUA(Tab t) {
        if (t.web == null) return;
        t.web.getSettings().setUserAgentString(t.desktop ? desktopUA : mobileUA);
    }

    void applyTextZoom() {
        int z = store.getInt("zoom", 100);
        for (Tab t : tabs) if (t.web != null) t.web.getSettings().setTextZoom(z);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private WebView createWebView(final Tab t) {
        final WebView w = new WebView(act);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(store.getBool("js", true));
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setTextZoom(store.getInt("zoom", 100));
        s.setGeolocationEnabled(true);
        if (t.incognito) {
            s.setCacheMode(WebSettings.LOAD_NO_CACHE);
            s.setSaveFormData(false);
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, store.getBool("third_party_cookies", true));
        if (android.os.Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(store.getBool("safe_browsing", true));
        s.setUserAgentString(t.desktop ? desktopUA : mobileUA);

        w.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String ua, String cd, String mime, long len) {
                download(url, ua, cd, mime);
            }
        });

        w.setFindListener(new WebView.FindListener() {
            @Override
            public void onFindResultReceived(int active, int total, boolean done) {
                if (t == cur) findCount.setText(total == 0 ? "0/0" : (active + 1) + "/" + total);
            }
        });

        w.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView.HitTestResult hr = w.getHitTestResult();
                int type = hr.getType();
                final String extra = hr.getExtra();
                if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    Handler h = new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message m) {
                            String u = m.getData().getString("url");
                            String src = m.getData().getString("src");
                            showLinkMenu(t, u != null ? u : extra, src);
                        }
                    };
                    w.requestFocusNodeHref(h.obtainMessage());
                    return true;
                }
                if (type == WebView.HitTestResult.IMAGE_TYPE && extra != null) {
                    showLinkMenu(t, null, extra);
                    return true;
                }
                return false;
            }
        });

        w.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (isSeekrHome(u)) {
                    t.ntp = true;
                    if (t == cur) { showCurrent(); refreshUi(); }
                    return true;
                }
                String sch = r.getUrl().getScheme();
                if (sch == null) return false;
                if (sch.equals("http") || sch.equals("https") || sch.equals("about") || sch.equals("javascript")
                        || sch.equals("data") || sch.equals("blob")) return false;
                if (sch.equals("file") || sch.equals("content")) return true;
                openApp(v, u);
                return true;
            }

            @Override
            public void onPageStarted(WebView v, String url, Bitmap fav) {
                t.url = url;
                t.loading = true;
                t.progress = 5;
                if (t == cur) refreshUi();
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                t.loading = false;
                t.progress = 100;
                t.url = url;
                String title = v.getTitle();
                if (title != null && !title.isEmpty()) t.title = title;
                if (!t.incognito && url != null && url.startsWith("http") && !isSeekrHome(url)) store.addHistory(url, t.title);
                if (t == cur) refreshUi();
            }

            @Override
            public void doUpdateVisitedHistory(WebView v, String url, boolean isReload) {
                if (url != null && !url.startsWith("data:")) {
                    t.url = url;
                    if (t == cur && !omni.hasFocus()) refreshUi();
                }
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (!r.isForMainFrame()) return;
                String u = r.getUrl().toString();
                v.loadDataWithBaseURL(u, errorHtml(u, String.valueOf(e.getDescription())), "text/html", "UTF-8", u);
            }

            @Override
            public void onReceivedSslError(WebView v, final SslErrorHandler h, SslError e) {
                dlg().setTitle(Theme.s("Your connection is not private", "اتصالك غير خاص"))
                        .setMessage(Store.hostOf(e.getUrl()) + "\n" + Theme.s("The site's security certificate is not trusted.", "شهادة أمان الموقع غير موثوقة."))
                        .setPositiveButton(Theme.s("Proceed anyway", "المتابعة على أي حال"), new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int x) { h.proceed(); }
                        })
                        .setNegativeButton(Theme.s("Back to safety", "العودة"), new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int x) { h.cancel(); }
                        })
                        .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                            @Override public void onCancel(android.content.DialogInterface d) { h.cancel(); }
                        }).show();
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView v, final HttpAuthHandler h, String host, String realm) {
                LinearLayout box = new LinearLayout(act);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setPadding(Theme.dp(24), Theme.dp(8), Theme.dp(24), 0);
                final EditText user = new EditText(act);
                user.setHint(Theme.s("Username", "اسم المستخدم"));
                user.setSingleLine(true);
                final EditText pass = new EditText(act);
                pass.setHint(Theme.s("Password", "كلمة المرور"));
                pass.setSingleLine(true);
                pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                box.addView(user);
                box.addView(pass);
                dlg().setTitle(host).setView(box)
                        .setPositiveButton(Theme.s("Sign in", "دخول"), new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int x) { h.proceed(user.getText().toString(), pass.getText().toString()); }
                        })
                        .setNegativeButton(Theme.s("Cancel", "إلغاء"), new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int x) { h.cancel(); }
                        })
                        .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                            @Override public void onCancel(android.content.DialogInterface d) { h.cancel(); }
                        }).show();
            }
        });

        w.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                t.progress = p;
                if (p < 100) t.loading = true;
                if (t == cur) updateProgress();
            }

            @Override
            public void onReceivedTitle(WebView v, String title) {
                if (title == null || title.isEmpty()) return;
                t.title = title;
                if (!t.incognito) store.updateHistoryTitle(v.getUrl(), title);
            }

            @Override
            public void onReceivedIcon(WebView v, Bitmap icon) {
                t.icon = icon;
                if (!t.incognito && v.getUrl() != null) store.saveIcon(v.getUrl(), icon);
            }

            @Override
            public boolean onCreateWindow(WebView v, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                if (!isUserGesture && store.getBool("block_popups", true)) return false;
                Tab nt = new Tab(t.incognito);
                nt.parent = t;
                nt.desktop = t.desktop;
                nt.ntp = false;
                nt.web = createWebView(nt);
                tabs.add(nt);
                WebView.WebViewTransport tr = (WebView.WebViewTransport) resultMsg.obj;
                tr.setWebView(nt.web);
                resultMsg.sendToTarget();
                switchTo(nt);
                return true;
            }

            @Override
            public void onCloseWindow(WebView v) {
                closeTab(t);
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try {
                    act.startActivityForResult(params.createIntent(), REQ_FILE);
                } catch (Throwable e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }

            @Override
            public boolean onJsAlert(WebView v, String url, String msg, final JsResult r) {
                dlg().setTitle(Store.hostOf(url)).setMessage(msg)
                        .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int x) { r.confirm(); }
                        })
                        .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                            @Override public void onCancel(android.content.DialogInterface d) { r.cancel(); }
                        }).show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView v, String url, String msg, final JsResult r) {
                dlg().setTitle(Store.hostOf(url)).setMessage(msg)
                        .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int x) { r.confirm(); }
                        })
                        .setNegativeButton(Theme.s("Cancel", "إلغاء"), new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int x) { r.cancel(); }
                        })
                        .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                            @Override public void onCancel(android.content.DialogInterface d) { r.cancel(); }
                        }).show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView v, String url, String msg, String def, final JsPromptResult r) {
                final EditText input = new EditText(act);
                input.setSingleLine(true);
                if (def != null) input.setText(def);
                LinearLayout box = new LinearLayout(act);
                box.setPadding(Theme.dp(24), Theme.dp(8), Theme.dp(24), 0);
                box.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                dlg().setTitle(Store.hostOf(url)).setMessage(msg).setView(box)
                        .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int x) { r.confirm(input.getText().toString()); }
                        })
                        .setNegativeButton(Theme.s("Cancel", "إلغاء"), new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int x) { r.cancel(); }
                        })
                        .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                            @Override public void onCancel(android.content.DialogInterface d) { r.cancel(); }
                        }).show();
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest req) {
                final List<String> res = new ArrayList<>();
                final List<String> need = new ArrayList<>();
                boolean cam = false, mic = false;
                for (String r : req.getResources()) {
                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) { res.add(r); need.add(Manifest.permission.CAMERA); cam = true; }
                    else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) { res.add(r); need.add(Manifest.permission.RECORD_AUDIO); mic = true; }
                }
                if (res.isEmpty()) { req.deny(); return; }
                if ((cam && !store.getBool("perm_camera", true)) || (mic && !store.getBool("perm_mic", true))) { req.deny(); return; }
                String what = cam && mic ? Theme.s("camera and microphone", "الكاميرا والميكروفون")
                        : cam ? Theme.s("camera", "الكاميرا") : Theme.s("microphone", "الميكروفون");
                dlg().setTitle(Store.hostOf(req.getOrigin().toString()))
                        .setMessage(Theme.s("Allow this site to use your ", "السماح لهذا الموقع باستخدام ") + what + "؟")
                        .setPositiveButton(Theme.s("Allow", "سماح"), new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int x) {
                                List<String> missing = new ArrayList<>();
                                for (String p : need) if (act.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) missing.add(p);
                                String[] arr = res.toArray(new String[0]);
                                if (missing.isEmpty()) req.grant(arr);
                                else {
                                    pendingPerm = req;
                                    pendingPermRes = arr;
                                    act.requestPermissions(missing.toArray(new String[0]), REQ_PERM);
                                }
                            }
                        })
                        .setNegativeButton(Theme.s("Block", "حظر"), new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int x) { req.deny(); }
                        })
                        .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                            @Override public void onCancel(android.content.DialogInterface d) { req.deny(); }
                        }).show();
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback cb) {
                if (!store.getBool("perm_location", true)) { cb.invoke(origin, false, false); return; }
                dlg().setTitle(Store.hostOf(origin))
                        .setMessage(Theme.s("Allow this site to know your location?", "السماح لهذا الموقع بمعرفة موقعك؟"))
                        .setPositiveButton(Theme.s("Allow", "سماح"), new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int x) {
                                if (act.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    cb.invoke(origin, true, false);
                                } else {
                                    geoCb = cb;
                                    geoOrigin = origin;
                                    act.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_GEO);
                                }
                            }
                        })
                        .setNegativeButton(Theme.s("Block", "حظر"), new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int x) { cb.invoke(origin, false, false); }
                        })
                        .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                            @Override public void onCancel(android.content.DialogInterface d) { cb.invoke(origin, false, false); }
                        }).show();
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                if (customView != null) { cb.onCustomViewHidden(); return; }
                customView = view;
                customCb = cb;
                fullscreenHolder.addView(view, U.fill());
                fullscreenHolder.setVisibility(View.VISIBLE);
                act.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }
        });

        w.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return w;
    }

    private void hideCustomView() {
        if (customView == null) return;
        fullscreenHolder.removeAllViews();
        fullscreenHolder.setVisibility(View.GONE);
        customView = null;
        try { if (customCb != null) customCb.onCustomViewHidden(); } catch (Throwable ignored) { }
        customCb = null;
        applySystemUi();
    }

    private void openApp(WebView v, String u) {
        try {
            Intent i;
            if (u.startsWith("intent:")) {
                i = Intent.parseUri(u, Intent.URI_INTENT_SCHEME);
                i.addCategory(Intent.CATEGORY_BROWSABLE);
                i.setComponent(null);
                i.setSelector(null);
                try {
                    act.startActivity(i);
                } catch (Throwable e) {
                    String fb = i.getStringExtra("browser_fallback_url");
                    if (fb != null) v.loadUrl(fb);
                }
                return;
            }
            i = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
            i.addCategory(Intent.CATEGORY_BROWSABLE);
            act.startActivity(i);
        } catch (Throwable e) {
            toast(Theme.s("No app can open this link", "لا يوجد تطبيق لفتح هذا الرابط"));
        }
    }

    private void showLinkMenu(final Tab t, final String url, final String img) {
        final List<String> names = new ArrayList<>();
        final List<Runnable> acts = new ArrayList<>();
        if (url != null) {
            names.add(Theme.s("Open in new tab", "فتح في تبويب جديد"));
            acts.add(new Runnable() { @Override public void run() { addTab(t.incognito, url, false); } });
            if (!t.incognito) {
                names.add(Theme.s("Open in Incognito tab", "فتح في تبويب متخفٍّ"));
                acts.add(new Runnable() { @Override public void run() { addTab(true, url, false); } });
            }
            names.add(Theme.s("Copy link address", "نسخ عنوان الرابط"));
            acts.add(new Runnable() { @Override public void run() { copyText(url); } });
            names.add(Theme.s("Share link", "مشاركة الرابط"));
            acts.add(new Runnable() { @Override public void run() { share(url, null); } });
        }
        if (img != null) {
            names.add(Theme.s("Open image in new tab", "فتح الصورة في تبويب جديد"));
            acts.add(new Runnable() { @Override public void run() { addTab(t.incognito, img, false); } });
            names.add(Theme.s("Download image", "تنزيل الصورة"));
            acts.add(new Runnable() { @Override public void run() { download(img, t.desktop ? desktopUA : mobileUA, null, null); } });
            names.add(Theme.s("Copy image address", "نسخ عنوان الصورة"));
            acts.add(new Runnable() { @Override public void run() { copyText(img); } });
        }
        if (names.isEmpty()) return;
        dlg().setTitle(Store.hostOf(url != null ? url : img))
                .setItems(names.toArray(new String[0]), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        acts.get(which).run();
                    }
                }).show();
    }

    private String errorHtml(String url, String desc) {
        String bg = Theme.dark ? "#1a1412" : "#fff8f6";
        String fg = Theme.dark ? "#f1e0db" : "#231917";
        String dim = Theme.dark ? "#b0a19c" : "#73655f";
        String ac = Theme.dark ? "#ffb59e" : "#b3452b";
        String onAc = Theme.dark ? "#3b0a00" : "#ffffff";
        return "<!doctype html><html dir='" + (Theme.ar ? "rtl" : "ltr") + "'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{margin:0;padding:40px 28px;background:" + bg + ";color:" + fg + ";font-family:sans-serif}"
                + "h1{font-size:22px;margin:0 0 12px}p{color:" + dim + ";line-height:1.7;word-break:break-word}"
                + "button{margin-top:18px;background:" + ac + ";color:" + onAc + ";border:0;border-radius:20px;padding:12px 28px;font-size:16px}"
                + "</style></head><body><h1>" + esc(Theme.s("This page can't be reached", "تعذّر الوصول إلى هذه الصفحة")) + "</h1>"
                + "<p>" + esc(Store.hostOf(url)) + "</p><p>" + esc(desc) + "</p>"
                + "<button onclick='location.reload()'>" + esc(Theme.s("Reload", "إعادة المحاولة")) + "</button></body></html>";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // =====================================================================
    //  دورة الحياة
    // =====================================================================
    void onPause() {
        saveSession();
        CookieManager.getInstance().flush();
        if (cur != null && cur.web != null) cur.web.onPause();
    }

    void onResume() {
        if (cur != null && cur.web != null) cur.web.onResume();
    }

    void onDestroy() {
        try { if (dlReceiver != null) act.unregisterReceiver(dlReceiver); } catch (Throwable ignored) { }
    }
}
