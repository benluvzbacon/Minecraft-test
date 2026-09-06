# Riftborn 1.0.0 — verification record

The production code at [`6acaa53`](https://github.com/benluvzbacon/Minecraft-test/commit/6acaa53765a6e289238b0ec90f316db34181ad0b) passed the complete [GitHub Actions verification run](https://github.com/benluvzbacon/Minecraft-test/actions/runs/34005448766) on Java 21 and Minecraft 1.21.1 with the pinned Fabric dependencies.

## Results

| Check | Result |
| --- | --- |
| `./gradlew --no-daemon clean build` | Passed; common, client, and development test source sets compile; production jar remapped |
| JUnit resource-integrity tests | **9 passed** |
| Dedicated-server GameTests | **20 passed**, zero failures |
| Generated PNG/JSON/NBT reproducibility | Byte-for-byte match with committed resources |
| Production archive audit | Passed, including Java 21 bytecode and test-mod exclusion |
| Real Fabric client + dedicated server | Passed, with clean shutdown |

The GameTests cover full-body blink clearance, walls, panes, diagonal corner grazing between samples, low ceilings, fluids, void landings, authoritative cooldown/durability, recipes, mining-tool tags, the Overworld ore-generation hook, nearby-anchor POI lookup, registry/template loading, saved return-point serialization, projectile damage, Stalker teleportation, Brute knockback, Wisp flight/ranged AI, Guardian phase transition, Heart drops, and summoned-Wisp cleanup.

The client/server smoke test independently confirmed:

- Natural structure locating in the Overworld and The Rift.
- Survival entry using a Rift Core, with exactly one Core consumed.
- Networked Riftblade movement, durability cost, and cooldown synchronization; repeated use during cooldown is rejected.
- Loaded floating-island terrain: **94 sampled columns** with terrain suspended above air, below the separate display structure.
- All four mob types reaching the client, registered renderers, the custom sky, and particles.
- Returning through a Rift Anchor to the player's saved Overworld origin.

The [in-game screenshot](images/the-rift.png) was captured by that real client. Its shrine and stationary display mobs are an explicit test exhibition, not a claim that this particular scene was found through survival exploration. Natural generation and combat AI are tested separately.

## Distributable

- File: **`riftborn-1.0.0.jar`**
- Size: **149,934 bytes**
- Archive: **215 entries**, including **36 Java 21 classes**
- SHA-256:

```text
f46bd08b8560c0bb8af1ec4d7f2722f589dd8e281830bbe6eb2d807886ba8a2e
```

The audit verifies intermediary remapping, entrypoint classes, JSON decoding, archive integrity, inclusion of every main resource, and exclusion of both development test mods. The retrieved jar was independently rechecked with `python3 scripts/verify_jar.py`.

A normal source build writes the installation jar to **`build/libs/riftborn-1.0.0.jar`**. The current workflow supplies only the compiled jar at the root of the **`riftborn-1.21.1`** Actions artifact ZIP; `SHA256SUMS` and test reports are in **`riftborn-1.21.1-diagnostics`**. Install the regular jar, not `-sources.jar`, alongside Fabric API on both the client and the server.

## Scope

These are automated build and runtime checks, not a claim of exhaustive playtesting. The multiplayer smoke uses one actual networked client; it is not a multi-user load test. Boss balance, long survival sessions, every world seed, arbitrary player-built obstructions, and third-party mod/shader-pack compatibility still warrant playtesting. Back up existing saves.

See [TESTING.md](TESTING.md) for reproducible commands and test-instance isolation details.
