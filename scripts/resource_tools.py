"""Small, dependency-free writers for the committed Riftborn assets (PNG and NBT)."""
from pathlib import Path
import gzip
import json
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"


def write_json(relative, value):
    path = RES / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


class Image:
    def __init__(self, width, height, color=(0, 0, 0, 0)):
        self.width, self.height = width, height
        self.pixels = [[color for _ in range(width)] for _ in range(height)]

    def pixel(self, x, y, color):
        if 0 <= x < self.width and 0 <= y < self.height:
            self.pixels[y][x] = tuple(color) if len(color) == 4 else (*color, 255)

    def rect(self, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.pixel(x, y, color)

    def line(self, x0, y0, x1, y1, color, width=1):
        length = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(length + 1):
            x = round(x0 + (x1 - x0) * i / length)
            y = round(y0 + (y1 - y0) * i / length)
            self.rect(x, y, x + width - 1, y + width - 1, color)

    def save(self, relative):
        def chunk(kind, data):
            return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)
        raw = b"".join(b"\x00" + bytes(c for p in row for c in p) for row in self.pixels)
        png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
        path = RES / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(png)


# Explicit NBT type tags prevent Python integers from accidentally becoming bytes/floats.
def byte(v): return (1, v)
def integer(v): return (3, v)
def long(v): return (4, v)
def float_tag(v): return (5, v)
def double(v): return (6, v)
def string(v): return (8, v)
def list_tag(kind, values): return (9, (kind, values))
def compound(values): return (10, values)


def _utf(text):
    data = text.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def _payload(kind, value):
    if kind in (1, 3, 4, 5, 6):
        return struct.pack({1: ">b", 3: ">i", 4: ">q", 5: ">f", 6: ">d"}[kind], value)
    if kind == 8:
        return _utf(value)
    if kind == 9:
        subtype, values = value
        return bytes([subtype]) + struct.pack(">i", len(values)) + b"".join(_payload(subtype, v) for v in values)
    if kind == 10:
        return b"".join(bytes([typ]) + _utf(name) + _payload(typ, val) for name, (typ, val) in value.items()) + b"\x00"
    raise ValueError(f"Unsupported NBT tag {kind}")


def write_nbt(path, root):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    # mtime=0 makes regeneration byte-for-byte reproducible.
    data = bytearray(gzip.compress(b"\x0a\x00\x00" + _payload(10, root), mtime=0))
    data[9] = 255  # Stable "unknown OS" header across Python 3.11+ and operating systems.
    path.write_bytes(data)
