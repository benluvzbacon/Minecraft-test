#!/usr/bin/env python3
"""Static site + cubiomes biome API. Bind 0.0.0.0 for the live preview."""

from __future__ import annotations

import json
import os
import subprocess
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

ROOT = os.path.dirname(os.path.abspath(__file__))
TOOL = os.path.join(ROOT, "native", "biome_tool")

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

    def do_GET(self):
        if self.path.split("?", 1)[0] == "/api/biomes":
            self._json(200, {"biomes": BIOMES, "engine": "cubiomes", "mc": "1.21"})
            return
        if self.path.split("?", 1)[0] == "/api/health":
            self._json(200, {"ok": True, "tool": os.path.exists(TOOL)})
            return
        return super().do_GET()

    def do_POST(self):
        path = self.path.split("?", 1)[0]
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

        self._json(404, {"error": "not found"})


if __name__ == "__main__":
    port = int(os.environ.get("PORT", "8080"))
    httpd = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    print(f"listening on 0.0.0.0:{port}", flush=True)
    httpd.serve_forever()
