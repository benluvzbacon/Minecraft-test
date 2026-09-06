package dev.riftborn.test;

import dev.riftborn.Riftborn;
import dev.riftborn.dimension.RiftDimensions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;

/** Uses a normal server, since vanilla's GameTest server does not create datapack dimensions. */
public final class RiftbornWorldTests implements ModInitializer {
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var world = server.getWorld(RiftDimensions.WORLD);
            if (world == null) throw new AssertionError("The Rift must exist on a normal server");
            var generator = world.getChunkManager().getChunkGenerator();
            var noise = world.getChunkManager().getNoiseConfig();
            for (String name : new String[]{"rift_ruin", "guardian_shrine"}) {
                var structure = (JigsawStructure) world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(Riftborn.id(name));
                int generated = 0, belowMap = 0;
                for (int x = -24; x <= 24; x += 8) for (int z = -24; z <= 24; z += 8) {
                    var context = new Structure.Context(world.getRegistryManager(), generator, generator.getBiomeSource(), noise,
                            world.getStructureTemplateManager(), world.getSeed(), new ChunkPos(x, z), world, biome -> true);
                    var candidate = structure.getStructurePosition(context);
                    if (candidate.isEmpty()) continue;
                    var pieces = candidate.get().generate();
                    if (pieces.isEmpty()) continue;
                    generated++;
                    var box = pieces.getBoundingBox();
                    int surface = generator.getHeight(box.getCenter().getX(), box.getCenter().getZ(), Heightmap.Type.WORLD_SURFACE_WG, world, noise);
                    if (box.getMinY() < 32) {
                        belowMap++;
                        if (belowMap <= 3) Riftborn.LOGGER.info("RIFTBORN_LEGACY_PLACEMENT {} chunk={}, root={}, surface={}", name, context.chunkPos(), box, surface);
                    }
                }
                Riftborn.LOGGER.info("RIFTBORN_LEGACY_PLACEMENT_SUMMARY {} generated={}, below_map={}", name, generated, belowMap);
            }
        });
    }
}
