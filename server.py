#!/usr/bin/env python3
"""Local website + cubiomes biome API.

Opens http://127.0.0.1:8080 and always serves index.html there.
Never returns this .py file as a web page.
"""

from __future__ import annotations

import json
import mimetypes
import os
import subprocess
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlparse

ROOT = os.path.dirname(os.path.abspath(__file__))
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


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=ROOT, **kwargs)

    def end_headers(self):
        self.send_header("Cache-Control", "no-store, max-age=0")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(204)
        self.end_headers()

    def _json(self, code, obj):
        data = json.dumps(obj).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _read_json(self):
        n = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(n) if n else b"{}"
        return json.loads(raw.decode("utf-8") or "{}")

    def _clean_path(self):
        return unquote(urlparse(self.path).path)

    def _send_bytes(self, code, body, content_type):
        if isinstance(body, str):
            body = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _serve_index(self):
        with open(INDEX, "rb") as fh:
            data = fh.read()
        self._send_bytes(200, data, "text/html; charset=utf-8")

    def _safe_file(self, url_path):
        """Map a URL path to a file under ROOT, or None."""
        rel = url_path.lstrip("/")
        if not rel or rel.endswith("/"):
            return None
        candidate = os.path.realpath(os.path.join(ROOT, rel))
        root_real = os.path.realpath(ROOT)
        if not (candidate == root_real or candidate.startswith(root_real + os.sep)):
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

    def do_GET(self):
        path = self._clean_path()

        if path == "/api/biomes":
            self._json(200, {"biomes": BIOMES, "engine": "cubiomes", "mc": "1.21"})
            return
        if path == "/api/health":
            self._json(200, {"ok": True, "tool": os.path.exists(TOOL)})
            return

        # Homepage and any request that would have shown this script.
        if path in ("", "/", "/index.html", "/index.htm", "/server.py"):
            self._serve_index()
            return

        ext = os.path.splitext(path)[1].lower()
        if ext in BLOCKED_EXT:
            # Do not display Python / C source in the browser.
            self._serve_index()
            return

        filepath = self._safe_file(path)
        if filepath:
            with open(filepath, "rb") as fh:
                data = fh.read()
            self._send_bytes(200, data, self._guess_type(filepath))
            return

        self.send_error(404, "File not found")

    def do_POST(self):
        path = self._clean_path()
        try:
            body = self._read_json()
        except Exception as exc:
            return self._json(400, {"error": f"bad json: {exc}"})

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


def main():
    # Default is the address you open in the browser. HOST=0.0.0.0 is for previews.
    host = os.environ.get("HOST", "127.0.0.1")
    port = int(os.environ.get("PORT", "8080"))
    ThreadingHTTPServer.allow_reuse_address = True
    httpd = ThreadingHTTPServer((host, port), Handler)
    print(f"Website: http://127.0.0.1:{port}", flush=True)
    print(f"listening on {host}:{port}", flush=True)
    print("Serving index.html — not server.py", flush=True)
    httpd.serve_forever()


if __name__ == "__main__":
    main()
