import enum


class CurlHttpVersion(enum.IntEnum):
    NONE = 0
    V1_0 = 1
    V1_1 = 2
    V2_0 = 3
    V2TLS = 4
    V2_PRIOR_KNOWLEDGE = 5
    V3 = 30
    V3ONLY = 31


class CurlOpt(enum.IntEnum):
    """القيم الحقيقية لخيارات libcurl (المستخدمة فقط للمقارنة/الترتيب)."""
    USERPWD = 10005
    PROXY = 10004
    CAINFO = 10065
    CAPATH = 10097
    USERAGENT = 10018
    REFERER = 10016
    HTTPAUTH = 107
    TIMEOUT = 13
    PROTOCOLS_STR = 10318
    REDIR_PROTOCOLS_STR = 10319
    SSL_VERIFYPEER = 64
    SSL_VERIFYHOST = 81
    FOLLOWLOCATION = 52
    MAXREDIRS = 68
    INTERFACE = 10062
