"""بديل خفيف بلغة بايثون الخالصة لحزمة msgspec (لا تتوفر لها حزمة جاهزة لأندرويد/Chaquopy).

يغطي ما يستخدمه Seekr فقط: Struct (kw_only / forbid_unknown_fields / __post_init__ /
__struct_fields__) و field و to_builtins و json.encode/decode(type=) و toml.decode(type=).
"""
from __future__ import annotations

import base64
import copy
import dataclasses
import datetime as _dt
import enum
import types
import typing as t


class MsgspecError(Exception):
    pass


class EncodeError(MsgspecError):
    pass


class DecodeError(MsgspecError):
    pass


class ValidationError(DecodeError, ValueError):
    pass


class _Unset:
    def __repr__(self):
        return "UNSET"

    def __bool__(self):
        return False


UNSET = _Unset()
_MISSING = object()


class _Field:
    __slots__ = ("default", "default_factory", "name")

    def __init__(self, default=_MISSING, default_factory=_MISSING, name=None):
        self.default = default
        self.default_factory = default_factory
        self.name = name


def field(*, default=_MISSING, default_factory=_MISSING, name=None):
    if default is not _MISSING and default_factory is not _MISSING:
        raise TypeError("Cannot set both default and default_factory")
    return _Field(default, default_factory, name)


def _default_value(spec):
    _, default, factory = spec
    if factory is not _MISSING:
        return factory()
    if isinstance(default, (list, dict, set, bytearray)):
        # msgspec ينسخ القيم الافتراضية القابلة للتعديل لكل نسخة
        return copy.copy(default)
    return default


def _is_classvar(ann) -> bool:
    if isinstance(ann, str):
        return ann.startswith(("ClassVar", "t.ClassVar", "typing.ClassVar"))
    return ann is t.ClassVar or t.get_origin(ann) is t.ClassVar


class _StructMeta(type):
    def __new__(mcs, name, bases, ns, **kw):
        kw_only = kw.pop("kw_only", None)
        forbid = kw.pop("forbid_unknown_fields", None)
        for ignored in ("frozen", "order", "eq", "omit_defaults", "array_like", "tag", "tag_field", "rename",
                        "repr_omit_defaults", "weakref", "dict", "gc", "cache_hash"):
            kw.pop(ignored, None)

        cls = super().__new__(mcs, name, bases, ns, **kw)

        cfg = {"kw_only": False, "forbid": False}
        for b in bases:
            base_cfg = getattr(b, "__struct_config__", None)
            if base_cfg:
                cfg.update(base_cfg)
        if kw_only is not None:
            cfg["kw_only"] = kw_only
        if forbid is not None:
            cfg["forbid"] = forbid
        cls.__struct_config__ = cfg

        specs: dict[str, tuple] = {}
        kwonly_names: set[str] = set()
        for b in reversed(cls.__mro__[1:]):
            for fname, spec, ko in getattr(b, "__struct_specs_own__", ()):
                specs[fname] = spec
                if ko:
                    kwonly_names.add(fname)
                else:
                    kwonly_names.discard(fname)

        own = []
        for fname, ann in ns.get("__annotations__", {}).items():
            if _is_classvar(ann):
                continue
            default = ns.get(fname, _MISSING)
            factory = _MISSING
            if isinstance(default, _Field):
                factory = default.default_factory
                default = default.default
            if fname in ns:
                try:
                    delattr(cls, fname)
                except AttributeError:
                    pass
            spec = (fname, default, factory)
            specs[fname] = spec
            own.append((fname, spec, cfg["kw_only"]))
            if cfg["kw_only"]:
                kwonly_names.add(fname)
            else:
                kwonly_names.discard(fname)
        cls.__struct_specs_own__ = tuple(own)
        cls.__struct_specs__ = specs
        cls.__struct_kwonly__ = frozenset(kwonly_names)

        ordered = [n for n in specs if n not in kwonly_names] + [n for n in specs if n in kwonly_names]
        cls.__struct_fields__ = tuple(ordered)
        cls.__match_args__ = tuple(n for n in ordered if n not in kwonly_names)
        return cls


