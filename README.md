# Riftborn 1.5 — Rift Ascension

A survival adventure mod for **Minecraft Java Edition 1.21.1**, **Fabric**, and **Java 21**.

Mine purple-veined Rift Stone, follow the signals of ruined dimensional anchors, and cross into a dark sky full of floating islands. Hunt Rift creatures, awaken **The Rift Guardian**, and forge its Heart into a **Riftblade** that lets you blink through open space.

![The Rift and Guardian shrine in Minecraft](docs/images/the-rift.png)

*Actual in-game capture from the automated client/server test; the shrine is placed as a test exhibition.*

## Requirements

| Component | Supported / pinned version |
| --- | --- |
| Minecraft Java Edition | **1.21.1** (not 1.21.2+) |
| Java | **21** |
| Fabric Loader | **0.16.10** or a compatible newer version |
| Fabric API | **0.116.6+1.21.1**; use the **1.21.1** build |
| Development mappings | Yarn **1.21.1+build.3** |
| Build tooling | Fabric Loom **1.8.13**, Gradle **8.10.2** |

Riftborn is required on **both the client and the server**. It is not a Forge/NeoForge mod. No additional content libraries are required.

## Installation

1. Install Java 21 and the Fabric Loader profile for Minecraft **1.21.1**.
2. Put the matching **Fabric API** jar in the instance's `mods` directory.
3. Put **`riftborn-1.5.0.jar`** in the same directory.
4. Start the Fabric instance. For a dedicated server, put both jars in that server's `mods` directory too.

Back up existing worlds first. Ore and Overworld ruins appear in **newly generated chunks**. Do not remove Riftborn from a save while players or important builds are in The Rift; removing content mods can damage modded saves.

### Getting the jar

Download **`riftborn-1.21.1`** from **Actions → Build and verify Riftborn → a successful run → Artifacts**. GitHub packages the artifact as a ZIP containing just **`riftborn-1.5.0.jar`** at its root. Extract that jar and put it in `mods`; no build command is needed. The artifact name identifies Minecraft 1.21.1; the jar name identifies Riftborn version 1.5.0.

The separate **`riftborn-1.21.1-diagnostics`** artifact contains test reports, logs, checksums, and screenshots—not the installation jar. To build locally with `gradlew.bat`, use the full repository checkout as described below, not the compiled-jar artifact.

## Survival progression

1. **Discover Rift Stone.** Small, rare veins generate in the Overworld between **Y −56 and Y 24**. Use an **iron pickaxe or better**. The block drops itself.
2. **Collect Rift Shards.** Defeat Rift Stalkers, or smelt/blast Rift Stone into shards. Stalkers are uncommon in dark Overworld areas; ruined structures also contain guardians and loot.
3. **Craft a Rift Core and Rift Compass.** Save enough material for **another Core** to summon the boss later.
4. **Follow a Rift signal.** Right-click the Rift Compass in the Overworld. It tracks nearby actual anchors (including your arrival portal) and surveys unexplored ruins, reporting **X/Z coordinates, approximate distance, and direction**, and sends a particle trail toward it. The locator can discover structures in unexplored terrain. It is intentionally a coordinate locator, **not a rotating compass needle**.
5. **Stabilize an anchor.** Find the rune-covered **Rift Anchor** inside a damaged ruin and use a **Rift Core** on it. One Core is consumed, the crossing is permanently stabilized for everyone, and you enter The Rift. Subsequent crossings through that anchor are free.
6. **Explore The Rift.** Bring a bow, shield, armor, food, torches, and plenty of bridging blocks. The dimension has nearby floating islands, blackstone undersides, Rift Stone surfaces and spires, glowing Void Blooms, purple motes, and its own starry purple sky. The void is real: watch your footing.
7. **Hunt and loot.** Stalkers drop shards, Brutes drop Void Fragments, and Wisps drop Rift Dust. Ruins contain supplies and Rift materials. Two Void Blooms make dust; four dust make a shard.
8. **Find a Guardian shrine.** **Sneak-right-click the compass while in The Rift** to locate one specifically. Ordinary right-click locates the nearest ruin/shrine Rift signal.
9. **Awaken The Rift Guardian.** Use another **Rift Core** on the raised **Guardian Altar**. The boss has 280 health, melee attacks, energy bolts, teleportation, limited Wisp summons, and a faster second phase below half health. A purple ring and boss-bar warning telegraph its shockwave: **jump or retreat**. Attacks do not destroy the arena. The boss is not summoned in Peaceful, and another cannot be summoned while one is alive within 96 blocks.
10. **Claim the Rift Heart and craft the Riftblade.** The Guardian always drops one Heart, additional materials, and experience; its summoned Wisps dissipate. Altars can be used again with another Core after a defeat or a failed attempt.

11. **Ascend with Rift Armor.** Craft all four pieces from Void Fragments and Rift Cores, with a Rift Heart for the chestplate. A complete worn set grants **Rift Flight** without changing your game mode. Another Guardian defeat supplies the Heart if you already used the first for a Riftblade.

### Rift Armor and Rift Flight

