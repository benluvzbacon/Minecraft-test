#!/usr/bin/env python3
"""Local website + cubiomes biome API.

Opens http://127.0.0.1:8080 and always serves index.html there.
Never returns this .py file as a web page.
"""

from __future__ import annotations

import json
import mimetypes
import os
import socket
import subprocess
import sys
import traceback
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlparse

ROOT = os.path.dirname(os.path.abspath(__file__))
if not ROOT:
    ROOT = os.getcwd()
TOOL = os.path.join(ROOT, "native", "biome_tool")
INDEX = os.path.join(ROOT, "index.html")

# Hidden from the browser so opening /server.py cannot show source.
BLOCKED_EXT = {".py", ".pyc", ".pyo", ".c", ".h", ".o", ".so", ".bat", ".sh"}

BIOMES = [
    "plains",
    "sunflower_plains",
    "meadow",
    "cherry_grove",
    "forest",
    "flower_forest",
    "birch_forest",
    "old_growth_birch_forest",
    "dark_forest",
    "taiga",
    "old_growth_pine_taiga",
    "old_growth_spruce_taiga",
    "snowy_taiga",
    "snowy_plains",
    "ice_spikes",
    "grove",
    "snowy_slopes",
    "jagged_peaks",
    "frozen_peaks",
    "stony_peaks",
    "windswept_hills",
    "desert",
    "savanna",
    "badlands",
    "eroded_badlands",
    "wooded_badlands",
    "jungle",
    "sparse_jungle",
    "bamboo_jungle",
    "swamp",
    "mangrove_swamp",
    "mushroom_fields",
    "beach",
    "stony_shore",
    "ocean",
    "warm_ocean",
    "lukewarm_ocean",
    "cold_ocean",
    "frozen_ocean",
    "river",
    "pale_garden",
]


def _read_index():
    for candidate in (INDEX, os.path.join(os.getcwd(), "index.html")):
        try:
            if candidate and os.path.isfile(candidate):
                with open(candidate, "rb") as fh:
                    return fh.read()
        except OSError:
            continue
    return (
        b"<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
        b"<title>Minecraft Seed Finder</title></head><body>"
        b"<h1>index.html is missing</h1>"
        b"<p>Run server.py from the project folder.</p></body></html>"
    )


