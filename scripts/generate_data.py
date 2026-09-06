#!/usr/bin/env python3
"""Generate the checked-in 1.21.1 datapack: singular registry paths are intentional."""
from resource_tools import write_json

D = "data/riftborn/"
A = "assets/riftborn/"


def uniform(minimum, maximum):
    return {"type": "minecraft:uniform", "min": minimum, "max": maximum}


def entry(item, minimum=1, maximum=1, weight=1, looting=False):
    result = {"type": "minecraft:item", "name": item, "weight": weight,
              "functions": [{"function": "minecraft:set_count", "count": uniform(minimum, maximum)}]}
    if looting:
        result["functions"].append({"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
                                    "count": uniform(0, 1), "limit": maximum + 3})
    return result


def pool(entries, rolls=1):
    return {"rolls": rolls, "bonus_rolls": 0, "entries": entries}


def tag(namespace, registry, name, values):
    write_json(f"data/{namespace}/tags/{registry}/{name}.json", {"replace": False, "values": values})


def generate_loot_and_recipes():
    for name in ("rift_stone", "void_bloom", "rift_anchor", "guardian_altar"):
        pools = [] if name in ("rift_anchor", "guardian_altar") else [{
            **pool([{"type": "minecraft:item", "name": f"riftborn:{name}"}]),
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]
        write_json(D + f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": pools})
    for name, item, minimum, maximum in [("rift_stalker", "rift_shard", 1, 3), ("void_brute", "void_fragment", 1, 2), ("rift_wisp", "rift_dust", 1, 3)]:
        write_json(D + f"loot_table/entities/{name}.json", {"type": "minecraft:entity", "pools": [pool([entry(f"riftborn:{item}", minimum, maximum, looting=True)])]})
    write_json(D + "loot_table/entities/rift_guardian.json", {"type": "minecraft:entity", "pools": [
        pool([entry("riftborn:rift_heart")]), pool([entry("riftborn:rift_shard", 6, 10)]), pool([entry("riftborn:void_fragment", 3, 5)])]})
    for name, rare in [("overworld_ruin", False), ("rift_ruin", True), ("guardian_shrine", True)]:
        items = [entry("riftborn:rift_shard", 2, 5, 8), entry("minecraft:ender_pearl", 1, 2, 4), entry("minecraft:bread", 3, 6, 5),
                 entry("minecraft:iron_ingot", 2, 6, 4), entry("minecraft:obsidian", 2, 5, 3), entry("riftborn:rift_core", 1, 1, 1)]
        if rare:
            items += [entry("riftborn:void_fragment", 1, 3, 4), entry("riftborn:rift_dust", 3, 7, 6), entry("minecraft:golden_apple", 1, 1, 2)]
        if name == "guardian_shrine": items.append(entry("minecraft:diamond", 1, 3, 2))
        write_json(D + f"loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [
            pool([entry("riftborn:rift_shard", 2, 4)]), pool(items, uniform(3, 5))]})

    recipes = {
        "rift_core": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SRS", "RER", "SRS"],
                      "key": {"S": {"item": "riftborn:rift_stone"}, "R": {"item": "riftborn:rift_shard"}, "E": {"item": "minecraft:ender_pearl"}},
                      "result": {"id": "riftborn:rift_core", "count": 1}},
        "rift_compass": {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" R ", "RCR", " R "],
                         "key": {"R": {"item": "riftborn:rift_shard"}, "C": {"item": "minecraft:compass"}}, "result": {"id": "riftborn:rift_compass", "count": 1}},
        "riftblade": {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" H ", " F ", " C "],
                      "key": {"H": {"item": "riftborn:rift_heart"}, "F": {"item": "riftborn:void_fragment"}, "C": {"item": "riftborn:rift_core"}},
                      "result": {"id": "riftborn:riftblade", "count": 1}},
        "rift_shard_from_dust": {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": "riftborn:rift_dust"}] * 4,
                                "result": {"id": "riftborn:rift_shard", "count": 1}},
        "rift_dust_from_blooms": {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": "riftborn:void_bloom"}] * 2,
                                 "result": {"id": "riftborn:rift_dust", "count": 1}},
    }
    armor = ("rift_helmet", "rift_chestplate", "rift_leggings", "rift_boots")
    for name, pattern in zip(armor, (["FFF", "FCF"], ["FCF", "FHF", "FFF"], ["FFF", "FCF", "F F"], ["F F", "FCF"])):
        key = {"F": {"item": "riftborn:void_fragment"}, "C": {"item": "riftborn:rift_core"}}
        if name == "rift_chestplate": key["H"] = {"item": "riftborn:rift_heart"}
        recipes[name] = {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern,
                         "key": key, "result": {"id": f"riftborn:{name}", "count": 1}}
    for furnace, ticks in [("smelting", 200), ("blasting", 100)]:
        recipes["rift_shard_from_" + furnace] = {"type": "minecraft:" + furnace, "category": "misc", "ingredient": {"item": "riftborn:rift_stone"},
                                               "result": {"id": "riftborn:rift_shard", "count": 1}, "experience": 0.7, "cookingtime": ticks}
    for name, recipe in recipes.items():
        write_json(D + f"recipe/{name}.json", recipe)
        trigger_item = {"riftblade": "rift_heart", "rift_dust_from_blooms": "void_bloom", "rift_shard_from_dust": "rift_dust"}.get(name, "rift_shard" if name == "rift_compass" else "rift_stone")
        if name in armor: trigger_item = "void_fragment"
        write_json(D + f"advancement/recipes/{name}.json", {"parent": "minecraft:recipes/root", "criteria": {
            "has_material": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"riftborn:{trigger_item}"}]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": f"riftborn:{name}"}}},
            "requirements": [["has_material", "has_the_recipe"]], "rewards": {"recipes": [f"riftborn:{name}"]}})
    tag("minecraft", "block", "mineable/pickaxe", ["riftborn:rift_stone", "riftborn:rift_anchor", "riftborn:guardian_altar"])
    tag("minecraft", "block", "needs_iron_tool", ["riftborn:rift_stone"])
    for name in ("dragon_immune", "wither_immune"):
        tag("minecraft", "block", name, ["riftborn:rift_anchor", "riftborn:guardian_altar"])
    for name in ("swords", "enchantable/sword", "enchantable/weapon", "enchantable/sharp_weapon", "enchantable/durability"):
        tag("minecraft", "item", name, ["riftborn:riftblade"])

    armor_ids = ["riftborn:" + name for name in armor]
    tag("minecraft", "item", "enchantable/durability", ["riftborn:riftblade"] + armor_ids)
    for group in ("armor", "enchantable/armor", "enchantable/equippable", "trimmable_armor"):
        tag("minecraft", "item", group, armor_ids)
    for slot, name in zip(("head", "chest", "leg", "foot"), armor):
        tag("minecraft", "item", slot + "_armor", ["riftborn:" + name])
        tag("minecraft", "item", "enchantable/" + slot + "_armor", ["riftborn:" + name])


