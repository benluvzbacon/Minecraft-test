# Verification and development tests

## Normal build

```sh
./gradlew clean build
```

Uses the pinned Gradle wrapper, Java 21, Loom, Yarn, Loader, and Fabric API versions in `gradle.properties` / `build.gradle`. It compiles common/server code, client code, and the development-only test source sets; runs JUnit resource integrity tests; then remaps the production and source jars for distribution.

The resource tests cover:

- JSON syntax, lowercase resource paths, and the 1.21.1 singular datapack directories.
- All block/item/entity models, loot tables, translations, and referenced PNGs.
- PNG decoding and texture dimensions.
- Structure NBT parsing, palettes, dimensions, non-duplicated positions, chests, and bounded mob counts.
- Jigsaw pool/template/placement references.
- Required boss loot and the absence of a craftable Rift Heart.
- Common code does not import client-only classes.

After building, audit the actual remapped archive (requires Python 3):

```sh
python3 scripts/verify_jar.py
```

This verifies Java 21 bytecode, intermediary remapping, packaged resources and entrypoints, and exclusion of the development test mods. It writes `build/jar-verification.json` and `build/libs/SHA256SUMS`.

## Dedicated-server GameTests

```sh
./gradlew runGametest
```

This boots Fabric's GameTest server and loads the actual dynamic registries and datapacks. Tests include clear-path blinking, solid walls, thin glass panes, diagonal corner grazing between samples, headroom, lava, missing footing, authoritative cooldown and durability, recipe-manager crafting, mining/tool tags, Overworld ore-biome injection, entity attributes, dimension registration, return-point serialization, projectile impact, Stalker teleportation, Brute knockback, Wisp ranged AI, nearby-anchor POI lookup, Guardian phase transition, Heart drops/minion cleanup, and decoding the jigsaw structure templates. Vanilla 1.21.1's GameTest server constructs only its predefined vanilla worlds; the separate normal dedicated-server smoke test verifies Rift world creation, natural structure locating, and cross-dimension travel.

Output: `build/gametest/results.xml` and `build/gametest/logs/latest.log`.

`src/gametest` is a separate development mod and does not ship in the production jar. The only deprecated API used by tests is vanilla 1.21.1's mock **server-player** factory, which has no equivalent non-deprecated factory in that target version. Production code does not need it.

## Real client + dedicated server smoke test

This test needs a display or Xvfb, OpenGL (Mesa software rendering is sufficient), Java 21, and Python 3. Read the [Minecraft EULA](https://aka.ms/MinecraftEULA) before accepting it for the test instance.

Linux/headless:

```sh
./gradlew build
LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null \
  xvfb-run -a -s '-screen 0 1280x720x24' \
  python3 scripts/smoke_multiplayer.py --accept-eula
```

With a working desktop OpenGL display, run the Python command without `xvfb-run`.

The harness:

1. Starts an isolated normal dedicated server in `build/smoke-server`.
2. Loads the real Overworld and Rift datapacks and places an exhibition shrine.
3. Runs normal structure-locate commands in both dimensions.
4. Starts an actual Fabric client that connects over the vanilla protocol.
5. Uses a Rift Core on an Overworld anchor, checking the dimension transition and exact material consumption.
6. Uses the Riftblade through normal client interactions; verifies movement, durability and cooldown synchronization, and a rejected repeated use.
7. Receives/renders all four custom mobs, the shrine, dimension sky, and particles; samples real floating-island terrain below the display; and captures a screenshot.
8. Uses a Rift Anchor to return to the player's original Overworld entry point.
9. Shuts down both processes cleanly.

Logs: `build/smoke-reports/`. Screenshot: `run/smoke-client/screenshots/riftborn-smoke.png`.

**Isolation:** the temporary server uses offline mode to let the development client connect without account credentials. It listens on port 25565 in the test environment and is stopped by the harness. Do not expose it publicly or reuse these test properties for a production server. Use a fresh checkout or remove only the generated `build/smoke-server` and `run/smoke-client` test directories when resetting a test. Do not remove any real saves.

## Continuous integration

`.github/workflows/build.yml` runs the build, the GameTest server, and the headless multiplayer test on Java 21. A separate GameTest check exposes full test results, and a diagnostic check exposes stage results, the jar audit, and log tails; the `riftborn-1.21.1` Actions artifact contains the jars, reports, logs, and screenshot. GitHub workflow-write permission is needed to update that workflow.

## What automated checks do not prove

A successful build is not a substitute for extended survival playtesting. Automated checks exercise mechanics, resource loading, and client/server compatibility; they do not establish perfect boss balance, compatibility with every third-party mod or shader pack, or every possible seed and player-built portal obstruction. Back up saves, and test your intended modpack before deploying a long-running server.