| Piece | Protection | Durability | Crafting materials |
| --- | ---: | ---: | --- |
| Rift Helmet | 3 | 495 | 5 Void Fragments + 1 Rift Core |
| Rift Chestplate | 8 | 720 | 7 Void Fragments + 1 Rift Core + 1 Rift Heart |
| Rift Leggings | 6 | 675 | 7 Void Fragments + 1 Rift Core |
| Rift Boots | 3 | 585 | 4 Void Fragments + 1 Rift Core |

The complete set provides **20 armor points**, **3 armor toughness per piece**, and **10% knockback resistance per piece**. Repair pieces with Void Fragments. Standard armor enchantments and trims are supported. Each piece has an original inventory icon, and the fitted vanilla biped armor models use custom dark/purple outer and leggings textures.

**Full-set bonus: Rift Flight**

- Wear the Rift Helmet, Chestplate, Leggings, and Boots in their respective armor slots. Holding a piece or mixing in another armor type does not count.
- In **Survival or Adventure**, double-tap Jump to toggle flight, just as in Creative. Use Jump to ascend, Sneak to descend, and your normal movement controls to steer. Releasing movement input brakes normally.
- Flight speed is **three times normal Creative flight** (`0.15` versus the normal `0.05`). This does not grant Creative mode, invulnerability, building permissions, or noclip.
- A quiet takeoff sound and a small periodic trail of Rift motes accompany flight.
- Removing or breaking **any** piece revokes the armor's permission and speed boost on the server. Land before removing armor: gravity and ordinary fall/void hazards resume.
- Death, respawn, dimension transfers, reconnects, and game-mode changes recheck the actual equipped set. Saves contain baseline capabilities, not a permanent Survival flight flag; resuming saved flight requires the complete set again.
- **Creative and Spectator keep their normal native flight and speed**, with or without armor. Independent flight permissions granted by other server systems are not confiscated when Riftborn removes its own bonus.
- **The Riftblade is unchanged.** Even while wearing flying armor, its blink still needs a clear path and a supported landing. Flight does not bypass its void-safety check.

Armor recipes (`F` = Void Fragment, `C` = Rift Core, `H` = Rift Heart, `.` = empty slot):

```text
Helmet       Chestplate   Leggings     Boots
F F F        F C F        F F F        F . F
F C F        F H F        F C F        F C F
             F F F        F . F
```

### Rift-only structure placement fix

Rift ruins and Guardian shrines now validate island terrain across their footprint and look for a nearby suitable island when the initial position is over void. The foundation follows the highest sampled terrain across that footprint, rather than treating a zero-height void column as ground. Vanilla jigsaw pieces, structure templates, loot, mobs, anchors, altars, random rotations, placement spacing, separation, and salts are retained.

The old Rift configuration's dimension padding did not validate the root piece: a normal-server reproduction placed roots at **Y = −1** when the sampled surface was **0**. The new `riftborn:rift_surface` placement type is used **only by the two Rift structures**. **Overworld structure generation is unchanged**, as are the dimension's island-noise generator and the Riftblade implementation.

This generation fix applies to **newly generated chunks**. Existing saved structures and player builds are not deleted, relocated, or regenerated. Existing valid shrines/anchors remain usable; already-generated misplaced structures are not retroactively moved. Back up saves before updating.

### Returning home

**Use any Rift Anchor in The Rift with an empty hand.** Return trips require no Core and lead to your own saved Overworld entry anchor, not another player's. Your return location survives disconnects and server restarts. A safe arrival platform and return anchor are created once on your first entry; they are not rebuilt over player builds on every visit.

Dismount before crossing, and allow the normal portal cooldown to settle between trips. Travel checks for collision-free footing. If your original landing is obstructed, the mod attempts a safe landing near Overworld spawn instead. Beds and charged respawn anchors **explode in The Rift**; they cannot set your respawn point here. Death otherwise follows normal Minecraft rules.

### Recipes

| Result | Recipe |
| --- | --- |
| Rift Shard | Smelt/blast 1 Rift Stone; or shapeless 4 Rift Dust |
| Rift Dust | Shapeless 2 Void Blooms |
| Rift Core | Rift Stone in the four corners, Rift Shards on the four edges, Ender Pearl in the center |
| Rift Compass | Vanilla Compass in the center, Rift Shards on the four edges |
| Riftblade | Vertical column: Rift Heart → Void Fragment → Rift Core |
| Rift Armor | Shaped recipes shown above; Void Fragments, Rift Cores, and a Heart for the chestplate |

Recipes unlock in the vanilla recipe book when you obtain their relevant materials. Riftborn also includes a small advancement tree.

### The Riftblade

- **10 attack damage**, **1.6 attack speed**, and **2,031 durability** before enchantments.
- Repair it using **Void Fragments**. Standard sword enchantments are supported.
- Right-click to blink horizontally forward: default **8 blocks**, **5-second cooldown**, **2 durability** per successful use.
- The server checks the player's **entire bounding box along the path**, including headroom, thin obstacles, world boundaries, fluids, and the destination's footing. It stops before obstructions and will not leave you over the void.
- You cannot blink while riding, sleeping, or gliding. A failed blink has a brief retry delay. Teleportation, durability, particles, sound, and cooldown are server-authoritative and synchronized using vanilla networking.

