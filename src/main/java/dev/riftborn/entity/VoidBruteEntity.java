package dev.riftborn.entity;

import dev.riftborn.effect.RiftEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public final class VoidBruteEntity extends AbstractRiftHostileEntity {
    public VoidBruteEntity(EntityType<? extends VoidBruteEntity> type, World world) {
        super(type, world);
        experiencePoints = 18;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 100)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.18)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 2.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.85)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32)
                .add(EntityAttributes.GENERIC_ARMOR, 6);
    }

    @Override public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && getWorld() instanceof ServerWorld world) {
            RiftEffects.burst(world, target.getPos().add(0, 1, 0), 14, 0.6);
            playSound(SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 0.65f);
        }
        return hit;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_RAVAGER_DEATH; }
}
