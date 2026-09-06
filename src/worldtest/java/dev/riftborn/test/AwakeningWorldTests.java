package dev.riftborn.test;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.riftborn.Riftborn;
import dev.riftborn.awakening.*;
import dev.riftborn.awakening.entity.*;
import dev.riftborn.awakening.item.RiftStaffItem;
import dev.riftborn.dimension.RiftDimensions;
import dev.riftborn.registry.ModItems;
import java.util.*;
import java.util.function.Consumer;
import static net.minecraft.server.command.CommandManager.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
/** Executes only in runSmokeServer. Expensive generation checks are spread across ticks. */
public final class AwakeningWorldTests {
    private static final Queue<Consumer<MinecraftServer>> TASKS = new ArrayDeque<>();
    private static int audited;
    private static final Map<String, BlockBox> SITES = new HashMap<>();
    private static void require(boolean b, String message) {
        if (!b)
            throw new IllegalStateException("RIFTBORN_SMOKE_FAILURE: " + message);
    }
    private static ServerPlayerEntity player(MinecraftServer s) {
        var p = s.getPlayerManager().getPlayer("RiftbornTester");
        require(p != null, "Awakening test player is online");
        return p;
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!TASKS.isEmpty())
                TASKS.remove().accept(server);
        });
        CommandRegistrationCallback.EVENT.register(
            (dispatcher, access, environment)
                -> dispatcher.register(literal("riftborn_awake")
                        .requires(s -> s.hasPermissionLevel(4))
                        .then(argument("step", StringArgumentType.word()).executes(ctx -> {
                            run(ctx.getSource().getServer(), StringArgumentType.getString(ctx, "step"));
                            return 1;
                        }))));
    }
    private static void auditStructure(MinecraftServer server, String name, boolean abyss) {
        var w = server.getWorld(abyss ? AbyssWorlds.ABYSS : RiftDimensions.WORLD);
        require(w != null, "Both realm worlds registered");
        var registry = w.getRegistryManager().get(RegistryKeys.STRUCTURE);
        var entry =
            registry.getEntry(RegistryKey.of(RegistryKeys.STRUCTURE, Riftborn.id("awakening/" + name))).orElseThrow();
        var gen = w.getChunkManager().getChunkGenerator();
        long start = System.nanoTime();
        var locate = gen.locateStructure(w, RegistryEntryList.of(entry), BlockPos.ORIGIN, 16, false);
        require(locate != null, "Natural generation locates " + name);
        var pos = locate.getFirst();
        var chunk = w.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        var structure = chunk.getStructureStart(entry.value());
        require(structure != null && structure.hasChildren(), "Natural start exists " + name);
        var box = structure.getChildren().getFirst().getBoundingBox();
        SITES.put(name, box);
        for (int x = box.getMinX() >> 4; x <= box.getMaxX() >> 4; x++)
            for (int z = box.getMinZ() >> 4; z <= box.getMaxZ() >> 4; z++) w.getChunk(x, z);
        var floor = new BlockPos(box.getCenter().getX(), box.getMinY(), box.getCenter().getZ());
        require(floor.getY() >= 32, "No below-map structure: " + name + " " + floor);
        require(!w.getBlockState(floor).isAir(), "Natural foundation exists " + name);
        Riftborn.LOGGER.info(
            "RIFTBORN_20_STRUCTURE_OK {} floor={} elapsed_ms={}", name, floor, (System.nanoTime() - start) / 1_000_000);
        audited++;
    }
    private static void auditTerrain(MinecraftServer server) {
        var w = server.getWorld(AbyssWorlds.ABYSS);
        require(w != null && w.getHeight() == 384, "Distinct 384-block Abyss dimension");
        var gen = w.getChunkManager().getChunkGenerator();
        var noise = w.getChunkManager().getNoiseConfig();
        BlockPos sample = null;
        for (int x = -128; x <= 128 && sample == null; x += 32)
            for (int z = -128; z <= 128; z += 32)
                if (gen.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, w, noise) > 200) {
                    sample = new BlockPos(x, 0, z);
                    break;
                }
        require(sample != null, "Upper Abyss landmasses exist");
        int lower = 0, upper = 0, ores = 0, voidCells = 0, enclosedAir = 0;
        var mutable = new BlockPos.Mutable();
        for (int cx = sample.getX() >> 4; cx < (sample.getX() >> 4) + 2; cx++)
            for (int cz = sample.getZ() >> 4; cz < (sample.getZ() >> 4) + 2; cz++) {
                var chunk = w.getChunk(cx, cz);
                for (int x = 0; x < 16; x++)
                    for (int z = 0; z < 16; z++)
                        for (int y = 8; y < 352; y++) {
                            var state = chunk.getBlockState(mutable.set(cx * 16 + x, y, cz * 16 + z));
                            if (state.isOf(AbyssBlocks.ORE))
                                ores++;
                            if (!state.isAir()) {
                                if (y < 140)
                                    lower++;
                                if (y > 180)
                                    upper++;
                            } else {
                                if (y > 140 && y < 165) voidCells++;
                                if (y > 90 && y < 235
                                        && !chunk.getBlockState(mutable.set(cx * 16 + x, y + 8, cz * 16 + z)).isAir()
                                        && !chunk.getBlockState(mutable.set(cx * 16 + x, y - 8, cz * 16 + z)).isAir()) enclosedAir++;
                            }
                        }
            }
        require(lower > 500 && upper > 500 && voidCells > 1000 && ores > 0 && enclosedAir > 0,
            "Layered Abyss terrain and natural crystal ore: " + lower + "," + upper + "," + voidCells + "," + ores + ", enclosed=" + enclosedAir);
        Riftborn.LOGGER.info(
            "RIFTBORN_20_TERRAIN_OK lower={} upper={} gap_air={} ores={} enclosed_air={}", lower, upper, voidCells, ores, enclosedAir);
    }
    private static AbyssBossEntity boss(ServerPlayerEntity p) {
        return p.getServerWorld()
            .getEntitiesByClass(AbyssBossEntity.class, p.getBoundingBox().expand(96), e -> e.isAlive())
            .stream()
            .findFirst()
            .orElseThrow();
    }
    private static void run(MinecraftServer server, String step) {
        var p = player(server);
        var w = p.getServerWorld();
        var state = AwakeningState.get(server);
        switch (step) {
            case "audit" -> {
                audited = 0;
                TASKS.add(AwakeningWorldTests::auditTerrain);
                for (String n : List.of("rift_citadel", "broken_temple", "sky_ruins"))
                    TASKS.add(s -> auditStructure(s, n, false));
                for (String n : List.of("abyssal_fortress", "forgotten_laboratory", "abyss_sky_ruins", "colossus_arena",
                         "architect_spire", "sovereign_arena", "storm_observatory"))
                    TASKS.add(s -> auditStructure(s, n, true));
                TASKS.add(s -> {
                    require(audited == 10, "Every new structure generated");
                    Riftborn.LOGGER.info("RIFTBORN_20_WORLD_AUDIT_OK");
                });
            }
            case "vista" -> {
                var box = SITES.get("abyssal_fortress");
                require(box != null, "Audited natural fortress available");
                var destination = server.getWorld(AbyssWorlds.ABYSS);
                var pos = new Vec3d(box.getCenter().getX() + 0.5, box.getMaxY() + 20, box.getMaxZ() + 28.5);
                destination.getChunk(BlockPos.ofFloored(pos));
                p.teleportTo(new net.minecraft.world.TeleportTarget(destination, pos, Vec3d.ZERO, 180, 25,
                        net.minecraft.world.TeleportTarget.ADD_PORTAL_CHUNK_TICKET));
            }
            case "stop_motion" -> {
                p.setVelocity(Vec3d.ZERO);
                p.velocityModified = true;
                p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(p));
            }
            case "clear_displays" -> {
                for (var entity : w.getOtherEntities(null, p.getBoundingBox().expand(100),
                        e -> e.getCommandTags().contains("awakening_display") || e.getCommandTags().contains("awakening_weapon"))) entity.discard();
            }
            case "key" -> {
                require(state.has(p.getUuid(), AwakeningState.TEMPLE), "Puzzle completed before key");
                var input = CraftingRecipeInput.create(3, 3,
                    List.of(new ItemStack(ModItems.RIFT_CORE), new ItemStack(ModItems.RIFT_HEART),
                        new ItemStack(ModItems.RIFT_CORE), new ItemStack(ModItems.VOID_FRAGMENT),
                        new ItemStack(AbyssItems.RESONANT_SIGIL), new ItemStack(ModItems.VOID_FRAGMENT),
                        new ItemStack(ModItems.RIFT_CORE), new ItemStack(ModItems.RIFT_HEART),
                        new ItemStack(ModItems.RIFT_CORE)));
                var recipe = w.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, input, w).orElseThrow();
                p.setStackInHand(Hand.MAIN_HAND, recipe.value().craft(input, w.getRegistryManager()));
            }
            case "entry" -> {
                require(w.getRegistryKey().equals(AbyssWorlds.ABYSS) && state.has(p.getUuid(), AwakeningState.ABYSS),
                    "Key attuned personal Abyss entry");
                require(state.player(p.getUuid()).origin.dimension().equals(RiftDimensions.WORLD),
                    "Abyss return belongs to The Rift");
            }
            case "dash" -> {
                require(AbyssGear.fullSet(p) && p.getAbilities().allowFlying
                        && state.player(p.getUuid()).dashReady > server.getOverworld().getTime(),
                    "Dash is server-owned and cooling down");
            }
            case "weapon" -> {
                var target = w.getEntitiesByClass(AbyssalBruteEntity.class, p.getBoundingBox().expand(40),
                                  e -> e.getCommandTags().contains("awakening_weapon"))
                                 .getFirst();
                require(target.getHealth() < target.getMaxHealth(), "Networked weapon damaged actual target");
                target.setHealth(target.getMaxHealth());
            }
            case "heal_setup" -> p.setHealth(10);
            case "heal" -> require(p.getHealth() > 10, "Mend spell actually restores health");
            case "artifact" ->
                require(AbyssGear.artifact(p, AbyssItems.VOID_CORE)
                        && p.getAbilities().getFlySpeed() > Riftborn.CONFIG.abyssalFlightSpeed,
                    "Offhand artifact modifies server flight speed");
            case "freeze" -> boss(p).setAiDisabled(true);
            case "phase3" -> boss(p).setHealth(boss(p).getMaxHealth() * 0.15f);
            case "combat" -> {
                var b = boss(p);
                b.setHealth(b.getMaxHealth() * (b.bossKind() == 2 ? 0.15f : 0.4f));
                b.setAiDisabled(false);
                b.setTarget(p);
            }
            case "defeat" -> {
                var b = boss(p);
                require(b.shotsFired > 0 && b.pulsesReleased > 0,
                    "Boss executes ranged and telegraphed area attacks: " + b.getType() + " shots=" + b.shotsFired
                        + " pulses=" + b.pulsesReleased);
                if (b.bossKind() == 1)
                    require(b.barriersMade > 0, "Architect creates temporary arena barriers");
                if (b.bossKind() == 2)
                    require(((AbyssSovereignEntity) b).usedFinalAttack(), "Sovereign releases Last Silence");
                require(b.summonsMade > 0, "Boss summons are active: " + b.getType());
                int kind = b.bossKind();
                b.damage(w.getDamageSources().playerAttack(p), 100000);
                require(state.has(p.getUuid(),
                            kind == 0       ? AwakeningState.COLOSSUS
                                : kind == 1 ? AwakeningState.ARCHITECT
                                            : AwakeningState.SOVEREIGN),
                    "Boss kill advances endgame progression");
                Riftborn.LOGGER.info("RIFTBORN_20_BOSS_OK kind={} shots={} pulses={} summons={} barriers={}", kind,
                    b.shotsFired, b.pulsesReleased, b.summonsMade, b.barriersMade);
            }
            case "storm" -> require(AwakeningEvents.startStorm(w, p.getBlockPos()), "Storm can start in a realm");
            case "storm_check" -> {
                require(AwakeningEvents.kind(w) == 1
                        && !w
                            .getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(64),
                                e -> e.getCommandTags().contains("riftborn_storm"))
                            .isEmpty(),
                    "Storm spawns stronger enemies");
                state.event(w.getRegistryKey()).ends = server.getOverworld().getTime();
                state.markDirty();
            }
            case "herald" -> {
                var b = boss(p);
                require(b.bossKind() == 3 && AwakeningEvents.kind(w) == 2, "Collapse has a unique Herald");
                b.damage(w.getDamageSources().playerAttack(p), 100000);
                require(AwakeningEvents.kind(w) == 0, "Herald death ends the Collapse");
                require(!w.getEntitiesByClass(ItemEntity.class, p.getBoundingBox().expand(64),
                              e -> e.getStack().isOf(AbyssItems.SOVEREIGN_SIGIL))
                            .isEmpty(),
                    "Herald drops final-boss sigil");
                Riftborn.LOGGER.info("RIFTBORN_20_COLLAPSE_OK");
            }
            case "finish" -> {
                require(state.has(p.getUuid(),
                            AwakeningState.GUARDIAN | AwakeningState.ABYSS | AwakeningState.COLOSSUS
                                | AwakeningState.ARCHITECT | AwakeningState.SOVEREIGN),
                    "Full endgame progression complete");
                Riftborn.LOGGER.info("RIFTBORN_20_SERVER_PROGRESSION_OK");
            }
            default -> throw new IllegalArgumentException("Unknown Awakening test step " + step);
        }
    }
}
