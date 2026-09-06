# The Rift Awakening — field guide

## Follow the second heartbeat

The original Rift progression is unchanged. Defeating a Rift Guardian now reveals that it was guarding a seal; existing players with the old Guardian advancement are recognized when joining. The new story is conveyed through brief inscriptions in explorable structures, not mandatory dialogue screens.

1. **Find a Broken Temple in The Rift.** Its central lock is surrounded by three resonators. Read the inscription, then tune the left/rear/right resonators to **1 / 3 / 2** marks and use the lock. Rotated structures use the same relative arrangement.
2. **Claim a Resonant Sigil.** The first solve grants one. An already-solved lock can turn a Rift Core into another, so co-op players are not permanently locked out.
3. **Craft an Abyssal Key:** four Rift Cores, two Rift Hearts, two Void Fragments, and the Sigil. These do not require Abyss materials, so entry is never circularly gated.
4. **Find an Abyss Gateway** in a Rift Citadel or Broken Temple. Using the Key in the air locates a gateway. Your first successful crossing consumes a Key and permanently attunes *you*. Failed/obstructed crossings do not consume it. Subsequent entry is free for that player.
5. **Explore The Abyss.** Any Abyss Gateway returns you to your own saved Rift entrance. Your original Overworld return record remains separate. Arrival has a one-time guarded landing, not repeated overwrites of player builds.
6. **Mine and hunt.** Crystal ore is exclusive to The Abyss and requires a diamond-tier pickaxe. Abyssal Shards refine into Crystals and also serve as Voidbow ammunition. Creatures drop Essence, which fuels the Staff and forms Cores.
7. **Explore a Forgotten Laboratory.** Schematics allow smithing Rift Armor + Abyssal Core into Abyssal Armor while retaining the base stack's custom components. Schematics can be duplicated with seven Crystals and Rift Stone.
8. **Awaken the Colossus** with an Abyssal Core at its arena. Its exclusive Titan Cores unlock the Architect and forge a Greatblade.
9. **Defeat the Architect** by offering a Titan Core at its spire. Reality Spindles make the Rift Staff and a Collapse Catalyst.
10. **Call The Collapse** at a Sovereign altar using the Catalyst, after both earlier bosses. Defeat the Herald to obtain its exclusive Sovereign Sigil.
11. **Offer the Sigil to the Sovereign.** Its three phases culminate in the Last Silence. The Sovereign Heart creates the Heart of the Rift artifact or a luminous Sovereign Standard. Complete the major milestones for **Rift Master**.

Nearby co-op participants receive boss milestones. Boss-exclusive materials never appear in ordinary chests. Bosses can be challenged again with another offering.

## Materials and equipment

| Item | Purpose / recipe |
|---|---|
| Abyssal Shard | Ore and creature reward; Voidbow ammunition |
| Abyssal Crystal | Shapeless four Shards + one Rift Dust; equipment material and repair ingredient |
| Abyssal Essence | Creature reward; Staff ammunition and Core ingredient |
| Abyssal Core | Four Crystals + four Essences around one Rift Core |
| Abyssal Schematic | Laboratory treasure; smithing template; duplicate rather than losing your last copy |
| Stormglass | Storm-touched enemy reward or observatory treasure; artifacts |
| Titan Core | Colossus-exclusive; Architect offering, Greatblade, Catalyst/artifact recipes |
| Reality Spindle | Architect-exclusive; Staff, Catalyst/artifact recipes |
| Sovereign Sigil | Collapse Herald-exclusive; final boss offering |
| Sovereign Heart | Sovereign-exclusive; final artifact or cosmetic trophy |

### Abyssal Armor — Abyssal Ascension

Smith each existing Rift piece with an Abyssal Core and an Abyssal Schematic. Native smithing retains enchantments and custom stack components.

| Piece | Protection | Durability |
|---|---:|---:|
| Helmet | 4 | 638 |
| Chestplate | 9 | 928 |
| Leggings | 7 | 870 |
| Boots | 4 | 754 |

Each piece supplies 4 toughness and 10% knockback resistance. Repair with Crystals. Armor enchantments and trims are supported.

The **complete** set grants:
- **Abyssal Flight**, default speed **0.22**, versus Rift Armor's unchanged default **0.15** and native Creative **0.05**. Both armor speeds are server-configurable.
- **Aegis:** one incoming hit is reduced by 50%, capped at 6 damage, once per ten seconds. It never cancels a hit or grants invulnerability; void/kill damage is not blocked.
- Reduced fall damage where applicable, and a short Slow Falling grace after a dash. Ordinary armor flight still uses native Minecraft controls.
- A cyan flight trail and the compact dash HUD.

Mixing armor sets does not grant either full-set bonus. Death, removal, breakage, respawn, mode changes, and login still use the reversible 1.5 server flight system. Creative/Spectator retain native flight, not the armor boost.

### Rift Dash

Default key: **R**, configurable in Minecraft's keybindings. Requires all four Abyssal pieces in Survival/Adventure. Default cooldown: **160 ticks / 8 seconds**.

The server sweeps the full body along the path, rejects solid blocks/fluids/unloaded chunks, and requires a supported endpoint. Low aerial dashes can end up to 24 blocks above a collision-checked landing; high open-void dashes are refused. Dash resets velocity/fall distance and briefly grants Slow Falling. Its server deadline persists through reconnects. A client request cannot supply its own distance or position.

