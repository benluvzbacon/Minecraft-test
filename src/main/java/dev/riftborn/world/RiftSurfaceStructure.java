package dev.riftborn.world;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.DimensionPadding;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import java.util.Optional;

/**
 * Used only by riftborn:rift_ruin and riftborn:guardian_shrine. Overworld ruins
 * remain vanilla minecraft:jigsaw. Existing vanilla pool pieces/templates and
 * saved starts keep their codecs, loot, mobs, anchors, and altar behavior.
 */
public final class RiftSurfaceStructure extends Structure {
    public static final MapCodec<RiftSurfaceStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            configCodecBuilder(instance),
            StructurePool.REGISTRY_CODEC.fieldOf("start_pool").forGetter(value -> value.startPool),
            Codec.intRange(0, 7).fieldOf("size").forGetter(value -> value.size),
            Codec.intRange(0, 64).optionalFieldOf("surface_search_radius", 48).forGetter(value -> value.searchRadius),
            Codec.BOOL.optionalFieldOf("circular_footprint", false).forGetter(value -> value.circularFootprint)
    ).apply(instance, RiftSurfaceStructure::new));

    private final RegistryEntry<StructurePool> startPool;
    private final int size;
    private final int searchRadius;
    private final boolean circularFootprint;
    public RiftSurfaceStructure(Config config, RegistryEntry<StructurePool> startPool, int size, int searchRadius, boolean circularFootprint) {
        super(config); this.startPool = startPool; this.size = size; this.searchRadius = searchRadius; this.circularFootprint = circularFootprint;
    }
    @Override public StructureType<?> getType() { return RiftStructures.SURFACE; }

    @Override public Optional<StructurePosition> getStructurePosition(Context context) {
        // Build the unplaced, randomly rotated jigsaw pieces once. This temporary Y
        // is only for calculating their footprint; it is never the final location.
        BlockPos provisional = new BlockPos(context.chunkPos().getStartX(),
                context.world().getBottomY() + context.world().getHeight() / 2, context.chunkPos().getStartZ());
        var generated = StructurePoolBasedGenerator.generate(context, startPool, Optional.empty(), size, provisional,
                false, Optional.empty(), 80, StructurePoolAliasLookup.EMPTY, DimensionPadding.NONE,
                StructureLiquidSettings.APPLY_WATERLOGGING);
        if (generated.isEmpty()) return Optional.empty();
        var pieces = generated.get().generate();
        if (pieces.isEmpty()) return Optional.empty();
        var box = pieces.getBoundingBox();
        var site = RiftSurfacePlacement.find(box,
                (x, z) -> context.chunkGenerator().getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig()),
                context.world().getBottomY() + 32, context.world().getTopY() - box.getBlockCountY(), searchRadius, circularFootprint);
        if (site.isEmpty()) return Optional.empty();
        var landing = site.get();
        int dy = landing.floorY() - box.getMinY();
        pieces.toList().pieces().forEach(piece -> piece.translate(landing.offsetX(), dy, landing.offsetZ()));
        // Reuse the evaluated collector: evaluating its random generator again would move/rotate the footprint.
        return Optional.of(new StructurePosition(generated.get().position().add(landing.offsetX(), dy, landing.offsetZ()), Either.right(pieces)));
    }
}