def generate_worldgen():
    write_json(D + "worldgen/configured_feature/rift_stone_ore.json", {"type": "minecraft:ore", "config": {
        "size": 5, "discard_chance_on_air_exposure": 0.5, "targets": [
            {"target": {"predicate_type": "minecraft:tag_match", "tag": f"minecraft:{base}_ore_replaceables"}, "state": {"Name": "riftborn:rift_stone"}}
            for base in ("stone", "deepslate")]}})
    write_json(D + "worldgen/placed_feature/rift_stone_ore.json", {"feature": "riftborn:rift_stone_ore", "placement": [
        {"type": "minecraft:count", "count": 2}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid", "min_inclusive": {"absolute": -56}, "max_inclusive": {"absolute": 24}}},
        {"type": "minecraft:biome"}]})
    write_json(D + "worldgen/configured_feature/rift_spires.json", {"type": "minecraft:block_column", "config": {
        "direction": "up", "allowed_placement": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air"]}, "prioritize_tip": True,
        "layers": [{"height": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 6},
                    "provider": {"type": "minecraft:simple_state_provider", "state": {"Name": "riftborn:rift_stone"}}},
                   {"height": 1, "provider": {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:amethyst_block"}}}]}})
    placement = [{"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"},
                 {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
                 {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": ["riftborn:rift_stone", "minecraft:blackstone"]}},
                 {"type": "minecraft:biome"}]
    write_json(D + "worldgen/placed_feature/rift_spires.json", {"feature": "riftborn:rift_spires", "placement": placement})
    write_json(D + "worldgen/configured_feature/void_bloom.json", {"type": "minecraft:simple_block", "config": {
        "to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": "riftborn:void_bloom"}}}})
    write_json(D + "worldgen/placed_feature/void_bloom.json", {"feature": "riftborn:void_bloom", "placement": [
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air"]}, {"type": "minecraft:would_survive", "state": {"Name": "riftborn:void_bloom"}}]}}]})
    write_json(D + "worldgen/configured_feature/void_bloom_patch.json", {"type": "minecraft:random_patch", "config": {
        "tries": 32, "xz_spread": 7, "y_spread": 3, "feature": "riftborn:void_bloom"}})
    write_json(D + "worldgen/placed_feature/void_bloom_patch.json", {"feature": "riftborn:void_bloom_patch", "placement": [
        {"type": "minecraft:count", "count": 4}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})

    # A vertical density envelope intersected with horizontal multi-octave noise produces
    # many nearby floating islands, without the End generator's 1000-block empty ring.
    def binary(kind, a, b): return {"type": "minecraft:" + kind, "argument1": a, "argument2": b}
    def gradient(fy, ty, fv, tv): return {"type": "minecraft:y_clamped_gradient", "from_y": fy, "to_y": ty, "from_value": fv, "to_value": tv}
    density = binary("add", binary("min", gradient(0, 64, -2, 0.18), gradient(64, 128, 0.18, -2)),
                     binary("add", binary("mul", 1.7, {"type": "minecraft:cache_2d", "argument": {
                         "type": "minecraft:noise", "noise": "riftborn:island_shape", "xz_scale": 1, "y_scale": 0}}),
                            binary("mul", 0.14, {"type": "minecraft:noise", "noise": "minecraft:surface", "xz_scale": 0.4, "y_scale": 0.8})))
    write_json(D + "worldgen/noise/island_shape.json", {"firstOctave": -6, "amplitudes": [1.0, 1.0, 0.5]})
    write_json(D + "worldgen/density_function/floating_islands.json", density)
    router = {name: 0.0 for name in ["barrier", "fluid_level_floodedness", "fluid_level_spread", "lava", "temperature", "vegetation", "continents", "erosion", "depth", "ridges", "vein_toggle", "vein_ridged", "vein_gap"]}
    router["initial_density_without_jaggedness"] = "riftborn:floating_islands"
    router["final_density"] = {"type": "minecraft:squeeze", "argument": {"type": "minecraft:interpolated", "argument": "riftborn:floating_islands"}}
    write_json(D + "worldgen/noise_settings/the_rift.json", {"sea_level": 0, "disable_mob_generation": False, "aquifers_enabled": False,
        "ore_veins_enabled": False, "legacy_random_source": False, "default_block": {"Name": "minecraft:blackstone"},
        "default_fluid": {"Name": "minecraft:air"}, "noise": {"min_y": 0, "height": 128, "size_horizontal": 2, "size_vertical": 1},
        "noise_router": router, "spawn_target": [], "surface_rule": {"type": "minecraft:condition", "if_true": {
            "type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"},
            "then_run": {"type": "minecraft:block", "result_state": {"Name": "riftborn:rift_stone"}}}})
    features = [[] for _ in range(11)]
    features[2] = ["riftborn:rift_spires"]
    features[9] = ["riftborn:void_bloom_patch"]
    spawns = [{"type": "riftborn:" + name, "weight": weight, "minCount": 1, "maxCount": count} for name, weight, count in
              [("rift_stalker", 55, 2), ("void_brute", 12, 1), ("rift_wisp", 30, 2)]]
    write_json(D + "worldgen/biome/void_reaches.json", {"has_precipitation": False, "temperature": 0.5, "downfall": 0,
        "effects": {"fog_color": 0x201034, "sky_color": 0x1D0A36, "water_color": 0x7542B0, "water_fog_color": 0x29113C,
                    "foliage_color": 0x9762BD, "grass_color": 0x69428B,
                    "particle": {"options": {"type": "riftborn:rift_mote"}, "probability": 0.007},
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2},
                    "music": {"sound": "minecraft:music.end", "min_delay": 12000, "max_delay": 24000, "replace_current_music": False}},
        "spawners": {"monster": spawns, "creature": [], "ambient": [], "axolotls": [], "underground_water_creature": [], "water_creature": [], "water_ambient": [], "misc": []},
        "spawn_costs": {}, "carvers": {}, "features": features})
    write_json(D + "dimension_type/the_rift.json", {"ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": False,
        "has_ceiling": False, "ambient_light": 0.14, "fixed_time": 18000, "piglin_safe": False, "bed_works": False,
        "respawn_anchor_works": False, "has_raids": False, "logical_height": 256, "min_y": 0, "height": 256,
        "infiniburn": "#minecraft:infiniburn_end", "effects": "riftborn:the_rift", "monster_spawn_block_light_limit": 0,
        "monster_spawn_light_level": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7}})
    write_json(D + "dimension/the_rift.json", {"type": "riftborn:the_rift", "generator": {"type": "minecraft:noise",
        "settings": "riftborn:the_rift", "biome_source": {"type": "minecraft:fixed", "biome": "riftborn:void_reaches"}}})

    for name, spacing, separation, salt in [("overworld_ruin", 32, 10, 4197231), ("rift_ruin", 18, 6, 4197243), ("guardian_shrine", 28, 10, 4197269)]:
        structure = {"type": "minecraft:jigsaw", "biomes": f"#riftborn:has_structure/{name}", "step": "surface_structures",
                     "spawn_overrides": {}, "terrain_adaptation": "beard_thin", "start_pool": f"riftborn:{name}", "size": 1,
                     "start_height": {"absolute": 0}, "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": 80,
                     "use_expansion_hack": False}
        if name != "overworld_ruin":
            # Only Rift structures use surface-aware placement. The Overworld JSON,
            # pools, templates, placement salts, and frequencies remain unchanged.
            structure["type"] = "riftborn:rift_surface"
            for key in ("start_height", "project_start_to_heightmap", "max_distance_from_center", "use_expansion_hack"):
                structure.pop(key)
            structure["surface_search_radius"] = 48 if name == "rift_ruin" else 64
        write_json(D + f"worldgen/structure/{name}.json", structure)
        write_json(D + f"worldgen/structure_set/{name}.json", {"structures": [{"structure": f"riftborn:{name}", "weight": 1}],
                   "placement": {"type": "minecraft:random_spread", "salt": salt, "spacing": spacing, "separation": separation, "spread_type": "linear"}})
        write_json(D + f"worldgen/template_pool/{name}.json", {"name": f"riftborn:{name}", "fallback": "minecraft:empty", "elements": [{
            "weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"riftborn:{name}", "processors": "minecraft:empty", "projection": "rigid"}}]})
        biomes = ["#minecraft:is_forest", "#minecraft:is_taiga", "#minecraft:is_mountain", "minecraft:plains", "minecraft:savanna", "minecraft:desert", "minecraft:swamp", "minecraft:badlands"] if name == "overworld_ruin" else ["riftborn:void_reaches"]
        tag("riftborn", "worldgen/biome", f"has_structure/{name}", biomes)
    tag("riftborn", "worldgen/structure", "rift_signals", ["riftborn:overworld_ruin", "riftborn:rift_ruin", "riftborn:guardian_shrine"])
    tag("riftborn", "worldgen/structure", "guardian_shrines", ["riftborn:guardian_shrine"])


def generate_advancements():
    steps = [("root", "rift_stone", "Beneath the Surface", "Mine Rift Stone with an iron pickaxe or better.", None),
             ("core", "rift_core", "A Fractured Key", "Craft a Rift Core to stabilize an ancient anchor.", "root"),
             ("enter_rift", "rift_compass", "Between Worlds", "Follow a Rift signal and enter The Rift.", "core"),
             ("guardian", "rift_heart", "Heart of the Void", "Defeat the Rift Guardian.", "enter_rift"),
             ("riftblade", "riftblade", "Walk the Fracture", "Forge the Riftblade from a Rift Heart.", "guardian")]
    for name, icon, title, description, parent in steps:
        display = {"icon": {"id": "riftborn:" + icon}, "title": {"translate": f"advancement.riftborn.{name}.title"},
                   "description": {"translate": f"advancement.riftborn.{name}.description"}, "frame": "challenge" if name in ("guardian", "riftblade") else "task",
                   "show_toast": True, "announce_to_chat": True, "hidden": False}
        if name == "root": display["background"] = "minecraft:textures/block/obsidian.png"
        criterion = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "riftborn:" + icon}]}}
        if name == "enter_rift": criterion = {"trigger": "minecraft:changed_dimension", "conditions": {"to": "riftborn:the_rift"}}
        if name == "guardian": criterion = {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {"type": "riftborn:rift_guardian"}}}
        advancement = {"display": display, "criteria": {"progress": criterion}, "requirements": [["progress"]]}
        if parent: advancement["parent"] = "riftborn:" + parent
        write_json(D + f"advancement/{name}.json", advancement)
    return {f"advancement.riftborn.{name}.{key}": value for name, _, title, description, _ in steps for key, value in [("title", title), ("description", description)]}


def generate_language():
    language = {
        "itemGroup.riftborn": "Riftborn", "dimension.riftborn.the_rift": "The Rift", "biome.riftborn.void_reaches": "Void Reaches",
        "block.riftborn.rift_stone": "Rift Stone", "block.riftborn.rift_anchor": "Rift Anchor", "block.riftborn.guardian_altar": "Guardian Altar", "block.riftborn.void_bloom": "Void Bloom",
        "item.riftborn.rift_shard": "Rift Shard", "item.riftborn.void_fragment": "Void Fragment", "item.riftborn.rift_dust": "Rift Dust", "item.riftborn.rift_core": "Rift Core",
        "item.riftborn.rift_heart": "Rift Heart", "item.riftborn.rift_compass": "Rift Compass", "item.riftborn.riftblade": "Riftblade",
        "item.riftborn.rift_shard.tooltip": "Dropped by Stalkers; also smelted from Rift Stone.",
        "item.riftborn.void_fragment.tooltip": "A Brute's shattered armor. Repairs the Riftblade.",
        "item.riftborn.rift_dust.tooltip": "Wisp remains. Four dust can form a Rift Shard.",
        "item.riftborn.rift_core.tooltip": "Use on a Rift Anchor or a Guardian Altar.",
        "item.riftborn.rift_heart.tooltip": "The Guardian's heart. Forge it into a Riftblade.",
        "item.riftborn.rift_compass.tooltip": "Use: locate a Rift. Sneak-use in The Rift: locate a Guardian shrine.",
        "item.riftborn.riftblade.tooltip": "Use: blink forward. Default: 8 blocks, 5-second cooldown.",
        "item.riftborn.riftblade.safety": "Requires a clear path and solid footing. Costs 2 durability.",
        "message.riftborn.blink.unavailable": "Dismount, stop gliding, or wake up before blinking.",
        "message.riftborn.blink.blocked": "No safe landing ahead. The Rift will not carry you through stone or into the void.",
        "message.riftborn.compass.wrong_dimension": "The compass resonates only in the Overworld and The Rift.",
        "message.riftborn.compass.not_found": "No Rift signal in range. Travel farther and try again.",
        "message.riftborn.compass.found": "%s signal at X %s, Z %s — about %s blocks %s.",
        "location.riftborn.shrine": "Guardian shrine", "location.riftborn.rift": "Active Rift",
        "direction.riftborn.east": "east", "direction.riftborn.west": "west", "direction.riftborn.north": "north", "direction.riftborn.south": "south", "direction.riftborn.diagonal": "%s-%s",
        "message.riftborn.anchor.core_required": "An active fracture. Use a Rift Core to stabilize this crossing permanently.",
        "message.riftborn.anchor.wait": "Dismount and wait for the crossing to settle before using the anchor again.",
        "message.riftborn.anchor.wrong_dimension": "This anchor cannot connect from this dimension.",
        "message.riftborn.anchor.dimension_missing": "The Rift dimension is unavailable. Ask the server owner to check the Riftborn datapack.",
        "message.riftborn.anchor.obstructed": "The landing is obstructed. Clear a safe space beside the destination anchor.",
        "message.riftborn.anchor.returned": "The fracture closes behind you. Welcome home.",
        "message.riftborn.anchor.arrived": "The Rift. Keep your compass close, bring bridges, and use any Rift Anchor to return home.",
        "message.riftborn.altar.core_required": "Offer a Rift Core to awaken the Guardian. Prepare armor, a bow, food, and a shield.",
        "message.riftborn.altar.wrong_dimension": "The Guardian can only be awakened in The Rift.",
        "message.riftborn.altar.peaceful": "The Guardian slumbers in Peaceful difficulty.",
        "message.riftborn.altar.occupied": "A Guardian is already awake nearby.",
        "message.riftborn.altar.obstructed": "Clear the space above the altar first.",
        "message.riftborn.altar.awakened": "The Rift Guardian awakens. Watch for the shockwave ring!",
        "boss.riftborn.pulse_warning": "The Rift Guardian — Shockwave! Jump or retreat!",
        "boss.riftborn.second_phase": "The Guardian fractures! Its attacks are faster and its Wisps multiply.",
        "boss.riftborn.defeated": "The Rift falls silent. Claim its Heart and forge the Riftblade.",
    }
    for name, title in [("rift_stalker", "Rift Stalker"), ("void_brute", "Void Brute"), ("rift_wisp", "Rift Wisp"), ("rift_guardian", "The Rift Guardian")]:
        language[f"entity.riftborn.{name}"] = title
        language[f"item.riftborn.{name}_spawn_egg"] = title + " Spawn Egg"
    language["entity.riftborn.rift_bolt"] = "Rift Bolt"
    for suffix, title in (("helmet", "Helmet"), ("chestplate", "Chestplate"), ("leggings", "Leggings"), ("boots", "Boots")):
        language[f"item.riftborn.rift_{suffix}"] = "Rift " + title
        language[f"item.riftborn.rift_{suffix}.tooltip"] = "Full Rift Armor set: Rift Flight at three times Creative flight speed."
    language["item.riftborn.rift_armor.tooltip"] = "Full set: Rift Flight. Three times normal Creative flight speed."
    language["item.riftborn.rift_armor.controls"] = "Double-tap Jump to fly. Jump/Sneak: ascend/descend. Removing any piece ends Rift Flight."
    language.update(generate_advancements())
    write_json(A + "lang/en_us.json", language)


if __name__ == "__main__":
    generate_loot_and_recipes()
    generate_worldgen()
    generate_language()
    print("Generated Riftborn recipes, loot, tags, dimensions, worldgen and translations.")
