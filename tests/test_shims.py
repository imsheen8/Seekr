"""اختبارات بدائل curl_cffi و msgspec (تعمل على الحاسوب: python3 tests/test_shims.py)."""
import asyncio, http.server, json, os, sys, threading
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "android-python"))

from curl_cffi import AsyncSession, CurlMime, CurlOpt
from curl_cffi.requests import Response, exceptions as ex


class H(http.server.BaseHTTPRequestHandler):
    def log_message(self, *a): pass
    def _send(self, code, body=b"", headers=None):
        self.send_response(code)
        for k, v in (headers or {}).items(): self.send_header(k, v)
        self.send_header("Content-Length", str(len(body))); self.end_headers(); self.wfile.write(body)
    def do_GET(self):
        if self.path == "/json": self._send(200, b'{"a": 1}', {"Content-Type": "application/json"})
        elif self.path == "/r1": self._send(302, b"", {"Location": "/r2", "Set-Cookie": "k=v"})
        elif self.path == "/r2": self._send(200, b"ok:" + (self.headers.get("Cookie") or "").encode())
        elif self.path == "/loop": self._send(302, b"", {"Location": "/loop"})
        elif self.path == "/ua": self._send(200, (self.headers.get("User-Agent") or "").encode())
        elif self.path == "/big": self._send(200, b"x" * 100000, {"Content-Type": "application/octet-stream"})
        elif self.path == "/404": self._send(404, b"nope")
        elif self.path == "/latin": self._send(200, "é".encode("latin-1"), {"Content-Type": "text/html; charset=iso-8859-1"})
        else: self._send(200, b"hi")
    def do_POST(self):
        n = int(self.headers.get("Content-Length", 0)); b = self.rfile.read(n)
        self._send(200, json.dumps({"ct": self.headers.get("Content-Type"), "body": b.decode("latin-1")}).encode())


srv = http.server.ThreadingHTTPServer(("127.0.0.1", 0), H)
threading.Thread(target=srv.serve_forever, daemon=True).start()
base = f"http://127.0.0.1:{srv.server_address[1]}"


async def main():
    c = AsyncSession(impersonate="chrome", default_headers=True, discard_cookies=True, response_class=Response)
    r = await c.request("GET", base + "/json"); assert r.ok and r.json() == {"a": 1}, r
    r = await c.request("GET", base + "/r1", allow_redirects=True)
    assert r.text == "ok:k=v" and len(r.history) == 1 and r.url.endswith("/r2"), (r.text, r.history)
    r = await c.request("GET", base + "/r1", allow_redirects=False); assert r.status_code == 302 and r.cookies["k"] == "v"
    try: await c.request("GET", base + "/loop", allow_redirects=True, max_redirects=3); assert False
    except ex.TooManyRedirects: pass
    r = await c.request("GET", base + "/ua"); assert "Chrome" in r.text
    r = await c.request("GET", base + "/ua", headers={"User-Agent": "X"}); assert r.text == "X"
    r = await c.request("POST", base + "/p", data={"q": "1"}); assert "application/x-www-form-urlencoded" in r.json()["ct"]
    r = await c.request("POST", base + "/p", json={"q": 1}); assert r.json()["body"] == '{"q": 1}'
    m = CurlMime(); m.addpart(name="altcha", data=b"abc")
    r = await c.request("POST", base + "/p", multipart=m); assert "multipart/form-data" in r.json()["ct"] and "abc" in r.json()["body"]
    r = await c.request("GET", base + "/404")
    try: r.raise_for_status(); assert False
    except ex.HTTPError: pass
    r = await c.request("GET", base + "/latin"); assert r.text == "é"
    async with c.stream("GET", base + "/big", accept_encoding=None) as s:
        n = 0
        async for chunk in s.aiter_content(): n += len(chunk)
        assert n == 100000
    try: await c.request("GET", "http://127.0.0.1:1/", timeout=2); assert False
    except ex.ConnectionError: pass
    c2 = AsyncSession(curl_options={CurlOpt.PROTOCOLS_STR: "https"})
    try: await c2.request("GET", base + "/"); assert False
    except ex.RequestException: pass
    await c.close(); await c2.close()
    print("curl_cffi shim: OK")

asyncio.run(main())

import msgspec
class A(msgspec.Struct, kw_only=True, forbid_unknown_fields=True):
    x: int = 1
    y: str | None = None
    z: list[str] = msgspec.field(default_factory=list)
a = msgspec.json.decode(b'{"x": 2, "z": ["a"]}', type=A); assert a.x == 2 and a.z == ["a"]
try: msgspec.json.decode(b'{"bad": 1}', type=A); assert False
except msgspec.ValidationError: pass
print("msgspec shim: OK")
