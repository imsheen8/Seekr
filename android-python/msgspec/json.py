import json as _json

from . import _to_builtins, _conv, DecodeError, EncodeError


def encode(obj, *, enc_hook=None, order=None) -> bytes:
    try:
        return _json.dumps(_to_builtins(obj, enc_hook), ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    except TypeError as e:
        raise EncodeError(str(e)) from e


def decode(buf, *, type=None, strict=True, dec_hook=None):  # noqa: A002
    try:
        data = _json.loads(buf)
    except ValueError as e:
        raise DecodeError(f"JSON is malformed: {e}") from e
    if type is None:
        return data
    return _conv(data, type, "$")
