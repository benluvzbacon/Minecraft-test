package dev.riftborn.dimension;

import dev.riftborn.Riftborn;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

public final class RiftDimensions {
    public static final RegistryKey<World> WORLD = RegistryKey.of(RegistryKeys.WORLD, Riftborn.id("the_rift"));
    private RiftDimensions() { }
}
