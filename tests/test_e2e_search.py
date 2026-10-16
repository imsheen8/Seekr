"""اختبار شامل: Seekr الحقيقي + بديل curl_cffi يبحث في محرك وهمي محلي (بلا إنترنت).
تشغيل: PYTHONPATH=android-python:<stubs-إن-لزم> python3 tests/test_e2e_search.py"""
import http.server, json, os, sys, tempfile, threading

class H(http.server.BaseHTTPRequestHandler):
    def log_message(self, *a): pass
    def do_GET(self):
        body = json.dumps({"results": [
            {"url": "https://example.org/a", "title": "Alpha result", "content": "first hit"},
            {"url": "https://example.org/b", "title": "Beta result", "content": "second hit"}]}).encode()
        self.send_response(200); self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body))); self.end_headers(); self.wfile.write(body)

srv = http.server.ThreadingHTTPServer(("127.0.0.1", 0), H)
threading.Thread(target=srv.serve_forever, daemon=True).start()
port = srv.server_address[1]

d = tempfile.mkdtemp()
open(os.path.join(d, "settings.yml"), "w").write(f"""
use_default_settings:
  engines:
    keep_only: []
server: {{secret_key: "e2e-secret", limiter: false}}
search: {{formats: [html, json]}}
engines:
  - name: mock
    engine: json_engine
    shortcut: mk
    categories: general
    search_url: http://127.0.0.1:{port}/search?q={{query}}
    results_query: results
    url_query: url
    title_query: title
    content_query: content
    enable_http: true
    disabled: false
""")
os.environ["SEARXNG_SETTINGS_PATH"] = os.path.join(d, "settings.yml")

import searx.webapp as w
c = w.app.test_client()
r = c.get("/"); assert r.status_code == 200, r.status_code
r = c.get("/search", query_string={"q": "hello", "format": "json"})
assert r.status_code == 200, (r.status_code, r.data[:300])
data = r.get_json()
titles = [x["title"] for x in data["results"]]
print("titles:", titles, "unresponsive:", data.get("unresponsive_engines"))
assert "Alpha result" in titles and "Beta result" in titles
r = c.get("/search", query_string={"q": "hello"}); assert r.status_code == 200 and b"Alpha result" in r.data
print("E2E OK")