**The original Riftblade is unchanged**, including its stricter supported-landing and void check.

## Weapons

- **Abyssal Greatblade:** 15 melee damage, 0.9 attack speed, 2,800 durability. Hold Use for at least one second and release a frontal cleave. Five-block range, line-of-sight requirement, four durability cost, five-second base cooldown. Crafted with a Titan Core, three Crystals, and an Abyssal Core.
- **Voidbow:** charged custom Abyss bolts; one Shard per shot. Supports Power, Punch, Flame, durability enchantments, and a short server cooldown. Special ammunition is still consumed with Infinity. Recipe: six Crystals, two Essences, one Bow.
- **Rift Staff:** sneak-use cycles modes; ordinary Use casts. One shared cooldown prevents swapping modes to bypass recovery. Recipe: Reality Spindle, two Crystals, Abyssal Core, Blaze Rod.
  - **Lance:** one Essence, focused 10-damage bolt, 25-tick base cooldown.
  - **Repulse:** two Essences, nearby hostile damage/knockback, 100-tick base cooldown.
  - **Mend:** three Essences, brief regeneration for you and nearby scoreboard teammates, 240-tick base cooldown.

Recipes are provided in the vanilla recipe book. The Staff's tooltip shows its selected mode.

## Choose one artifact

Only an artifact held in the **offhand** provides its passive effect. It competes with a shield; putting more artifacts in your inventory does not stack their bonuses.

| Artifact | Choice |
|---|---|
| Heart of the Rift | 20% shorter **new** weapon/dash cooldowns; forged with Sovereign and earlier boss materials; original Riftblade timings are not changed |
| Abyssal Eye | Night Vision in the realms. Use to locate landmarks; sneak-use cycles Abyss targets. Observatory signals are revealed during realm events |
| Void Core | 12% additional armor-flight speed; Stormglass + Crystals + Abyssal Core |
| Warding Rift Anchor | While grounded and still, reduce a hit by 25%, up to 3 damage, once per ten seconds; distinct from the existing portal-anchor block |

## Know your opponents

| Enemy | Role / counterplay |
|---|---|
| Abyss Stalker | Fast quadruped hunter, brief invisibility and short lunges. Glowing eyes/audio telegraph its approach |
| Void Reaver | Airborne strafing manta, changing orbit direction and paired projectiles. Keep moving and use cover |
| Abyssal Brute | 150-health tank, heavy melee and a warned ground stomp. Jump, fly, or retreat outside its ring |
| Rift Echo | Mirrors player sprinting, jumping, shield use, and ranged-item use. Change tactics rather than feeding its imitation |
| Abyssal Warden | Elite sentinel with a charged, line-of-sight beam. Move away from its locked aim or break sight |

### Major encounters

Boss health scales moderately for nearby co-op players. Fights are bounded to their arena region and reject player damage from beyond 64 blocks; they are not intended to be sniped from outside the encounter.

- **Abyssal Colossus — 520 base health:** huge melee reach, projectiles, Stalker summons, two phases, ground shockwaves and marked area attacks. Grounded waves can be jumped; its second phase attacks faster.
- **Rift Architect — 650 base health:** teleportation, Echo constructs, volleys, and temporary barrier segments. Barriers only occupy empty, unoccupied cells, can be broken, expire automatically, and are cleared on defeat. It does not overwrite terrain or player builds.
- **Abyss Sovereign — 1,000 base health:** three phases at roughly 67% and 33%, growing pressure and summons, shifting barriers, then a bright transformed crown/body. **Last Silence** is warned for five seconds: reach the small inner safe ring rather than fleeing outward. Armor, cover, movement and defensive cooldowns matter.

## Exploration, storms and The Collapse

The new structures vary from 25-block-wide sky ruins to the 81-block-wide Sovereign arena and 64-block-tall Architect spire. Citadels have climbable towers; laboratories have multiple floors and schematics; temples have the resonance puzzle; arenas have their own altars, cover and lore. Chests use weighted rarity tiers and do not hand out boss-exclusive cores.

**Rift Storm:** rare, temporary realm event with sky/HUD changes, more ambient motes, strengthened enemies, Stormglass rewards and observatory signals. It never changes Overworld weather or generation.

**The Collapse:** a much rarer post-Architect event, or an expensive altar ritual. Bounded waves accompany a unique Herald. Its defeat ends the event and drops the sigil needed for the Sovereign.

There is at most one active event per realm. Timers are persisted, waves are capped, spawning stays in loaded chunks away from the player, and event creatures are cleaned up when the event ends. Temporary boss barriers use saved scheduled ticks, so they expire after chunk/server reloads rather than becoming permanent walls.

## Updates and scope

Old worlds, item IDs, mobs, recipes, return points, Overworld structures, and the 1.5 Rift placement fix are retained. New structures and material veins require **new chunks**. No retroactive structure relocation or terrain rewrite is performed. Back up saves before upgrading, and update both the server and all clients.

The existing Gradle wrapper/build remains in use. The installation output is `build/libs/riftborn-2.0.0.jar`; the `riftborn-1.21.1` Actions artifact remains a ZIP containing only that compiled jar. Diagnostics/screenshots are separate.
