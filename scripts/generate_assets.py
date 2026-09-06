#!/usr/bin/env python3
"""Generate original pixel textures and resource models. No third-party art or tools required."""
import math
import random
from resource_tools import Image, write_json

A = "assets/riftborn/"
DARK = (26, 18, 38)
PURPLE = (151, 74, 217)
BRIGHT = (222, 179, 255)
TEAL = (130, 251, 231)


def stone(seed=104):
    rng = random.Random(seed)
    image = Image(16, 16)
    for y in range(16):
        for x in range(16):
            n = rng.randrange(-6, 8)
            shade = 7 if y % 7 == 0 else 0
            image.pixel(x, y, (38 + n - shade, 29 + n - shade, 49 + n - shade))
    for coords in [(2, 0, 5, 4), (5, 4, 3, 8), (3, 8, 8, 11), (8, 11, 9, 15), (15, 4, 11, 7)]:
        image.line(*coords, (60, 31, 82), 2)
        image.line(*coords, (111, 55, 152))
    image.pixel(5, 4, BRIGHT)
    image.pixel(3, 8, PURPLE)
    image.pixel(12, 6, (182, 111, 230))
    return image


def generate_blocks():
    stone().save(A + "textures/block/rift_stone.png")
    for open_state in (False, True):
        image = stone(201)
        for y in range(16):
            for x in range(16):
                r = math.hypot(x - 7.5, y - 7.5)
                if 4.7 < r < 6.5:
                    image.pixel(x, y, BRIGHT if open_state else PURPLE)
                elif r < 4:
                    image.pixel(x, y, (80, 27, 123) if open_state else (19, 11, 29))
        image.line(7, 5, 7, 10, TEAL if open_state else BRIGHT, 2)
        image.line(5, 7, 10, 7, TEAL if open_state else BRIGHT, 2)
        image.save(A + f"textures/block/rift_anchor{'_open' if open_state else ''}_top.png")
    side = stone(208)
    side.rect(0, 0, 15, 1, (73, 49, 94))
    side.rect(0, 14, 15, 15, (18, 14, 26))
    for x in (3, 7, 11):
        side.line(x, 4, x, 11, PURPLE)
        side.line(x, 5, x + 1, 6, BRIGHT)
    side.save(A + "textures/block/rift_anchor_side.png")
    altar = stone(306)
    altar.rect(1, 1, 14, 14, (81, 49, 101))
    altar.rect(3, 3, 12, 12, DARK)
    for a, b, c, d in [(4, 7, 7, 4), (7, 4, 11, 7), (11, 7, 7, 11), (7, 11, 4, 7)]:
        altar.line(a, b, c, d, (236, 151, 251))
    altar.rect(7, 6, 8, 9, BRIGHT)
    altar.save(A + "textures/block/guardian_altar_top.png")
    flower = Image(16, 16)
    flower.line(7, 7, 7, 15, (86, 51, 119), 2)
    flower.line(7, 12, 3, 10, PURPLE)
    flower.line(8, 11, 12, 9, PURPLE)
    for y in range(11):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 5)
            if d < 6 and (abs(x - 7.5) < 2 or abs(y - 5) < 2 or abs(x - 7.5) + abs(y - 5) > 3):
                flower.pixel(x, y, PURPLE if d > 3 else BRIGHT)
    flower.rect(7, 4, 8, 6, TEAL)
    flower.save(A + "textures/block/void_bloom.png")

    write_json(A + "models/block/rift_stone.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "riftborn:block/rift_stone"}})
    write_json(A + "blockstates/rift_stone.json", {"variants": {"": {"model": "riftborn:block/rift_stone"}}})
    for name in ("rift_anchor", "rift_anchor_open"):
        write_json(A + f"models/block/{name}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"riftborn:block/{name}_top", "side": "riftborn:block/rift_anchor_side", "bottom": "riftborn:block/rift_stone"}})
    write_json(A + "blockstates/rift_anchor.json", {"variants": {"open=false": {"model": "riftborn:block/rift_anchor"}, "open=true": {"model": "riftborn:block/rift_anchor_open"}}})
    faces = {face: {"texture": "#top" if face == "up" else "#side"} for face in ("up", "down", "north", "south", "east", "west")}
    write_json(A + "models/block/guardian_altar.json", {"parent": "minecraft:block/block", "textures": {
        "top": "riftborn:block/guardian_altar_top", "side": "riftborn:block/rift_anchor_side", "particle": "riftborn:block/rift_stone"},
        "elements": [{"from": lo, "to": hi, "faces": faces} for lo, hi in [([0, 0, 0], [16, 4, 16]), ([2, 4, 2], [14, 12, 14]), ([0, 12, 0], [16, 16, 16])]]})
    write_json(A + "blockstates/guardian_altar.json", {"variants": {"": {"model": "riftborn:block/guardian_altar"}}})
    write_json(A + "models/block/void_bloom.json", {"parent": "minecraft:block/cross", "textures": {"cross": "riftborn:block/void_bloom"}})
    write_json(A + "blockstates/void_bloom.json", {"variants": {"": {"model": "riftborn:block/void_bloom"}}})
    for name in ("rift_stone", "rift_anchor", "guardian_altar"):
        write_json(A + f"models/item/{name}.json", {"parent": f"riftborn:block/{name}"})
    write_json(A + "models/item/void_bloom.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "riftborn:block/void_bloom"}})


def crystal(name, color, variant=0):
    image = Image(16, 16)
    for y in range(2, 15):
        for x in range(2, 14):
            half = min((y - 1) * 0.7, (15 - y) * 0.8, 4.5)
            if abs(x - (8 - (y - 8) * 0.15)) <= half:
                factor = 1.25 if x < 7 else 0.70 if x > 9 else 1.0
                image.pixel(x, y, tuple(min(255, int(c * factor)) for c in color))
    image.line(7, 4, 5, 9, BRIGHT)
    if variant:
        image.line(8, 7, 10, 10, TEAL)
    image.save(A + f"textures/item/{name}.png")


def generate_items():
    crystal("rift_shard", (147, 73, 221))
    crystal("void_fragment", (76, 50, 111), 1)
    rng = random.Random(410)
    dust = Image(16, 16)
    for _ in range(24):
        x, y = rng.randrange(2, 14), rng.randrange(3, 14)
        dust.rect(x, y, x + 1, y + 1, rng.choice([PURPLE, BRIGHT, TEAL]))
    dust.save(A + "textures/item/rift_dust.png")
    core = Image(16, 16)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r <= 6.5:
                color = DARK if r > 5.5 else BRIGHT if 4.5 < r < 5.5 else PURPLE if r > 3.5 else (35, 16, 65)
                core.pixel(x, y, color)
    core.line(7, 4, 7, 11, TEAL, 2)
    core.line(4, 7, 11, 7, BRIGHT, 2)
    core.save(A + "textures/item/rift_core.png")
    heart = Image(16, 16)
    for y in range(2, 14):
        for x in range(1, 15):
            inside = (y <= 6 and (math.hypot(x - 4.5, y - 5) < 3.6 or math.hypot(x - 10.5, y - 5) < 3.6)) or (y >= 6 and abs(x - 7.5) < 14 - y)
            if inside:
                heart.pixel(x, y, (190 - y * 4, 68 + x * 2, 228 - y * 2))
    heart.line(7, 5, 8, 7, BRIGHT)
    heart.line(8, 7, 6, 9, TEAL)
    heart.line(6, 9, 8, 12, BRIGHT)
    heart.line(3, 4, 5, 3, (251, 200, 255))
    heart.save(A + "textures/item/rift_heart.png")
    compass = Image(16, 16)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r < 7:
                compass.pixel(x, y, (131, 116, 158) if r > 5.6 else (45, 25, 67))
    compass.line(8, 2, 8, 12, BRIGHT)
    compass.line(3, 7, 12, 7, PURPLE)
    compass.line(8, 3, 11, 7, TEAL)
    compass.line(8, 3, 5, 7, TEAL)
    compass.rect(7, 7, 9, 9, (205, 128, 255))
    compass.save(A + "textures/item/rift_compass.png")
    sword = Image(16, 16)
    sword.line(3, 12, 5, 10, (81, 44, 110), 2)
    sword.rect(1, 13, 3, 15, (121, 63, 166))
    sword.pixel(2, 14, TEAL)
    sword.line(5, 9, 12, 2, (40, 28, 64), 3)
    sword.line(6, 8, 13, 1, PURPLE, 2)
    sword.line(7, 8, 14, 1, BRIGHT)
    sword.line(7, 8, 12, 3, TEAL)
    sword.line(3, 8, 8, 13, (69, 39, 90), 2)
    sword.line(3, 8, 8, 13, (181, 120, 230))
    sword.rect(5, 10, 6, 11, BRIGHT)
    sword.save(A + "textures/item/riftblade.png")
    for name in ("rift_shard", "void_fragment", "rift_dust", "rift_core", "rift_heart", "rift_compass", "riftblade"):
        write_json(A + f"models/item/{name}.json", {"parent": "minecraft:item/handheld" if name == "riftblade" else "minecraft:item/generated",
                    "textures": {"layer0": f"riftborn:item/{name}"}})
    for name in ("rift_stalker", "void_brute", "rift_wisp", "rift_guardian"):
        write_json(A + f"models/item/{name}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})


def generate_entities():
    for index, (name, base, glow) in enumerate([
        ("rift_stalker", (42, 29, 55), (202, 131, 250)),
        ("void_brute", (28, 26, 39), (134, 79, 199)),
        ("rift_guardian", (45, 36, 60), (234, 178, 255))]):
        rng = random.Random(501 + index)
        image = Image(64, 64)
        for y in range(64):
            for x in range(64):
                n = rng.randrange(-7, 8)
                color = tuple(max(0, c + n) for c in base)
                if (x + 2 * y) % 19 == 0: color = tuple(int(c * 0.6) for c in glow)
                image.pixel(x, y, color)
        # UV-space eyes, brow, chest rune, wrist bands and crown crystals.
        image.rect(8, 10, 15, 10, (15, 12, 22))
        image.rect(9, 11, 10, 12, glow)
        image.rect(13, 11, 14, 12, glow)
        image.pixel(9, 11, TEAL)
        image.pixel(13, 11, TEAL)
        image.line(26, 23, 26, 31, glow)
        image.line(23, 27, 26, 24, glow)
        image.line(26, 24, 29, 27, glow)
        image.line(23, 27, 26, 30, glow)
        image.line(26, 30, 29, 27, glow)
        for y in (29, 49): image.rect(44, y, 47, y + 1, glow)
        image.rect(0, 48, 7, 54, glow)
        image.save(A + f"textures/entity/{name}.png")
    image = Image(32, 32)
    rng = random.Random(601)
    for y in range(32):
        for x in range(32):
            image.pixel(x, y, rng.choice([(66, 33, 100), (79, 41, 128), (89, 54, 138)]))
    image.rect(8, 9, 15, 14, (160, 113, 213))
    image.rect(9, 10, 14, 13, TEAL)
    image.rect(11, 10, 12, 13, (20, 17, 34))
    image.rect(0, 18, 15, 24, BRIGHT)
    image.save(A + "textures/entity/rift_wisp.png")


def generate_sky_and_particles():
    image = Image(128, 128)
    rng = random.Random(701)
    for y in range(128):
        for x in range(128):
            # Seamless, subdued purple nebula, with deterministic sparse stars.
            waves = (math.sin(x * math.tau / 128 + math.sin(y * math.tau / 128)) + math.cos((x + y) * math.tau / 64)) * 0.5
            color = (int(22 + waves * 8), int(8 + waves * 4), int(41 + waves * 15))
            if rng.random() < 0.008: color = rng.choice([(135, 100, 169), (95, 82, 135), (171, 145, 203)])
            image.pixel(x, y, color)
    image.save(A + "textures/environment/rift_sky.png")
    particle = Image(8, 8)
    for y in range(8):
        for x in range(8):
            r = math.hypot(x - 3.5, y - 3.5)
            if r < 3.8:
                particle.pixel(x, y, (255, 255, 255, max(0, int((1 - r / 3.8) * 255))))
    particle.save(A + "textures/particle/rift_mote.png")
    write_json(A + "particles/rift_mote.json", {"textures": ["riftborn:rift_mote"]})
    icon = Image(64, 64, (20, 13, 31, 255))
    for y in range(64):
        for x in range(64):
            r = math.hypot((x - 31.5) * 1.35, (y - 31.5) * 0.85)
            if 20 < r < 26: icon.pixel(x, y, PURPLE if r > 23 else BRIGHT)
    icon.line(32, 12, 27, 31, TEAL, 3)
    icon.line(27, 31, 36, 32, BRIGHT, 3)
    icon.line(36, 32, 30, 51, TEAL, 3)
    icon.save(A + "icon.png")


if __name__ == "__main__":
    generate_blocks()
    generate_items()
    generate_entities()
    generate_sky_and_particles()
    print("Generated Riftborn PNG textures and client models.")
