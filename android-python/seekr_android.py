"""تشغيل Seekr داخل تطبيق الأندرويد (Chaquopy).

تستدعيه MainActivity: يشغّل خادم WSGI على 127.0.0.1 (منفذ 18888 أو أي منفذ حر)، ويحميه
برمز عشوائي يُولَّد عند كل تشغيل للتطبيق: أول طلب يجب أن يكون /__auth?t=<الرمز> فيضع كوكي
HttpOnly، وأي طلب بلا الكوكي (مثلًا من تطبيق آخر على الهاتف) يُرفَض بـ 403.
"""
import hmac
import json
import os
import re
import secrets
import threading
from http.cookies import SimpleCookie

PREFERRED_PORT = 18888
COOKIE = "sxng_app"

_lock = threading.Lock()
_info = None
_TOKEN = secrets.token_urlsafe(24)

_SETTINGS_TEMPLATE = """\
# إعدادات Seekr داخل التطبيق. يمكنك تعديل هذا الملف؛ لن يُستبدل بعد إنشائه.
# المرجع: https://docs.searxng.org/admin/settings/index.html
use_default_settings: true

general:
  debug: false
  instance_name: "Seekr"
  enable_metrics: false

server:
  secret_key: "{secret}"
  bind_address: "127.0.0.1"
  port: {port}
  limiter: false
  public_instance: false
  image_proxy: true

ui:
  default_locale: "ar"

search:
  autocomplete: "duckduckgo"
  formats:
    - html
    - json

outgoing:
  request_timeout: 6.0
  max_request_timeout: 20.0
  pool_connections: 30
  enable_http2: false
  retries: 0
"""


def _prepare_settings(data_dir: str, port: int) -> str:
    os.makedirs(data_dir, exist_ok=True)
    path = os.path.join(data_dir, "settings.yml")
    if not os.path.exists(path):
        secret_file = os.path.join(data_dir, ".secret")
        if os.path.exists(secret_file):
            secret = open(secret_file, encoding="utf-8").read().strip()
        else:
            secret = secrets.token_hex(32)
            with open(secret_file, "w", encoding="utf-8") as fh:
                fh.write(secret)
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(_SETTINGS_TEMPLATE.format(secret=secret, port=port))
    else:
        # ملف إعدادات قديم (أُنشئ قبل إضافة الواجهة العربية): نضيف كتلة ui دون المساس بتعديلات المستخدم
        with open(path, encoding="utf-8") as fh:
            content = fh.read()
        migrated = re.sub(r'^(\s*instance_name:\s*)"SearXNG"\s*$', r'\1"Seekr"', content, flags=re.M)
        if migrated != content:
            content = migrated
            with open(path, "w", encoding="utf-8") as fh:
                fh.write(content)
        if not re.search(r"^ui:", content, re.M):
            with open(path, "a", encoding="utf-8") as fh:
                fh.write('\n# واجهة عربية افتراضيًا (غيّرها إلى "" لاتباع لغة الجهاز)\nui:\n  default_locale: "ar"\n')
    return path


def _engines_json() -> str:
    """قائمة المحركات (للإعدادات الأصلية في التطبيق): [{key, name, cat, on}] — on = مفعّل افتراضيًا."""
    from searx import settings  # pylint: disable=import-outside-toplevel
    from searx.engines import DEFAULT_CATEGORY, engines  # pylint: disable=import-outside-toplevel

    allowed = list(settings["categories_as_tabs"].keys()) + [DEFAULT_CATEGORY]
    out = []
    for eng in engines.values():
        for cat in eng.categories:
            if cat in allowed:
                out.append({"key": f"{eng.name}__{cat}", "name": eng.name, "cat": cat, "on": not eng.disabled})
    out.sort(key=lambda e: (e["cat"], e["name"].lower()))
    return json.dumps(out, ensure_ascii=False)


def _guard(app):
    """WSGI: يتحقق من الكوكي، ويضيف X-Real-IP لتفادي رسائل خطأ كاشف الروبوتات."""

    def inner(environ, start_response):
        path = environ.get("PATH_INFO", "")
        if path == "/__auth":
            query = environ.get("QUERY_STRING", "")
            given = ""
            for part in query.split("&"):
                if part.startswith("t="):
                    given = part[2:]
            if hmac.compare_digest(given, _TOKEN):
                # SameSite=Lax (وليس Strict): صفحة التحميل تُخدَّم من https://localhost داخل Capacitor، والانتقال
                # إلى http://127.0.0.1 يُعدّ طلبًا عابرًا للمواقع؛ كوكي Strict لا يُرسَل مع إعادة التوجيه إلى "/"
                # فيظهر Forbidden. Lax يُرسَل مع التنقل العلوي GET فقط، والرمز العشوائي يبقى الحماية الفعلية.
                start_response("302 Found", [
                    ("Location", "/"),
                    ("Set-Cookie", f"{COOKIE}={_TOKEN}; Path=/; HttpOnly; SameSite=Lax"),
                    ("Content-Length", "0"),
                ])
                return [b""]
            start_response("403 Forbidden", [("Content-Type", "text/plain"), ("Content-Length", "9")])
            return [b"Forbidden"]

        jar = SimpleCookie()
        try:
            jar.load(environ.get("HTTP_COOKIE", ""))
        except Exception:  # pylint: disable=broad-except
            pass
        morsel = jar.get(COOKIE)
        if morsel is None or not hmac.compare_digest(morsel.value, _TOKEN):
            start_response("403 Forbidden", [("Content-Type", "text/plain"), ("Content-Length", "9")])
            return [b"Forbidden"]

        if path == "/__api/engines":
            body = _engines_json().encode("utf-8")
            start_response("200 OK", [("Content-Type", "application/json; charset=utf-8"), ("Content-Length", str(len(body)))])
            return [body]

        environ["HTTP_X_REAL_IP"] = environ.get("REMOTE_ADDR") or "127.0.0.1"
        return app(environ, start_response)

    return inner


def start(data_dir: str) -> str:
    """يبدأ Seekr (مرة واحدة لكل عملية). يعيد JSON: {"port": N, "token": "..."}؛ يرفع استثناء عند الفشل."""
    global _info  # pylint: disable=global-statement
    with _lock:
        if _info is not None:
            return _info

        from werkzeug.serving import make_server  # pylint: disable=import-outside-toplevel

        # نختار المنفذ قبل كتابة الإعدادات (يُسجَّل في settings.yml عند أول تشغيل فقط)
        settings_path = _prepare_settings(data_dir, PREFERRED_PORT)
        os.environ["SEARXNG_SETTINGS_PATH"] = settings_path
        os.environ["SEARXNG_DISABLE_ETC_SETTINGS"] = "1"

        import searx.webapp  # pylint: disable=import-outside-toplevel

        wsgi = _guard(searx.webapp.app)
        try:
            server = make_server("127.0.0.1", PREFERRED_PORT, wsgi, threaded=True)
        except OSError:
            server = make_server("127.0.0.1", 0, wsgi, threaded=True)
        threading.Thread(target=server.serve_forever, daemon=True, name="seekr-http").start()
        _info = json.dumps({"port": server.server_address[1], "token": _TOKEN})
        return _info