class Struct(metaclass=_StructMeta):
    __struct_fields__ = ()
    __struct_specs__ = {}
    __struct_kwonly__ = frozenset()

    def __init__(self, *args, **kwargs):
        cls = type(self)
        fields = cls.__struct_fields__
        specs = cls.__struct_specs__
        positional = [n for n in fields if n not in cls.__struct_kwonly__]
        if len(args) > len(positional):
            raise TypeError(f"Extra positional arguments provided (expected at most {len(positional)})")
        values = dict(zip(positional, args))
        for k, v in kwargs.items():
            if k in values:
                raise TypeError(f"Argument '{k}' given by name and position")
            if k not in specs:
                raise TypeError(f"Unexpected keyword argument '{k}'")
            values[k] = v
        for fname in fields:
            if fname in values:
                object.__setattr__(self, fname, values[fname])
                continue
            _, default, factory = specs[fname]
            if default is _MISSING and factory is _MISSING:
                raise TypeError(f"Missing required argument '{fname}'")
            object.__setattr__(self, fname, _default_value(specs[fname]))
        post = getattr(self, "__post_init__", None)
        if post is not None:
            post()

    def __repr__(self):
        inner = ", ".join(f"{n}={getattr(self, n, UNSET)!r}" for n in type(self).__struct_fields__)
        return f"{type(self).__name__}({inner})"

    def __eq__(self, other):
        if type(other) is not type(self):
            return NotImplemented
        return all(getattr(self, n, UNSET) == getattr(other, n, UNSET) for n in type(self).__struct_fields__)

    __hash__ = None  # type: ignore[assignment]  # كما في msgspec: غير قابل للتجزئة بدون frozen

    def __copy__(self):
        new = object.__new__(type(self))
        new.__dict__.update(self.__dict__)
        return new


# ---------------------------------------------------------------------------
# to_builtins
# ---------------------------------------------------------------------------

def to_builtins(obj, *, str_keys=False, builtin_types=None, enc_hook=None, order=None):
    return _to_builtins(obj, enc_hook)


def _to_builtins(obj, enc_hook=None):  # pylint: disable=too-many-return-statements
    if obj is None or isinstance(obj, (bool, int, float, str)):
        return obj
    if isinstance(obj, Struct):
        return {n: _to_builtins(getattr(obj, n, None), enc_hook) for n in type(obj).__struct_fields__}
    if isinstance(obj, dict):
        return {(k if isinstance(k, str) else str(k)): _to_builtins(v, enc_hook) for k, v in obj.items()}
    if isinstance(obj, (list, tuple, set, frozenset)):
        return [_to_builtins(v, enc_hook) for v in obj]
    if isinstance(obj, (bytes, bytearray)):
        return base64.b64encode(bytes(obj)).decode("ascii")
    if isinstance(obj, (_dt.datetime, _dt.date, _dt.time)):
        s = obj.isoformat()
        return s[:-6] + "Z" if s.endswith("+00:00") else s
    if isinstance(obj, enum.Enum):
        return _to_builtins(obj.value, enc_hook)
    if dataclasses.is_dataclass(obj) and not isinstance(obj, type):
        return {f.name: _to_builtins(getattr(obj, f.name), enc_hook) for f in dataclasses.fields(obj)}
    if enc_hook is not None:
        return _to_builtins(enc_hook(obj), None)
    raise TypeError(f"Encoding objects of type {type(obj).__name__} is unsupported")


# ---------------------------------------------------------------------------
# التحقق من الأنواع / التحويل
# ---------------------------------------------------------------------------

def _type_name(v) -> str:
    return {type(None): "null", bool: "bool", int: "int", float: "float", str: "str", list: "array",
            dict: "object", bytes: "bytes"}.get(type(v), type(v).__name__)


def _expected(tp) -> str:
    return {str: "str", int: "int", float: "float", bool: "bool"}.get(tp, getattr(tp, "__name__", str(tp)))


def _hints(cls) -> dict:
    cached = cls.__dict__.get("__struct_hints__")
    if cached is not None:
        return cached
    hints: dict = {}
    for klass in reversed(cls.__mro__):
        if klass is object or klass is Struct:
            continue
        try:
            mod = __import__(klass.__module__, fromlist=["*"])
            hints.update(t.get_type_hints(klass, globalns=vars(mod), localns=dict(vars(klass))))
        except Exception:  # pylint: disable=broad-except
            for k, v in getattr(klass, "__annotations__", {}).items():
                hints.setdefault(k, v)
    type.__setattr__(cls, "__struct_hints__", hints)
    return hints


def _fail(msg, path):
    raise ValidationError(f"{msg} - at `{path}`" if path else msg)


