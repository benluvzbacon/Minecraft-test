package dev.riftborn.world;

import dev.riftborn.Riftborn;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.structure.StructureType;

public final class RiftStructures {
    public static final StructureType<RiftSurfaceStructure> SURFACE = Registry.register(Registries.STRUCTURE_TYPE,
            Riftborn.id("rift_surface"), () -> RiftSurfaceStructure.CODEC);
    public static void initialize() { }
    private RiftStructures() { }
}
