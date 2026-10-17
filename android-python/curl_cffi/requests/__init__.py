"""AsyncSession / Response متوافقان مع ما يستخدمه Seekr، مبنيّان على مكتبة requests."""
from __future__ import annotations

import asyncio
import concurrent.futures
import http.cookiejar
import json as _json
import re
import typing as t
from urllib.parse import urljoin

import requests as _rq
from requests.auth import HTTPDigestAuth
from requests.structures import CaseInsensitiveDict

from ..const import CurlHttpVersion, CurlOpt
from ..mime import CurlMime
from . import exceptions
from .exceptions import RequestException

__all__ = ["AsyncSession", "Response", "exceptions", "RequestException"]

# مجمّع خيوط مشترك لكل الطلبات (requests متزامنة، تُشغَّل خارج حلقة asyncio)
_EXECUTOR = concurrent.futures.ThreadPoolExecutor(max_workers=48, thread_name_prefix="sxng-http")

_UA_CHROME_ANDROID = (
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/131.0.0.0 Mobile Safari/537.36"
)
_UA_CHROME = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/131.0.0.0 Safari/537.36"
)
_UA_FIREFOX = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0"
_UA_NEUTRAL = "Mozilla/5.0 (compatible; Seekr)"

_REDIRECT_CODES = (301, 302, 303, 307, 308)
_DNS_HINTS = ("name or service not known", "getaddrinfo", "nodename nor servname", "temporary failure in name",
              "no address associated")
_META_CHARSET = re.compile(rb"<meta[^>]+charset=[\"']?\s*([A-Za-z0-9_\-:.]+)", re.I)


class _Cookies(dict):
    def get_dict(self):
        return dict(self)


class _Request:
    __slots__ = ("method", "url", "headers")

    def __init__(self, method: str, url: str, headers):
        self.method = method
        self.url = url
        self.headers = headers


class Response:
    """يحاكي curl_cffi.requests.Response (الخصائص التي يقرؤها Seekr فقط)."""

    def __init__(self, request: _Request | None = None):
        self.request = request
        self.status_code = 0
        self.reason = ""
        self.headers: CaseInsensitiveDict = CaseInsensitiveDict()
        self.content = b""
        self.history: list[Response] = []
        self.cookies = _Cookies()
        self.http_version = int(CurlHttpVersion.V1_1)
        self.encoding: str | None = None
        self.default_encoding = "utf-8"
        self.elapsed = 0.0
        self.url = ""
        self._raw: _rq.Response | None = None
        self._iter = None

    # ---- Inhalt -----------------------------------------------------------
    def _guess_encoding(self) -> str:
        if self.encoding:
            return self.encoding
        ctype = self.headers.get("content-type", "")
        m = re.search(r"charset=([^\s;]+)", ctype, re.I)
        if m:
            return m.group(1).strip("\"'")
        if "html" in ctype.lower() or not ctype:
            m2 = _META_CHARSET.search(self.content[:4096])
            if m2:
                return m2.group(1).decode("ascii", "ignore")
        return self.default_encoding

    @property
    def text(self) -> str:
        enc = self._guess_encoding()
        try:
            return self.content.decode(enc, errors="replace")
        except LookupError:
            return self.content.decode("utf-8", errors="replace")

    def json(self, **kwargs):
        return _json.loads(self.content, **kwargs)

    @property
    def ok(self) -> bool:
        return self.status_code < 400

    def raise_for_status(self):
        if self.status_code >= 400:
            raise exceptions.HTTPError(
                f"HTTP Error {self.status_code}: {self.reason}", code=self.status_code, response=self
            )

    # ---- Streaming --------------------------------------------------------
    async def aiter_content(self, chunk_size: int | None = None):
        if self._raw is None:
            raise exceptions.StreamConsumedError("Response content already consumed")
        loop = asyncio.get_running_loop()
        if self._iter is None:
            self._iter = self._raw.raw.stream(chunk_size or 8192, decode_content=False)
        it = self._iter

        def _next():
            try:
                return next(it)
            except StopIteration:
                return None
            except Exception as e:  # pylint: disable=broad-except
                raise exceptions.IncompleteRead(str(e)) from e

        while True:
            chunk = await loop.run_in_executor(_EXECUTOR, _next)
            if chunk is None:
                break
            yield chunk

    async def aclose(self):
        raw, self._raw = self._raw, None
        if raw is not None:
            await asyncio.get_running_loop().run_in_executor(_EXECUTOR, raw.close)

    async def __aenter__(self):
        return self

    async def __aexit__(self, *exc):
        await self.aclose()


