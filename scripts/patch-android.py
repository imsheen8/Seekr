#!/usr/bin/env python3
"""يُشغَّل بعد `npx cap add android`: يدمج Seekr وبايثون (Chaquopy) في مشروع الأندرويد."""
import json
import os
import pathlib
import re
import shutil

root = pathlib.Path(__file__).resolve().parent.parent
cfg = json.load(open(root / 'capacitor.config.json', encoding='utf-8'))
app_id = cfg['appId']
app = pathlib.Path('android/app')
main = app / 'src/main'

# 1) MainActivity: يبدأ بايثون في خيط خلفي ويعرض الحالة لصفحة التحميل عبر SXApp.status()
java_dir = main / 'java' / pathlib.Path(*app_id.split('.'))
java_dir.mkdir(parents=True, exist_ok=True)
(java_dir / 'MainActivity.java').write_text('''package %s;

import android.content.Context;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
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
        startPython(this);
        WebView wv = getBridge().getWebView();
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
                    return "{\\"state\\":\\"starting\\"}";
                }
            }
        }, "SXApp");
    }
}
''' % app_id, encoding='utf-8')

# 2) نسخ مصادر بايثون إلى Chaquopy: app/src/main/python
py_dst = main / 'python'
if py_dst.exists():
    shutil.rmtree(py_dst)
py_dst.mkdir(parents=True)
ignore = shutil.ignore_patterns('__pycache__', '*.pyc', '*.po', '*.pot')
shutil.copytree(root / 'searx', py_dst / 'searx', ignore=ignore)
for item in (root / 'android-python').iterdir():
    if item.name == '__pycache__':
        continue
    if item.is_dir():
        shutil.copytree(item, py_dst / item.name, ignore=ignore)
    else:
        shutil.copy2(item, py_dst / item.name)

# 3) الصلاحيات + السماح بـ http://127.0.0.1 (محليًا فقط)
manifest = main / 'AndroidManifest.xml'
m = manifest.read_text(encoding='utf-8')
if 'android.permission.INTERNET' not in m:
    m = m.replace('</manifest>', '    <uses-permission android:name="android.permission.INTERNET" />\n</manifest>')
if 'usesCleartextTraffic' not in m:
    m = m.replace('<application', '<application android:usesCleartextTraffic="true" android:largeHeap="true"', 1)
manifest.write_text(m, encoding='utf-8')

# 3.5) عجلات lxml/pyyaml/markupsafe لدى Chaquopy موسومة android_24 → يجب أن يكون minSdkVersion >= 24
# (Capacitor 6 يضع 22 افتراضيًا في android/variables.gradle، فيرفض pip العجلات ويفشل بـ "from versions: none")
variables = pathlib.Path('android/variables.gradle')
if variables.exists():
    v = variables.read_text(encoding='utf-8')
    v = re.sub(r'minSdkVersion\s*=\s*\d+', 'minSdkVersion = 24', v)
    variables.write_text(v, encoding='utf-8')
else:
    raise SystemExit('android/variables.gradle غير موجود: تعذّر رفع minSdkVersion إلى 24')

# 4) رقم الإصدار + Chaquopy + حزم pip
gradle = app / 'build.gradle'
g = gradle.read_text(encoding='utf-8')
code = os.environ.get('VERSION_CODE', '1')
name = os.environ.get('VERSION_NAME', '1.0.0')
g = re.sub(r'versionCode\s+\d+', 'versionCode %s' % code, g, count=1)
g = re.sub(r'versionName\s+"[^"]*"', 'versionName "%s"' % name, g, count=1)

reqs = [ln.strip() for ln in open(root / 'requirements-android.txt', encoding='utf-8')
        if ln.strip() and not ln.lstrip().startswith('#')]
pip_block = '\n'.join('                install "%s"' % r for r in reqs)

if os.environ.get('SIGNING') == '1' and 'signingConfigs' not in g:
    signing = '''    signingConfigs {
        release {
            storeFile file(System.getenv("KEYSTORE_PATH"))
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {'''
    g = g.replace('    buildTypes {', signing, 1)
    g = re.sub(r'(buildTypes\s*\{\s*release\s*\{)', r'\1\n            signingConfig signingConfigs.release', g, count=1)

if 'com.chaquo.python' not in g:
    g = re.sub(r"(apply plugin: ['\"]com\.android\.application['\"])", r"\1\napply plugin: 'com.chaquo.python'", g, count=1)
    g = re.sub(
        r'(defaultConfig\s*\{)',
        r'''\1
        ndk { abiFilters "arm64-v8a", "armeabi-v7a" }
        python {
            version "3.11"
            // Seekr يقرأ ملفاته من نظام الملفات (iterdir/open: answerers, engines, templates, static, data...)
            // أما Chaquopy فيحمّل الحزم افتراضيًا من داخل الـ APK بلا مسارات حقيقية، فنطلب استخراج الحزمة كاملة.
            extractPackages "searx"
            pip {
                // الحزم ذات الكود الأصلي (lxml/pyyaml) لا تُبنى من المصدر على أندرويد؛
                // نجبر pip على استخدام عجلات (wheels) Chaquopy الجاهزة فقط بدل أحدث sdist من PyPI.
                options "--only-binary=lxml,pyyaml"
%s
            }
        }''' % pip_block,
        g, count=1)
    if 'com.chaquo.python' not in g or 'pip {' not in g:
        raise SystemExit('تعذّر تعديل android/app/build.gradle لإضافة Chaquopy')
gradle.write_text(g, encoding='utf-8')

root_gradle = pathlib.Path('android/build.gradle')
rg = root_gradle.read_text(encoding='utf-8')
if 'com.chaquo.python' not in rg:
    rg = re.sub(r'(buildscript\s*\{\s*repositories\s*\{)', r'\1\n        maven { url "https://chaquo.com/maven" }', rg, count=1)
    rg = re.sub(r"(dependencies\s*\{\s*classpath ['\"]com\.android\.tools\.build:gradle:[^'\"]+['\"])",
                r"\1\n        classpath 'com.chaquo.python:gradle:15.0.1'", rg, count=1)
    root_gradle.write_text(rg, encoding='utf-8')
    if 'com.chaquo.python:gradle' not in rg or 'chaquo.com/maven' not in rg:
        raise SystemExit('تعذّر تعديل android/build.gradle لإضافة Chaquopy')

print('patched android project:', app_id, 'versionCode', code, 'versionName', name,
      'signing', os.environ.get('SIGNING') == '1', 'pip packages', len(reqs))
