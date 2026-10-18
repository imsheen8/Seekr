"""أسماء الاستثناءات (والوحدة نفسها) مطابقة لـ curl_cffi.requests.exceptions لأن Seekr
يستخدم اسم الصنف الكامل نصًّا في رسائل الأخطاء."""


class RequestException(Exception):
    def __init__(self, *args, code=None, response=None, request=None, **kwargs):
        super().__init__(*args)
        self.code = code
        self.response = response
        self.request = request


class CurlError(RequestException):
    pass


class HTTPError(RequestException):
    pass


class ConnectionError(RequestException):  # pylint: disable=redefined-builtin
    pass


class Timeout(RequestException):
    pass


class ConnectTimeout(ConnectionError, Timeout):
    pass


class ReadTimeout(Timeout):
    pass


class DNSError(ConnectionError):
    pass


class SSLError(ConnectionError):
    pass


class ProxyError(ConnectionError):
    pass


class TooManyRedirects(RequestException):
    pass


class IncompleteRead(RequestException):
    pass


class InvalidURL(RequestException):
    pass


class StreamError(RequestException):
    pass


class StreamConsumedError(StreamError, TypeError):
    pass


class SessionClosed(RequestException):
    pass


class ImpersonateError(RequestException):
    pass


class InterfaceError(RequestException):
    pass
