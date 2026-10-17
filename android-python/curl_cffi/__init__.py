"""بديل لحزمة curl_cffi فوق مكتبة requests (curl_cffi تحتاج مكتبات أصلية غير متوفرة لأندرويد/Chaquopy).

يوفّر الواجهة التي يستخدمها Seekr فقط. الفرق الجوهري: لا يوجد انتحال لبصمة TLS للمتصفح
(impersonate) — يُرسَل فقط ترويسات متصفح افتراضية، وقد تحجب بعض المحركات (جوجل/DuckDuckGo) ذلك أحيانًا.
"""
from .const import CurlHttpVersion, CurlOpt
from .mime import CurlMime
from .requests import AsyncSession, Response

__all__ = ["AsyncSession", "Response", "CurlHttpVersion", "CurlOpt", "CurlMime"]
