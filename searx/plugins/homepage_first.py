# SPDX-License-Identifier: AGPL-3.0-or-later
# pylint: disable=missing-module-docstring, unused-argument
"""إن بحثتَ باسم موقع (مثل «github» أو «stack overflow») ووُجدت صفحته الرئيسية
بين النتائج، تُرفع إلى أول القائمة بدل أن تسبقها مقالات تتحدث عنه."""

import typing as t
from urllib.parse import urlparse

from . import Plugin, PluginInfo

if t.TYPE_CHECKING:
    from searx.search import SearchWithPlugins
    from searx.extended_types import SXNG_Request
    from searx.plugins import PluginCfg

_SECOND_LEVEL = {"co", "com", "org", "net", "gov", "edu", "ac"}
_BOOST_POSITIONS = 20  # يجعل مجموع النقاط أعلى من أي نتيجة عادية


def _site_label(host: str) -> str:
    """الاسم الأساسي للنطاق: www.github.com -> github ، bbc.co.uk -> bbc"""
    host = host.lower().split(":")[0]
    if host.startswith("www."):
        host = host[4:]
    parts = host.split(".")
    if len(parts) >= 3 and len(parts[-1]) == 2 and parts[-2] in _SECOND_LEVEL:
        return parts[-3]
    if len(parts) >= 2:
        return parts[-2]
    return host


def _is_apex(host: str) -> bool:
    """True إن لم يكن للنطاق نطاق فرعي (docs.python.org ليس رئيسيًا)."""
    parts = host.split(".")
    if len(parts) >= 3 and len(parts[-1]) == 2 and parts[-2] in _SECOND_LEVEL:
        return len(parts) == 3
    return len(parts) == 2


@t.final
class SXNGPlugin(Plugin):
    """Homepage first: الصفحة الرئيسية للموقع المبحوث عنه تظهر أولًا."""

    id = "homepage_first"

    def __init__(self, plg_cfg: "PluginCfg") -> None:
        super().__init__(plg_cfg)
        self.info = PluginInfo(
            id=self.id,
            name="Homepage first",
            description="If the query is a site name and its homepage is in the results, show it first",
            preference_section="general",
        )

    def post_search(self, request: "SXNG_Request", search: "SearchWithPlugins"):
        query = (search.search_query.query or "").strip().lower()
        if not query or len(query.split()) > 3:
            return None
        name = query.replace(" ", "")
        if name.startswith("www."):
            name = name[4:]
        # «github.com» يطابق النطاق كاملًا، و«github» يطابق الاسم الأساسي
        for result in search.result_container.main_results_map.values():
            url = getattr(result, "url", None) or (result.get("url") if hasattr(result, "get") else None)
            if not url:
                continue
            parsed = urlparse(url)
            host = (parsed.hostname or "").lower()
            if host.startswith("www."):
                host = host[4:]
            if parsed.path not in ("", "/") or parsed.query:
                continue
            if _is_apex(host) and name in (host, _site_label(host)):
                result.priority = "high"
                result.positions = list(result.positions) + [1] * _BOOST_POSITIONS
                break
        return None