def _conv(value, tp, path):  # pylint: disable=too-many-return-statements,too-many-branches,too-many-statements
    if tp is t.Any or tp is object or isinstance(tp, str):
        return value
    if tp is None or tp is type(None):
        if value is None:
            return None
        _fail(f"Expected `null`, got `{_type_name(value)}`", path)
    origin = t.get_origin(tp)

    if origin is t.Union or origin is types.UnionType:
        args = t.get_args(tp)
        if value is None and type(None) in args:
            return None
        for a in args:
            if a is type(None):
                continue
            try:
                return _conv(value, a, path)
            except ValidationError:
                continue
        names = " | ".join(_expected(a) for a in args if a is not type(None))
        if type(None) in args:
            names += " | null"
        _fail(f"Expected `{names}`, got `{_type_name(value)}`", path)

    if origin is t.Literal:
        if value in t.get_args(tp):
            return value
        _fail(f"Invalid enum value {value!r}", path)

    if origin in (list, set, frozenset, tuple) or tp in (list, set, frozenset, tuple):
        if not isinstance(value, (list, tuple)):
            _fail(f"Expected `array`, got `{_type_name(value)}`", path)
        args = t.get_args(tp)
        if origin is tuple and args and not (len(args) == 2 and args[1] is Ellipsis):
            if len(args) != len(value):
                _fail(f"Expected `array` of length {len(args)}, got {len(value)}", path)
            return tuple(_conv(v, a, f"{path}[{i}]") for i, (v, a) in enumerate(zip(value, args)))
        inner = args[0] if args else t.Any
        items = [_conv(v, inner, f"{path}[{i}]") for i, v in enumerate(value)]
        container = origin or tp
        return items if container is list else container(items)

    if origin is dict or tp is dict:
        if not isinstance(value, dict):
            _fail(f"Expected `object`, got `{_type_name(value)}`", path)
        args = t.get_args(tp)
        vt = args[1] if len(args) == 2 else t.Any
        return {k: _conv(v, vt, f"{path}[...]") for k, v in value.items()}

    if isinstance(tp, type) and issubclass(tp, Struct):
        if isinstance(value, tp):
            return value
        return _build_struct(tp, value, path)

    if isinstance(tp, type) and issubclass(tp, enum.Enum):
        try:
            return tp(value)
        except ValueError:
            _fail(f"Invalid enum value {value!r}", path)

    if tp is float:
        if isinstance(value, bool):
            _fail("Expected `float`, got `bool`", path)
        if isinstance(value, (int, float)):
            return float(value)
        _fail(f"Expected `float`, got `{_type_name(value)}`", path)
    if tp is int:
        if isinstance(value, int) and not isinstance(value, bool):
            return value
        if isinstance(value, float) and value.is_integer():
            return int(value)
        _fail(f"Expected `int`, got `{_type_name(value)}`", path)
    if tp is bool:
        if isinstance(value, bool):
            return value
        _fail(f"Expected `bool`, got `{_type_name(value)}`", path)
    if tp is str:
        if isinstance(value, str):
            return value
        _fail(f"Expected `str`, got `{_type_name(value)}`", path)
    if tp is bytes:
        if isinstance(value, bytes):
            return value
        if isinstance(value, str):
            return base64.b64decode(value)
        _fail(f"Expected `bytes`, got `{_type_name(value)}`", path)

    if isinstance(tp, type):
        if isinstance(value, tp):
            return value
        _fail(f"Expected `{tp.__name__}`, got `{_type_name(value)}`", path)
    return value


def _build_struct(cls, value, path):
    if not isinstance(value, dict):
        _fail(f"Expected `object`, got `{_type_name(value)}`", path)
    hints = _hints(cls)
    specs = cls.__struct_specs__
    forbid = cls.__struct_config__.get("forbid", False)
    kwargs = {}
    for k, v in value.items():
        if k not in specs:
            if forbid:
                _fail(f"Object contains unknown field `{k}`", path)
            continue
        kwargs[k] = _conv(v, hints.get(k, t.Any), f"{path}.{k}")
    for fname, (_, default, factory) in specs.items():
        if fname not in kwargs and default is _MISSING and factory is _MISSING:
            _fail(f"Object missing required field `{fname}`", path)
    return cls(**kwargs)


def convert(obj, type=t.Any, **_kw):  # noqa: A002  # pylint: disable=redefined-builtin
    return _conv(obj, type, "$")


from . import json, toml  # noqa: E402,F401  pylint: disable=wrong-import-position,cyclic-import

__all__ = ["Struct", "field", "to_builtins", "ValidationError", "DecodeError", "EncodeError", "MsgspecError",
           "UNSET", "json", "toml", "convert"]
