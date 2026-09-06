package dev.riftborn.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public abstract class AbstractRiftHostileEntity extends HostileEntity {
    protected AbstractRiftHostileEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.65));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        goalSelector.add(7, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_ENDERMAN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_ENDERMAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ENDERMAN_DEATH; }
}
