package dev.riftborn.test;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.riftborn.Riftborn;
import dev.riftborn.dimension.RiftDimensions;
import dev.riftborn.effect.RiftFlight;
import dev.riftborn.registry.ModBlocks;
import dev.riftborn.world.RiftSurfaceStructure;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import java.util.HashSet;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Normal-world, development-only checks. Never packaged into the installation jar. */
public final class RiftbornWorldTests implements ModInitializer {
    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("RIFTBORN_SMOKE_FAILURE: " + message);
    }
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var world = server.getWorld(RiftDimensions.WORLD);
            require(world != null, "The Rift must exist on a normal server");
            var generator = world.getChunkManager().getChunkGenerator();
            var noise = world.getChunkManager().getNoiseConfig();
            var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
            for (String name : new String[]{"rift_ruin", "guardian_shrine"}) {
                var entry = registry.getEntry(RegistryKey.of(RegistryKeys.STRUCTURE, Riftborn.id(name))).orElseThrow();
                var structure = (RiftSurfaceStructure) entry.value();
                int generated = 0;
                var elevations = new HashSet<Integer>();
                for (int x = -24; x <= 24; x += 8) for (int z = -24; z <= 24; z += 8) {
                    var context = new Structure.Context(world.getRegistryManager(), generator, generator.getBiomeSource(), noise,
                            world.getStructureTemplateManager(), world.getSeed(), new ChunkPos(x, z), world, biome -> true);
                    var candidate = structure.getStructurePosition(context);
                    if (candidate.isEmpty()) continue;
                    var pieces = candidate.get().generate();
                    require(!pieces.isEmpty(), "Generated Rift placement must contain pieces");
                    generated++;
                    var box = pieces.getBoundingBox();
                    int surface = generator.getHeight(box.getCenter().getX(), box.getCenter().getZ(), Heightmap.Type.WORLD_SURFACE_WG, world, noise);
                    require(box.getMinY() >= 32 && surface >= 32, "Rift root must not be below the map: " + box + " / " + surface);
                    require(box.getMinY() + 1 >= surface && box.getMinY() + 1 - surface <= 10, "Rift root must follow nearby island elevation");
                    elevations.add(box.getMinY());
                }
                // The same seed/grid in 1.0 had only 31/35 above-map starts (18/14 void failures).
                // Relocation must preserve at least that many usable starts, not just disable the broken ones.
                require(generated >= (name.equals("rift_ruin") ? 31 : 35), "Too many valid Rift placements lost: " + name + " " + generated);
                require(elevations.size() >= 4, "Natural elevations must vary, not be forced to one Y");
                Riftborn.LOGGER.info("RIFTBORN_SURFACE_PLACEMENT_OK {} valid={}/49, below_map=0, elevations={}", name, generated, elevations);

                // Also exercise actual natural generation, not only unplaced structure starts.
                var nearest = generator.locateStructure(world, RegistryEntryList.of(entry), BlockPos.ORIGIN, 32, false);
                require(nearest != null, "Natural locate must find " + name);
                var located = nearest.getFirst();
                var chunk = world.getChunk(located.getX() >> 4, located.getZ() >> 4);
                var start = chunk.getStructureStart(structure);
                require(start != null && start.hasChildren(), "Natural Rift structure must have a valid start");
                var root = start.getChildren().getFirst().getBoundingBox();
                for (int x = root.getMinX() >> 4; x <= root.getMaxX() >> 4; x++)
                    for (int z = root.getMinZ() >> 4; z <= root.getMaxZ() >> 4; z++) world.getChunk(x, z);
                var floor = new BlockPos(root.getCenter().getX(), root.getMinY(), root.getCenter().getZ());
                require(root.getMinY() >= 32 && world.getBlockState(floor).isOf(ModBlocks.RIFT_STONE), "Natural Rift floor must actually be placed on the island: " + floor);
                Riftborn.LOGGER.info("RIFTBORN_NATURAL_STRUCTURE_OK {} floor={} box={}", name, floor, root);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(
                literal("riftborn_test").requires(source -> source.hasPermissionLevel(4))
                        .then(argument("state", StringArgumentType.word()).executes(context -> {
                            ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer("RiftbornTester");
                            require(player != null, "Smoke player must be online");
                            String state = StringArgumentType.getString(context, "state");
                            switch (state) {
                                case "flight_active" -> require(RiftFlight.hasFullSet(player) && RiftFlight.ownsFlight(player)
                                        && player.getAbilities().allowFlying && player.getAbilities().flying
                                        && Math.abs(player.getAbilities().getFlySpeed() - RiftFlight.FLIGHT_SPEED) < 0.00001,
                                        "Server must own the complete-set flight grant");
                                case "flight_absent" -> require(!RiftFlight.ownsFlight(player) && !player.getAbilities().allowFlying
                                        && !player.getAbilities().flying && Math.abs(player.getAbilities().getFlySpeed() - 0.05f) < 0.00001,
                                        "Server must revoke incomplete/dead/respawned flight");
                                case "creative_native" -> require(player.interactionManager.getGameMode() == GameMode.CREATIVE
                                        && player.getAbilities().allowFlying && !RiftFlight.ownsFlight(player)
                                        && Math.abs(player.getAbilities().getFlySpeed() - 0.05f) < 0.00001, "Creative must retain native flight");
                                default -> throw new IllegalArgumentException("Unknown smoke state " + state);
                            }
                            Riftborn.LOGGER.info("RIFTBORN_SERVER_FLIGHT_CHECK {} mode={} position={}", state, player.interactionManager.getGameMode(), player.getPos());
                            return 1;
                        }))));
    }
}
