import tomllib

from . import _conv, DecodeError


def decode(buf, *, type=None, strict=True, dec_hook=None):  # noqa: A002
    if isinstance(buf, (bytes, bytearray)):
        buf = bytes(buf).decode("utf-8")
    try:
        data = tomllib.loads(buf)
    except tomllib.TOMLDecodeError as e:
        raise DecodeError(f"TOML is malformed: {e}") from e
    if type is None:
        return data
    return _conv(data, type, "$")
