package dev.riftborn.effect;

import dev.riftborn.registry.ModParticles;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class RiftEffects {
    private RiftEffects() { }

    public static void teleport(ServerWorld world, Vec3d pos) {
        burst(world, pos.add(0, 1, 0), 36, 0.5);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 0.8f, 1.25f);
    }

    public static void burst(ServerWorld world, Vec3d pos, int count, double spread) {
        world.spawnParticles(ModParticles.RIFT_MOTE, pos.x, pos.y, pos.z, count,
                spread, spread, spread, 0.04);
    }
}
