"""يشغّل seekr_android.start() كما تفعل MainActivity ويتحقق من حماية الرمز وتحميل الواجهة."""
import json, os, sys, tempfile
import requests
import seekr_android as sa

info = json.loads(sa.start(tempfile.mkdtemp()))
base = f"http://127.0.0.1:{info['port']}"
assert requests.get(base + "/", timeout=10).status_code == 403
assert requests.get(base + "/__auth?t=wrong", timeout=10, allow_redirects=False).status_code == 403
s = requests.Session()
r = s.get(base + "/__auth?t=" + info["token"], timeout=30)
assert r.status_code == 200 and "Seekr" in r.text, r.status_code
assert s.get(base + "/static/themes/simple/sxng-ltr.min.css", timeout=10).status_code == 200
assert requests.get(base + "/static/themes/simple/sxng-ltr.min.css", timeout=10).status_code == 403
assert s.get(base + "/healthz", timeout=10).text.strip() == "OK"
r = s.get(base + "/preferences", timeout=30); assert r.status_code == 200
print("android entry OK on port", info["port"])