### Creatures

| Mob | Health | Behavior | Drop |
| --- | ---: | --- | --- |
| Rift Stalker | 28 | Fast melee, occasional short teleport | 1–3 Rift Shards |
| Void Brute | 100 | Slow, armored, 12 melee damage and heavy knockback | 1–2 Void Fragments |
| Rift Wisp | 20 | Flies around targets, keeps its distance, fires dodgeable bolts | 1–3 Rift Dust |
| The Rift Guardian | 280 | Persistent altar boss with a boss bar and two phases | Rift Heart + materials |

Ordinary mob drops benefit from Looting. Ruins use vanilla **jigsaw structures, template pools, biome tags, and random-spread structure sets**—there is no per-chunk placement hook. The one exception is the one-time portal arrival platform, for safe survival access.

## Development and building

Install a **JDK 21** (not just a JRE). Confirm `java -version` reports 21 and set `JAVA_HOME` if necessary.

```sh
git clone https://github.com/benluvzbacon/Minecraft-test.git
cd Minecraft-test
# If these changes are not yet merged, use the Riftborn working branch:
git checkout arena/01a07434-minecraft-test
./gradlew build
```

Windows PowerShell: from the full repository's root, run `.\gradlew.bat build`. A system Gradle installation is **not** required. The wrapper is included and verifies the Gradle distribution checksum. The first build requires internet access to download Gradle, Minecraft, and Fabric dependencies.

Outputs:

```text
build/libs/riftborn-1.5.0.jar          ← install this
build/libs/riftborn-1.5.0-sources.jar  ← development sources
```

Other useful commands:

```sh
./gradlew runClient       # development client
./gradlew runServer       # development dedicated server; read/accept its EULA first
./gradlew test            # resource integrity unit tests
./gradlew runGametest     # in-game, dedicated-server regression tests
./gradlew clean build     # full clean rebuild
```

See [docs/TESTING.md](docs/TESTING.md) for the headless multiplayer test, CI details, and verification scope. The [1.0.0 verification record](docs/VERIFICATION.md) is retained as the baseline; Ascension adds armor/flight, lifecycle, preservation, and Rift-placement regression checks.

### Configuration

The first start creates **`config/riftborn.json`** automatically. Settings take effect after restarting the server or single-player instance. Server values govern gameplay; clients cannot bypass them.

```json
{
  "blinkCooldownTicks": 100,
  "blinkDistance": 8.0,
  "compassCooldownTicks": 100,
  "locateRadius": 32,
  "overworldStalkerWeight": 6
}
```

Twenty ticks are one second at normal server speed. `locateRadius` is the vanilla random-spread structure search radius (in **structure-placement regions**, not a distance in blocks). Higher values can increase server search time. `overworldStalkerWeight: 0` disables additional natural Overworld Stalkers, but not creatures already contained in ruins. Invalid/out-of-range configuration falls back to defaults or is safely clamped. Structure spacing, loot, and terrain can also be adjusted through normal datapacks.

### Project layout

```text
src/main/java/dev/riftborn/
  registry/    blocks, items, entities, attributes, spawn restrictions, particles
  block/       anchors, Guardian altars, Void Blooms
  item/        compass, Riftblade, tool material and tooltips
  entity/      hostile mobs, Guardian, energy projectiles
  dimension/   dimension key, safe crossings, saved per-player return points
  effect/      unchanged collision-safe teleport utilities, server effects, and reversible armor flight
  world/       unchanged Overworld hooks, plus Rift-only surface-aware jigsaw placement
  mixin/       server flight packet validation and capability save/game-mode lifecycle hooks
  config/      bounded, server-side configuration
src/client/java/dev/riftborn/
  client/         client initializer, purple sky, particle factory
  entity/client/  original animated models and renderers
src/main/resources/    committed textures, models, loot, recipes, worldgen and NBT templates
src/test/              file-level resource checks
src/gametest/          development-only server test mod
src/smoketest/         development-only multiplayer client test mod
src/worldtest/         development-only normal-server terrain and capability checks
scripts/               reproducible asset generators and optional integration harness
```

Client code is isolated with Loom's split source sets. Test mods are **not bundled** into the installation jar. Ascension uses small server-side mixins to validate flight packets and saved capabilities. It does not patch Overworld generation or the Riftblade. No external shaders or custom network protocols are needed.

### Asset regeneration

All necessary resources are already committed; Python is **not needed to build the mod**. To edit/regenerate the original placeholder-style pixel art, data files, or structure layouts with Python 3:

```sh
python3 scripts/generate_assets.py
python3 scripts/generate_data.py
python3 scripts/generate_structures.py
```

The generators use only Python's standard library and fixed seeds. Textures and models are original to this project. Audio deliberately reuses registered **vanilla sound events**, so there are no missing custom sound files or separate sound downloads.

## License

Riftborn code, generated textures, and structure designs are available under the [MIT License](LICENSE). Minecraft and the vanilla graphics shown in the documentation screenshot belong to Mojang/Microsoft. Riftborn is an independent, unofficial mod, not an official Minecraft product.
