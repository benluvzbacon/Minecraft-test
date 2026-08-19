#!/bin/sh
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${CUBIOMES:-/tmp/cubiomes}"
if [ ! -f "$SRC/libcubiomes.a" ]; then
  echo "clone and build cubiomes first: git clone https://github.com/Cubitect/cubiomes $SRC && make -C $SRC"
  exit 1
fi
gcc -O2 -fwrapv -I"$SRC" "$ROOT/native/biome_tool.c" "$SRC/libcubiomes.a" -lm -o "$ROOT/native/biome_tool"
echo "built $ROOT/native/biome_tool"
