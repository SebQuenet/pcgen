"""Thin client over the PCGen headless HTTP server.

The server exposes every session operation at POST /api/{operation} and answers
{"ok": bool, "data": ..., "error": ...}. Everything here returns `data` or
raises PcgenError with the operation name and the server's message.
"""

from __future__ import annotations

import json
import os
import urllib.error
import urllib.request

HOST = f"127.0.0.1:{os.environ.get('PCGEN_PORT', '8420')}"
BASE_URL = f"http://{HOST}"
TIMEOUT_SECONDS = 600


class PcgenError(RuntimeError):
    def __init__(self, operation: str, message: str) -> None:
        super().__init__(f"{operation}: {message}")
        self.operation = operation
        self.message = message


class PcgenInterrupted(PcgenError):
    """The call outlived its timeout: the server is still busy with it and will answer nothing else."""


class PcgenUnreachable(PcgenError):
    """Nothing listens on the port: the server is gone, and no character is to blame."""


def call(operation: str, **arguments):
    payload = json.dumps(arguments).encode("utf-8")
    request = urllib.request.Request(
        f"{BASE_URL}/api/{operation}",
        data=payload,
        headers={"Content-Type": "application/json", "Host": HOST},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            body = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        raise PcgenError(operation, error.read().decode("utf-8", "replace")) from error
    except urllib.error.URLError as error:
        raise PcgenUnreachable(operation, f"serveur injoignable ({error.reason})") from error
    except OSError as error:
        # A single slow operation must not end a run of several hundred.
        raise PcgenInterrupted(operation, f"appel interrompu ({error})") from error
    if not body.get("ok"):
        raise PcgenError(operation, json.dumps(body.get("error"), ensure_ascii=False))
    return body.get("data")


def health() -> dict:
    with urllib.request.urlopen(f"{BASE_URL}/health", timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))["data"]