class _StreamContext:
    def __init__(self, session: "AsyncSession", method: str, url: str, kwargs: dict):
        self._args = (session, method, url, kwargs)
        self._resp: Response | None = None

    async def __aenter__(self) -> Response:
        session, method, url, kwargs = self._args
        loop = asyncio.get_running_loop()
        self._resp = await loop.run_in_executor(_EXECUTOR, session._do, method, url, kwargs, True)
        return self._resp

    async def __aexit__(self, *exc):
        if self._resp is not None:
            await self._resp.aclose()


def _browser_headers(impersonate: str | None) -> dict[str, str]:
    imp = (impersonate or "").lower()
    if not imp or imp == "none":
        return {"User-Agent": _UA_NEUTRAL}
    if "firefox" in imp:
        ua = _UA_FIREFOX
    elif "android" in imp:
        ua = _UA_CHROME_ANDROID
    else:
        ua = _UA_CHROME
    return {
        "User-Agent": ua,
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.9",
    }


class AsyncSession:
    def __init__(  # pylint: disable=too-many-arguments,unused-argument
        self,
        *,
        verify: bool | str = True,
        max_redirects: int = 30,
        max_clients: int = 10,
        response_class: type[Response] | None = None,
        discard_cookies: bool = False,
        proxy: str | None = None,
        proxies: dict[str, str] | None = None,
        impersonate: str | None = None,
        default_headers: bool | None = None,
        interface: str | None = None,
        http_version: int | None = None,
        curl_options: dict[int, t.Any] | None = None,
        timeout: float | None = None,
        headers: dict[str, str] | None = None,
        **_ignored: t.Any,
    ):
        self._verify = verify
        self._max_redirects = max_redirects
        self._response_class = response_class or Response
        self._impersonate = impersonate
        self._default_headers = True if default_headers is None else default_headers
        self._curl_options = dict(curl_options or {})
        self._timeout = timeout
        self._headers = dict(headers or {})
        self._proxies = dict(proxies or {})
        if proxy:
            self._proxies = {"http": proxy, "https": proxy}
        self._session = _rq.Session()
        adapter = _rq.adapters.HTTPAdapter(pool_connections=max_clients, pool_maxsize=max_clients)
        self._session.mount("http://", adapter)
        self._session.mount("https://", adapter)
        if discard_cookies:
            self._session.cookies.set_policy(http.cookiejar.DefaultCookiePolicy(allowed_domains=[]))
        self._closed = False

    # ---- öffentliche API -------------------------------------------------
    async def request(self, method: str, url: str, **kwargs: t.Any) -> Response:
        loop = asyncio.get_running_loop()
        return await loop.run_in_executor(_EXECUTOR, self._do, method, url, kwargs, False)

    async def get(self, url: str, **kwargs):
        return await self.request("GET", url, **kwargs)

    async def post(self, url: str, **kwargs):
        return await self.request("POST", url, **kwargs)

    async def head(self, url: str, **kwargs):
        return await self.request("HEAD", url, **kwargs)

    def stream(self, method: str, url: str, **kwargs: t.Any) -> _StreamContext:
        return _StreamContext(self, method, url, kwargs)

    async def close(self):
        self._closed = True
        self._session.close()

    # ---- Implementierung -------------------------------------------------
    def _wrap(self, resp: _rq.Response, prep, history: list, cookies: dict, stream: bool) -> Response:
        out = self._response_class(request=_Request(prep.method, prep.url, prep.headers))
        out.status_code = resp.status_code
        out.reason = resp.reason or ""
        out.headers = CaseInsensitiveDict(resp.headers)
        out.cookies = _Cookies(cookies)
        out.url = str(resp.url)
        out.history = history
        version = getattr(resp.raw, "version", 11)
        out.http_version = int(CurlHttpVersion.V1_0 if version == 10 else CurlHttpVersion.V1_1)
        out.elapsed = resp.elapsed.total_seconds() if resp.elapsed is not None else 0.0
        if stream:
            out._raw = resp  # pylint: disable=protected-access
        else:
            out.content = resp.content
        return out

    def _do(self, method: str, url: str, kwargs: dict, stream: bool) -> Response:
        try:
            return self._do_inner(method, url, dict(kwargs), stream)
        except exceptions.RequestException:
            raise
        except _rq.exceptions.ConnectTimeout as e:
            raise exceptions.ConnectTimeout(str(e)) from e
        except _rq.exceptions.ReadTimeout as e:
            raise exceptions.ReadTimeout(str(e)) from e
        except _rq.exceptions.SSLError as e:
            raise exceptions.SSLError(str(e)) from e
        except _rq.exceptions.ProxyError as e:
            raise exceptions.ProxyError(str(e)) from e
        except _rq.exceptions.TooManyRedirects as e:
            raise exceptions.TooManyRedirects(str(e)) from e
        except _rq.exceptions.ConnectionError as e:
            if any(h in str(e).lower() for h in _DNS_HINTS):
                raise exceptions.DNSError(str(e)) from e
            raise exceptions.ConnectionError(str(e)) from e
        except (_rq.exceptions.ChunkedEncodingError, _rq.exceptions.ContentDecodingError) as e:
            raise exceptions.IncompleteRead(str(e)) from e
        except _rq.exceptions.Timeout as e:
            raise exceptions.Timeout(str(e)) from e
        except (_rq.exceptions.InvalidURL, _rq.exceptions.MissingSchema, _rq.exceptions.InvalidSchema) as e:
            raise exceptions.InvalidURL(str(e)) from e
        except _rq.exceptions.RequestException as e:
            raise exceptions.RequestException(str(e)) from e

    def _do_inner(self, method: str, url: str, kw: dict, stream: bool) -> Response:
        # pylint: disable=too-many-locals,too-many-branches,too-many-statements
        method = method.upper()
        headers = {**_browser_headers(self._impersonate if self._default_headers else None)}
        headers.update(self._headers)
        headers.update(kw.pop("headers", None) or {})
        cookies = dict(kw.pop("cookies", None) or {})
        auth = kw.pop("auth", None)
        verify = kw.pop("verify", self._verify)
        max_redirects = kw.pop("max_redirects", self._max_redirects)
        allow_redirects = kw.pop("allow_redirects", True)
        timeout = kw.pop("timeout", None) or self._timeout or 30
        params = kw.pop("params", None)
        data = kw.pop("data", None)
        json_body = kw.pop("json", None)
        content = kw.pop("content", None)
        multipart = kw.pop("multipart", None)
        if "accept_encoding" in kw:
            ae = kw.pop("accept_encoding")
            headers["Accept-Encoding"] = "identity" if ae is None else str(ae)
        if kw.pop("impersonate", None) is not None or kw.pop("default_headers", None) is not None:
            pass
        proxy = kw.pop("proxy", None)
        proxies = dict(self._proxies)
        if proxy:
            proxies = {"http": proxy, "https": proxy}
        proxies.update(kw.pop("proxies", None) or {})
        curl_options = {**self._curl_options, **(kw.pop("curl_options", None) or {})}
        kw.clear()  # بقية الخيارات (interface, http_version, ...) غير مدعومة هنا وتُتجاهل

        if str(curl_options.get(CurlOpt.PROTOCOLS_STR, "")).lower() == "https" and url.lower().startswith("http://"):
            raise exceptions.RequestException("Protocol \"http\" not supported", code=1)
        if verify is True and curl_options.get(CurlOpt.CAINFO):
            verify = str(curl_options[CurlOpt.CAINFO])
        if auth is None and curl_options.get(CurlOpt.HTTPAUTH) == 2 and curl_options.get(CurlOpt.USERPWD):
            user, _, pwd = str(curl_options[CurlOpt.USERPWD]).partition(":")
            auth = HTTPDigestAuth(user, pwd)

        files = None
        if isinstance(multipart, CurlMime):
            files = multipart.as_files()
        body: t.Any = None
        if content:
            body = content
        elif data:
            body = data
        json_arg = json_body if json_body else None

        history: list[Response] = []
        cookie_acc: dict = {}
        redirects = 0
        cur_method, cur_url = method, url
        while True:
            req = _rq.Request(
                cur_method, cur_url, headers=headers, params=params, cookies=cookies, auth=auth,
                data=body, json=json_arg, files=files,
            )
            prep = self._session.prepare_request(req)
            settings = self._session.merge_environment_settings(prep.url, proxies, stream, verify, None)
            resp = self._session.send(prep, timeout=(timeout, timeout), allow_redirects=False, **settings)
            cookie_acc.update(resp.cookies.get_dict())

            location = resp.headers.get("location")
            if allow_redirects and resp.status_code in _REDIRECT_CODES and location:
                if redirects >= max_redirects:
                    resp.close()
                    raise exceptions.TooManyRedirects(f"Maximum ({max_redirects}) redirects exceeded")
                redirects += 1
                _ = resp.content  # الاتصال يعود للمجمّع
                history.append(self._wrap(resp, prep, [], dict(cookie_acc), False))
                cur_url = urljoin(resp.url, location)
                if resp.status_code == 303 or (resp.status_code in (301, 302) and cur_method == "POST"):
                    if cur_method != "HEAD":
                        cur_method = "GET"
                    body = json_arg = files = None
                    headers = {k: v for k, v in headers.items() if k.lower() not in ("content-type", "content-length")}
                params = None
                cookies = {**cookies, **cookie_acc}
                continue
            return self._wrap(resp, prep, history, cookie_acc, stream)
