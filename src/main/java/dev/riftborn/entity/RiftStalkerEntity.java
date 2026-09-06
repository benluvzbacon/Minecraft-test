package dev.riftborn.entity;

import dev.riftborn.effect.RiftEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class RiftStalkerEntity extends AbstractRiftHostileEntity {
    private int teleportTicks = 100;

    public RiftStalkerEntity(EntityType<? extends RiftStalkerEntity> type, World world) {
        super(type, world);
        experiencePoints = 7;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 28)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32)
                .add(EntityAttributes.GENERIC_ARMOR, 2);
    }

    @Override protected void mobTick() {
        super.mobTick();
        LivingEntity target = getTarget();
        if (--teleportTicks <= 0) {
            teleportTicks = 100 + random.nextInt(80);
            if (target != null && target.isAlive() && squaredDistanceTo(target) > 9 && squaredDistanceTo(target) < 144) {
                Vec3d oldPos = getPos();
                // LivingEntity.teleport finds solid ground and rejects obstructed destinations.
                double angle = random.nextDouble() * Math.PI * 2;
                double x = getX() + Math.cos(angle) * 5;
                double z = getZ() + Math.sin(angle) * 5;
                if (teleport(x, getY() + 3, z, false)) {
                    RiftEffects.teleport((ServerWorld) getWorld(), oldPos);
                    RiftEffects.teleport((ServerWorld) getWorld(), getPos());
                }
            }
        }
    }
}