class Handler(BaseHTTPRequestHandler):
    # HTTP/1.1 + Connection: close so Chrome always gets a finished reply.
    protocol_version = "HTTP/1.1"
    close_connection = True
    timeout = 30

    def log_message(self, fmt, *args):
        try:
            sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))
            sys.stderr.flush()
        except Exception:
            pass

    def handle_one_request(self):
        """Never drop a connection without writing an HTTP response."""
        try:
            self.raw_requestline = self.rfile.readline(65537)
            if not self.raw_requestline:
                self.close_connection = True
                return
            if len(self.raw_requestline) > 65536:
                self._send(414, b"URI too long", "text/plain; charset=utf-8")
                return
            if not self.parse_request():
                if not getattr(self, "_wrote", False):
                    self._serve_index()
                return
            method = getattr(self, "do_" + self.command, None)
            if method is None:
                self._serve_index()
                return
            method()
            try:
                self.wfile.flush()
            except Exception:
                pass
        except Exception:
            traceback.print_exc()
            try:
                if not getattr(self, "_wrote", False):
                    self._serve_index()
            except Exception:
                self.close_connection = True

    def _send(self, code, body, content_type, extra_headers=None):
        """Write a complete HTTP response in one shot, then flush."""
        if body is None:
            body = b""
        if isinstance(body, str):
            body = body.encode("utf-8")
        reason = {
            200: "OK",
            204: "No Content",
            400: "Bad Request",
            404: "Not Found",
            414: "URI Too Long",
            500: "Internal Server Error",
        }.get(code, "OK")
        headers = [
            "HTTP/1.1 %s %s" % (code, reason),
            "Content-Type: %s" % content_type,
            "Content-Length: %s" % len(body),
            "Connection: close",
            "Cache-Control: no-store, max-age=0",
            "Access-Control-Allow-Origin: *",
            "Access-Control-Allow-Headers: Content-Type",
            "Access-Control-Allow-Methods: GET, POST, OPTIONS, HEAD",
        ]
        if extra_headers:
            headers.extend(extra_headers)
        blob = ("\r\n".join(headers) + "\r\n\r\n").encode("ascii") + body
        self.wfile.write(blob)
        self.wfile.flush()
        self.close_connection = True
        self._wrote = True
        try:
            self.log_request(code, len(body))
        except Exception:
            pass

    def _json(self, code, obj):
        data = json.dumps(obj).encode("utf-8")
        self._send(code, data, "application/json; charset=utf-8")

    def _read_json(self):
        n = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(n) if n else b"{}"
        return json.loads(raw.decode("utf-8") or "{}")

    def _clean_path(self):
        raw = getattr(self, "path", "/") or "/"
        return unquote(urlparse(raw).path)

    def _serve_index(self):
        self._send(200, _read_index(), "text/html; charset=utf-8")

    def _safe_file(self, url_path):
        rel = url_path.lstrip("/")
        if not rel or rel.endswith("/"):
            return None
        candidate = os.path.realpath(os.path.join(ROOT, rel.replace("/", os.sep)))
        root_real = os.path.realpath(ROOT)
        prefix = root_real if root_real.endswith(os.sep) else root_real + os.sep
        if candidate != root_real and not candidate.lower().startswith(prefix.lower()):
            return None
        if not os.path.isfile(candidate):
            return None
        ext = os.path.splitext(candidate)[1].lower()
        if ext in BLOCKED_EXT:
            return None
        return candidate

    def _guess_type(self, filepath):
        ext = os.path.splitext(filepath)[1].lower()
        extra = {
            ".html": "text/html; charset=utf-8",
            ".css": "text/css; charset=utf-8",
            ".js": "text/javascript; charset=utf-8",
            ".mjs": "text/javascript; charset=utf-8",
            ".json": "application/json; charset=utf-8",
            ".svg": "image/svg+xml",
            ".png": "image/png",
            ".jpg": "image/jpeg",
            ".jpeg": "image/jpeg",
            ".ico": "image/x-icon",
            ".wasm": "application/wasm",
            ".txt": "text/plain; charset=utf-8",
            ".md": "text/plain; charset=utf-8",
        }
        if ext in extra:
            return extra[ext]
        guessed, _ = mimetypes.guess_type(filepath)
        return guessed or "application/octet-stream"

    def do_OPTIONS(self):
        self._send(204, b"", "text/plain")

    def do_HEAD(self):
        path = self._clean_path()
        if path in ("", "/", "/index.html", "/index.htm", "/server.py"):
            body = _read_index()
            self._send(200, b"", "text/html; charset=utf-8", extra_headers=[
                "Content-Length: %s" % len(body),
            ])
            return
        filepath = self._safe_file(path)
        if not filepath:
            self._send(404, b"", "text/plain")
            return
        size = os.path.getsize(filepath)
        self._send(200, b"", self._guess_type(filepath), extra_headers=[
            "Content-Length: %s" % size,
        ])

    def do_GET(self):
        try:
            path = self._clean_path()

            if path == "/api/biomes":
                self._json(200, {"biomes": BIOMES, "engine": "cubiomes", "mc": "1.21"})
                return
            if path == "/api/health":
                self._json(200, {"ok": True, "tool": os.path.exists(TOOL)})
                return

            if path in ("", "/", "/index.html", "/index.htm", "/server.py"):
                self._serve_index()
                return

            ext = os.path.splitext(path)[1].lower()
            if ext in BLOCKED_EXT:
                self._serve_index()
                return

            filepath = self._safe_file(path)
            if filepath:
                with open(filepath, "rb") as fh:
                    data = fh.read()
                self._send(200, data, self._guess_type(filepath))
                return

            self._send(404, b"File not found", "text/plain; charset=utf-8")
        except Exception:
            traceback.print_exc()
            if not getattr(self, "_wrote", False):
                self._serve_index()

    def do_POST(self):
        path = self._clean_path()
        try:
            body = self._read_json()
        except Exception as exc:
            return self._json(400, {"error": "bad json: %s" % exc})

        if path == "/api/biomes/at":
            seed = str(body.get("seed", "0"))
            x = int(body.get("x", 0))
            z = int(body.get("z", 0))
            mc = str(body.get("mc") or "1.21")
            try:
                out = subprocess.check_output(
                    [TOOL, "at", seed, str(x), str(z), mc],
                    cwd=ROOT,
                    timeout=20,
                    stderr=subprocess.STDOUT,
                )
                return self._json(200, json.loads(out.decode()))
            except subprocess.CalledProcessError as exc:
                return self._json(500, {"error": exc.output.decode(errors="replace")})
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        if path == "/api/biomes/search":
            points = body.get("points") or []
            count = int(body.get("count") or 5)
            maxn = int(body.get("max") or 8000)
            mc = str(body.get("mc") or "1.21")
            args = [TOOL, "search", str(count), str(maxn), mc]
            for p in points[:8]:
                args += [str(int(p.get("x", 0))), str(int(p.get("z", 0))), str(p.get("biome") or "plains")]
            try:
                out = subprocess.check_output(args, cwd=ROOT, timeout=90, stderr=subprocess.STDOUT)
                return self._json(200, json.loads(out.decode()))
            except subprocess.CalledProcessError as exc:
                text = exc.output.decode(errors="replace")
                try:
                    return self._json(200, json.loads(text))
                except Exception:
                    return self._json(500, {"error": text or str(exc)})
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        if path == "/api/villages":
            seed = str(body.get("seed", "0"))
            radius = int(body.get("radius") or 500)
            mc = str(body.get("mc") or "1.21")
            try:
                out = subprocess.check_output(
                    [TOOL, "villages", seed, str(radius), mc],
                    cwd=ROOT,
                    timeout=30,
                    stderr=subprocess.STDOUT,
                )
                return self._json(200, json.loads(out.decode()))
            except subprocess.CalledProcessError as exc:
                return self._json(500, {"error": exc.output.decode(errors="replace")})
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        if path == "/api/spawn":
            seed = str(body.get("seed", "0"))
            mc = str(body.get("mc") or "1.21")
            try:
                out = subprocess.check_output(
                    [TOOL, "spawn", seed, mc], cwd=ROOT, timeout=40, stderr=subprocess.STDOUT
                )
                return self._json(200, json.loads(out.decode()))
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        if path == "/api/around":
            seed = str(body.get("seed", "0"))
            radius = int(body.get("radius") or 800)
            mc = str(body.get("mc") or "1.21")
            try:
                out = subprocess.check_output(
                    [TOOL, "around", seed, str(radius), mc], cwd=ROOT, timeout=40, stderr=subprocess.STDOUT
                )
                return self._json(200, json.loads(out.decode()))
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        if path == "/api/structures/filter":
            st = str(body.get("type") or "outpost")
            radius = int(body.get("radius") or 500)
            mc = str(body.get("mc") or "1.21")
            seeds = [str(s) for s in (body.get("seeds") or [])][:80]
            if not seeds:
                return self._json(200, {"hits": [], "found": 0})
            args = [TOOL, "filter_struct", st, str(radius), mc] + seeds
            try:
                out = subprocess.check_output(args, cwd=ROOT, timeout=90, stderr=subprocess.STDOUT)
                return self._json(200, json.loads(out.decode()))
            except subprocess.CalledProcessError as exc:
                text = exc.output.decode(errors="replace")
                try:
                    return self._json(200, json.loads(text))
                except Exception:
                    return self._json(500, {"error": text or str(exc)})
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        if path == "/api/structures/search":
            st = str(body.get("type") or "outpost")
            count = int(body.get("count") or 5)
            radius = int(body.get("radius") or 500)
            maxn = int(body.get("max") or 12000)
            mc = str(body.get("mc") or "1.21")
            args = [TOOL, "find_struct", st, str(count), str(maxn), str(radius), mc]
            try:
                out = subprocess.check_output(args, cwd=ROOT, timeout=120, stderr=subprocess.STDOUT)
                return self._json(200, json.loads(out.decode()))
            except subprocess.CalledProcessError as exc:
                text = exc.output.decode(errors="replace")
                try:
                    return self._json(200, json.loads(text))
                except Exception:
                    return self._json(500, {"error": text or str(exc)})
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        if path == "/api/villages/cluster":
            count = int(body.get("count") or 4)
            how_many = int(body.get("howMany") or 2)
            max_dist = int(body.get("maxDist") or 380)
            maxn = int(body.get("max") or 12000)
            mc = str(body.get("mc") or "1.21")
            args = [TOOL, "cluster", str(count), str(maxn), str(how_many), str(max_dist), mc]
            try:
                out = subprocess.check_output(args, cwd=ROOT, timeout=120, stderr=subprocess.STDOUT)
                return self._json(200, json.loads(out.decode()))
            except subprocess.CalledProcessError as exc:
                text = exc.output.decode(errors="replace")
                try:
                    return self._json(200, json.loads(text))
                except Exception:
                    return self._json(500, {"error": text or str(exc)})
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        if path == "/api/hunt":
            count = int(body.get("count") or 4)
            maxn = int(body.get("max") or 8000)
            impossible = 1 if body.get("impossible") else 0
            eyes = body.get("eyes")
            try:
                eyes_i = int(eyes)
            except (TypeError, ValueError):
                eyes_i = -1
            if eyes_i is None:
                eyes_i = -1
            mc = str(body.get("mc") or "1.21")
            args = [TOOL, "hunt", str(count), str(maxn), str(impossible), str(eyes_i), mc]
            try:
                out = subprocess.check_output(args, cwd=ROOT, timeout=120, stderr=subprocess.STDOUT)
                return self._json(200, json.loads(out.decode()))
            except subprocess.CalledProcessError as exc:
                text = exc.output.decode(errors="replace")
                try:
                    return self._json(200, json.loads(text))
                except Exception:
                    return self._json(500, {"error": text or str(exc)})
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        if path == "/api/portal":
            seed = str(body.get("seed", "0"))
            mc = str(body.get("mc") or "1.21")
            try:
                out = subprocess.check_output(
                    [TOOL, "portal", seed, mc], cwd=ROOT, timeout=40, stderr=subprocess.STDOUT
                )
                return self._json(200, json.loads(out.decode()))
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        if path == "/api/villages/filter":
            seeds = [str(s) for s in (body.get("seeds") or [])][:80]
            radius = int(body.get("radius") or 500)
            mc = str(body.get("mc") or "1.21")
            if not seeds:
                return self._json(200, {"hits": [], "found": 0})
            args = [TOOL, "filter_village", str(radius), mc] + seeds
            try:
                out = subprocess.check_output(args, cwd=ROOT, timeout=90, stderr=subprocess.STDOUT)
                return self._json(200, json.loads(out.decode()))
            except subprocess.CalledProcessError as exc:
                text = exc.output.decode(errors="replace")
                try:
                    return self._json(200, json.loads(text))
                except Exception:
                    return self._json(500, {"error": text or str(exc)})
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        self._json(404, {"error": "not found"})


class Server(ThreadingHTTPServer):
    allow_reuse_address = True
    daemon_threads = True
    request_queue_size = 64

    def server_bind(self):
        try:
            self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        except OSError:
            pass
        try:
            self.socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        except OSError:
            pass
        super().server_bind()

    def handle_error(self, request, client_address):
        traceback.print_exc()


def main():
    host = os.environ.get("HOST", "127.0.0.1")
    port = int(os.environ.get("PORT", "8080"))
    httpd = Server((host, port), Handler)
    print("Website: http://127.0.0.1:%s" % port, flush=True)
    print("listening on %s:%s" % (host, port), flush=True)
    print("Serving index.html — not server.py", flush=True)
    httpd.serve_forever()


if __name__ == "__main__":
    main()
