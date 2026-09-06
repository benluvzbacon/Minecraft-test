package dev.riftborn.registry;

import dev.riftborn.Riftborn;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModParticles {
    public static final SimpleParticleType RIFT_MOTE = Registry.register(
            Registries.PARTICLE_TYPE, Riftborn.id("rift_mote"), FabricParticleTypes.simple());

    private ModParticles() { }
    public static void initialize() { }
}
