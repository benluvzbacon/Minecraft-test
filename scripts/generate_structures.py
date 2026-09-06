#!/usr/bin/env python3
"""Build original compressed structure templates for vanilla jigsaw world generation."""
import math
import random
from resource_tools import RES, ROOT, byte, integer, float_tag, string, list_tag, compound, write_nbt


class Template:
    def __init__(self, size):
        self.size = size
        self.blocks = {}
        self.entities = []

    def set(self, x, y, z, name, properties=None, nbt=None):
        assert 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]
        self.blocks[x, y, z] = (name, properties or {}, nbt)

    def clear(self):
        for x in range(self.size[0]):
            for y in range(self.size[1]):
                for z in range(self.size[2]):
                    self.set(x, y, z, "minecraft:air")

    def chest(self, x, y, z, table):
        self.set(x, y, z, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"},
                 {"id": string("minecraft:chest"), "LootTable": string("riftborn:chests/" + table)})

    def mob(self, x, y, z, name, health):
        data = {"id": string("riftborn:" + name), "Pos": list_tag(6, [x, y, z]), "Motion": list_tag(6, [0., 0., 0.]),
                "Rotation": list_tag(5, [0., 0.]), "Health": float_tag(health), "PersistenceRequired": byte(1)}
        self.entities.append({"pos": list_tag(6, [x, y, z]), "blockPos": list_tag(3, [math.floor(x), math.floor(y), math.floor(z)]), "nbt": compound(data)})

    def write(self, path):
        palette = []
        palette_index = {}
        blocks = []
        for pos, (name, properties, nbt) in sorted(self.blocks.items()):
            key = (name, tuple(sorted(properties.items())))
            if key not in palette_index:
                palette_index[key] = len(palette)
                state = {"Name": string(name)}
                if properties: state["Properties"] = compound({k: string(v) for k, v in properties.items()})
                palette.append(state)
            block = {"pos": list_tag(3, list(pos)), "state": integer(palette_index[key])}
            if nbt is not None: block["nbt"] = compound(nbt)
            blocks.append(block)
        write_nbt(path, {"DataVersion": integer(3955), "size": list_tag(3, list(self.size)),
                        "palette": list_tag(10, palette), "blocks": list_tag(10, blocks), "entities": list_tag(10, self.entities)})


def ruin(name, seed, in_rift=False):
    rng = random.Random(seed)
    t = Template((17, 10, 17))
    t.clear()
    for x in range(17):
        for z in range(17):
            if (x in (0, 16) or z in (0, 16)) and rng.random() < .4:
                continue
            material = "riftborn:rift_stone" if x in (7, 8, 9) or z in (7, 8, 9) or (x + z) % 9 == 0 else rng.choice([
                "minecraft:polished_blackstone_bricks", "minecraft:cracked_polished_blackstone_bricks", "minecraft:cobbled_deepslate"])
            t.set(x, 0, z, material)
    # Broken walls, never a sealed box. The front entrance is three blocks wide.
    for x in range(2, 15):
        for z in (2, 14):
            if z == 2 and 6 <= x <= 10: continue
            for y in range(1, rng.choice((2, 3, 4))):
                t.set(x, y, z, rng.choice(["riftborn:rift_stone", "minecraft:cracked_polished_blackstone_bricks"]))
    for z in range(3, 14):
        for x in (2, 14):
            if 7 <= z <= 9: continue
            for y in range(1, rng.choice((2, 3, 4))):
                t.set(x, y, z, "minecraft:polished_blackstone_bricks")
    for x, z, height in [(3, 3, 6), (13, 3, 4), (3, 13, 5), (13, 13, 7)]:
        for y in range(1, height):
            t.set(x, y, z, "riftborn:rift_stone" if y % 2 else "minecraft:polished_blackstone_bricks")
        t.set(x, height, z, "minecraft:crying_obsidian")
    # Damaged ritual arch around a permanent, recognizable anchor.
    for x in (6, 10):
        for y in range(1, 6): t.set(x, y, 9, "riftborn:rift_stone")
    for x in range(6, 11):
        if x != 9: t.set(x, 6, 9, "riftborn:rift_stone")
    t.set(8, 1, 9, "riftborn:rift_anchor", {"open": "true" if in_rift else "false"})
    t.chest(4, 1, 11, name)
    t.chest(12, 1, 11, name)
    for x, z in [(4, 4), (12, 4), (4, 12), (11, 13)]:
        t.set(x, 0, z, "riftborn:rift_stone")
        t.set(x, 1, z, "riftborn:void_bloom")
    t.mob(5.5, 1., 7.5, "rift_stalker", 28)
    if in_rift: t.mob(11.5, 3., 6.5, "rift_wisp", 20)
    else: t.mob(11.5, 1., 7.5, "rift_stalker", 28)
    t.write(RES / f"data/riftborn/structure/{name}.nbt")


def shrine():
    t = Template((33, 14, 33))
    rng = random.Random(808)
    t.clear()
    for x in range(33):
        for z in range(33):
            r = math.hypot(x - 16, z - 16)
            if r > 16: continue
            t.set(x, 0, z, "riftborn:rift_stone" if 10.5 < r < 12.5 or abs(x - 16) <= 1 or abs(z - 16) <= 1 else
                  rng.choice(["minecraft:polished_blackstone_bricks", "minecraft:cracked_polished_blackstone_bricks"]))
            if 15 < r <= 16 and abs(x - 16) > 2 and abs(z - 16) > 2:
                t.set(x, 1, z, "riftborn:rift_stone")
    for x, z, height in [(6, 6, 7), (26, 6, 9), (6, 26, 6), (26, 26, 8), (3, 16, 5), (29, 16, 5)]:
        for y in range(1, height):
            for dx, dz in [(0, 0), (1, 0), (0, 1), (1, 1)]:
                t.set(x + dx, y, z + dz, "riftborn:rift_stone" if y % 3 else "minecraft:crying_obsidian")
        t.set(x, height, z, "minecraft:amethyst_block")
    for x in range(14, 19):
        for z in range(14, 19): t.set(x, 1, z, "riftborn:rift_stone")
    t.set(16, 2, 16, "riftborn:guardian_altar")
    t.set(16, 1, 4, "riftborn:rift_anchor", {"open": "true"})
    t.chest(9, 1, 16, "guardian_shrine")
    t.chest(23, 1, 16, "guardian_shrine")
    for x, z in [(9, 9), (23, 9), (9, 23), (23, 23)]:
        t.set(x, 0, z, "riftborn:rift_stone")
        t.set(x, 1, z, "riftborn:void_bloom")
    t.mob(16.5, 1., 25.5, "void_brute", 100)
    t.write(RES / "data/riftborn/structure/guardian_shrine.nbt")


def test_arena():
    t = Template((16, 8, 16))
    t.clear()
    for x in range(16):
        for z in range(16): t.set(x, 0, z, "riftborn:rift_stone")
    t.write(ROOT / "src/gametest/resources/data/riftborn_test/structure/arena.nbt")


if __name__ == "__main__":
    ruin("overworld_ruin", 807)
    ruin("rift_ruin", 809, True)
    shrine()
    test_arena()
    print("Generated three jigsaw structure templates and the GameTest arena.")
