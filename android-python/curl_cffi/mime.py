class CurlMime:
    """بديل لـ curl_cffi.CurlMime: يجمع الأجزاء ثم يُحوَّل إلى files في requests."""

    def __init__(self, curl=None):
        self._parts = []

    def addpart(self, name, *, content_type=None, filename=None, local_path=None, data=None):
        if data is None and local_path is not None:
            with open(local_path, "rb") as fh:
                data = fh.read()
        self._parts.append((name, filename, data if data is not None else b"", content_type))

    @classmethod
    def from_list(cls, parts):
        m = cls()
        for p in parts:
            m.addpart(**p)
        return m

    def as_files(self):
        out = []
        for name, filename, data, ctype in self._parts:
            item = (filename, data, ctype) if ctype else (filename, data)
            out.append((name, item))
        return out

    def close(self):
        self._parts = []
