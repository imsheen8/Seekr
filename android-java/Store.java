package __APP_ID__;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** تخزين السجل والإشارات المرجعية والاختصارات وسجل التنزيلات والإعدادات (ملفات JSON داخل بيانات التطبيق). */
final class Store {
    static final class Entry {
        String title;
        String url;
        long ts;
        long id; // للتنزيلات: معرّف DownloadManager
        String mime;

        Entry(String title, String url, long ts) {
            this.title = title == null ? "" : title;
            this.url = url;
            this.ts = ts;
        }
    }

    private final Context ctx;
    private final File dir;
    private final SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<Entry> history = new ArrayList<>();
    private final List<Entry> bookmarks = new ArrayList<>();
    private final List<Entry> shortcuts = new ArrayList<>();
    private final List<Entry> downloads = new ArrayList<>();
    private final Map<String, Bitmap> icons = new HashMap<>();

    Store(Context c) {
        ctx = c.getApplicationContext();
        dir = new File(ctx.getFilesDir(), "browser");
        dir.mkdirs();
        new File(dir, "icons").mkdirs();
        prefs = ctx.getSharedPreferences("br", Context.MODE_PRIVATE);
        load("history.json", history);
        load("bookmarks.json", bookmarks);
        load("shortcuts.json", shortcuts);
        load("downloads.json", downloads);
    }

    // ---------- إعدادات ----------
    boolean getBool(String k, boolean def) { return prefs.getBoolean(k, def); }
    void setBool(String k, boolean v) { prefs.edit().putBoolean(k, v).apply(); }
    String getStr(String k, String def) { return prefs.getString(k, def); }
    void setStr(String k, String v) { prefs.edit().putString(k, v).apply(); }
    int getInt(String k, int def) { return prefs.getInt(k, def); }
    void setInt(String k, int v) { prefs.edit().putInt(k, v).apply(); }

    // ---------- JSON ----------
    private void load(String name, List<Entry> out) {
        try {
            File f = new File(dir, name);
            if (!f.exists()) return;
            byte[] buf = new byte[(int) f.length()];
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            int off = 0;
            while (off < buf.length) {
                int n = in.read(buf, off, buf.length - off);
                if (n < 0) break;
                off += n;
            }
            in.close();
            JSONArray a = new JSONArray(new String(buf, "UTF-8"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Entry e = new Entry(o.optString("t"), o.optString("u"), o.optLong("ts"));
                e.id = o.optLong("id", -1);
                e.mime = o.optString("m", null);
                out.add(e);
            }
        } catch (Throwable ignored) {
            // ملف تالف: نبدأ من الصفر
        }
    }

    private void save(final String name, List<Entry> list) {
        final JSONArray a = new JSONArray();
        for (Entry e : list) {
            try {
                JSONObject o = new JSONObject();
                o.put("t", e.title);
                o.put("u", e.url);
                o.put("ts", e.ts);
                if (e.id != 0) o.put("id", e.id);
                if (e.mime != null) o.put("m", e.mime);
                a.put(o);
            } catch (Throwable ignored) {
                // تجاهل
            }
        }
        final String data = a.toString();
        io.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File tmp = new File(dir, name + ".tmp");
                    FileOutputStream out = new FileOutputStream(tmp);
                    out.write(data.getBytes("UTF-8"));
                    out.close();
                    File dst = new File(dir, name);
                    if (!tmp.renameTo(dst)) {
                        dst.delete();
                        tmp.renameTo(dst);
                    }
                } catch (Throwable ignored) {
                    // تجاهل
                }
            }
        });
    }

    // ---------- السجل ----------
    synchronized void addHistory(String url, String title) {
        if (url == null || url.isEmpty() || isHome(url)) return;
        long now = System.currentTimeMillis();
        if (!history.isEmpty()) {
            Entry last = history.get(0);
            if (last.url.equals(url)) {
                if (title != null && !title.isEmpty()) last.title = title;
                last.ts = now;
                save("history.json", history);
                return;
            }
        }
        history.add(0, new Entry(title, url, now));
        while (history.size() > 4000) history.remove(history.size() - 1);
        save("history.json", history);
    }

    synchronized void updateHistoryTitle(String url, String title) {
        if (title == null || title.isEmpty()) return;
        for (int i = 0; i < Math.min(history.size(), 5); i++) {
            Entry e = history.get(i);
            if (e.url.equals(url)) {
                if (!title.equals(e.title)) {
                    e.title = title;
                    save("history.json", history);
                }
                return;
            }
        }
    }

    private static boolean isHome(String u) {
        return u.matches("^https?://(127\\.0\\.0\\.1|localhost)(:\\d+)?/?(#.*)?$");
    }

    synchronized List<Entry> history() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : history) if (!isHome(e.url)) out.add(e);
        return out;
    }

    synchronized void deleteHistory(Entry e) {
        history.remove(e);
        save("history.json", history);
    }

    synchronized void clearHistory(long sinceMillis) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).ts >= sinceMillis) history.remove(i);
        }
        save("history.json", history);
    }

    // ---------- الإشارات المرجعية ----------
    synchronized boolean isBookmarked(String url) {
        for (Entry e : bookmarks) if (e.url.equals(url)) return true;
        return false;
    }

    /** يعيد true إن أُضيفت، false إن أُزيلت. */
    synchronized boolean toggleBookmark(String url, String title) {
        for (int i = 0; i < bookmarks.size(); i++) {
            if (bookmarks.get(i).url.equals(url)) {
                bookmarks.remove(i);
                save("bookmarks.json", bookmarks);
                return false;
            }
        }
        bookmarks.add(0, new Entry(title == null || title.isEmpty() ? url : title, url, System.currentTimeMillis()));
        save("bookmarks.json", bookmarks);
        return true;
    }

    synchronized List<Entry> bookmarks() { return new ArrayList<>(bookmarks); }

    synchronized void removeBookmark(Entry e) {
        bookmarks.remove(e);
        save("bookmarks.json", bookmarks);
    }

    // ---------- الاختصارات ----------
    synchronized List<Entry> shortcuts() { return new ArrayList<>(shortcuts); }

    synchronized void addShortcut(String title, String url) {
        shortcuts.add(new Entry(title, url, System.currentTimeMillis()));
        save("shortcuts.json", shortcuts);
    }

    synchronized void removeShortcut(Entry e) {
        shortcuts.remove(e);
        save("shortcuts.json", shortcuts);
    }

    /** أكثر المواقع زيارة (حسب النطاق) من السجل، يستثني خادم Seekr المحلي. */
    synchronized List<Entry> topSites(int n) {
        Map<String, int[]> count = new LinkedHashMap<>();
        Map<String, Entry> first = new HashMap<>();
        for (Entry e : history) {
            String h = originOf(e.url);
            if (h == null || h.contains("127.0.0.1")) continue;
            int[] c = count.get(h);
            if (c == null) {
                c = new int[1];
                count.put(h, c);
                first.put(h, e);
            }
            c[0]++;
        }
        List<Map.Entry<String, int[]>> items = new ArrayList<>(count.entrySet());
        java.util.Collections.sort(items, new java.util.Comparator<Map.Entry<String, int[]>>() {
            @Override
            public int compare(Map.Entry<String, int[]> a, Map.Entry<String, int[]> b) {
                return b.getValue()[0] - a.getValue()[0];
            }
        });
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<String, int[]> it : items) {
            if (out.size() >= n) break;
            Entry src = first.get(it.getKey());
            String title = hostOf(it.getKey());
            out.add(new Entry(title, it.getKey(), src.ts));
        }
        return out;
    }

    static String originOf(String url) {
        try {
            URI u = new URI(url);
            if (u.getScheme() == null || u.getHost() == null) return null;
            if (!u.getScheme().startsWith("http")) return null;
            return u.getScheme() + "://" + u.getHost() + (u.getPort() > 0 ? ":" + u.getPort() : "") + "/";
        } catch (Throwable t) {
            return null;
        }
    }

    static String hostOf(String url) {
        try {
            String h = new URI(url).getHost();
            if (h == null) return url;
            return h.startsWith("www.") ? h.substring(4) : h;
        } catch (Throwable t) {
            return url;
        }
    }

    // ---------- التنزيلات ----------
    synchronized void addDownload(long id, String name, String url, String mime) {
        Entry e = new Entry(name, url, System.currentTimeMillis());
        e.id = id;
        e.mime = mime;
        downloads.add(0, e);
        save("downloads.json", downloads);
    }

    synchronized List<Entry> downloads() { return new ArrayList<>(downloads); }

    synchronized void removeDownload(Entry e) {
        downloads.remove(e);
        save("downloads.json", downloads);
    }

    synchronized void clearDownloads() {
        downloads.clear();
        save("downloads.json", downloads);
    }

    // ---------- أيقونات المواقع ----------
    synchronized void saveIcon(String url, Bitmap bmp) {
        final String key = hostOf(url);
        if (bmp == null || key == null || icons.containsKey(key)) return;
        icons.put(key, bmp);
        final Bitmap copy = bmp;
        io.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream out = new FileOutputStream(new File(new File(dir, "icons"), safe(key) + ".png"));
                    copy.compress(Bitmap.CompressFormat.PNG, 90, out);
                    out.close();
                } catch (Throwable ignored) {
                    // تجاهل
                }
            }
        });
    }

    synchronized Bitmap icon(String url) {
        String key = hostOf(url);
        if (key == null) return null;
        Bitmap b = icons.get(key);
        if (b != null) return b;
        File f = new File(new File(dir, "icons"), safe(key) + ".png");
        if (f.exists()) {
            b = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (b != null) icons.put(key, b);
        }
        return b;
    }

    synchronized void clearIcons() {
        icons.clear();
        File[] fs = new File(dir, "icons").listFiles();
        if (fs != null) for (File f : fs) f.delete();
    }

    private static String safe(String s) {
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    // ---------- جلسة التبويبات ----------
    void saveSession(String json) {
        prefs.edit().putString("session", json).apply();
    }

    String loadSession() {
        return prefs.getString("session", null);
    }
}
