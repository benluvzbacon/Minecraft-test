package dev.riftborn.entity;

import dev.riftborn.registry.ModParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public final class RiftWispEntity extends HostileEntity {
    @Nullable private UUID summoner;
    private int summonedLife = -1;

    public RiftWispEntity(EntityType<? extends RiftWispEntity> type, World world) {
        super(type, world);
        moveControl = new FlightMoveControl(this, 20, true);
        setNoGravity(true);
        experiencePoints = 8;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 36)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5);
    }

    @Override protected EntityNavigation createNavigation(World world) {
        BirdNavigation navigation = new BirdNavigation(this, world);
        navigation.setCanPathThroughDoors(false);
        navigation.setCanEnterOpenDoors(false);
        navigation.setCanSwim(false);
        return navigation;
    }

    @Override protected void initGoals() {
        goalSelector.add(1, new OrbitAndShootGoal());
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override public void tick() {
        setNoGravity(true);
        super.tick();
        if (getWorld().isClient && age % 3 == 0) {
            getWorld().addParticle(ModParticles.RIFT_MOTE, getX(), getY() + 0.3, getZ(), 0, -0.015, 0);
        }
        if (!getWorld().isClient && summonedLife > 0 && --summonedLife == 0) discard();
    }

    public void setSummoner(UUID uuid) {
        summoner = uuid;
        summonedLife = 900;
    }
    @Nullable public UUID getSummoner() { return summoner; }

    @Override public boolean handleFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_ALLAY_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ALLAY_DEATH; }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (summoner != null) nbt.putUuid("RiftSummoner", summoner);
        nbt.putInt("RiftSummonedLife", summonedLife);
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        summoner = nbt.containsUuid("RiftSummoner") ? nbt.getUuid("RiftSummoner") : null;
        summonedLife = nbt.contains("RiftSummonedLife") ? nbt.getInt("RiftSummonedLife") : -1;
    }

    private final class OrbitAndShootGoal extends Goal {
        private int fireTicks = 50;
        private int moveTicks;

        private OrbitAndShootGoal() { setControls(EnumSet.of(Control.MOVE, Control.LOOK)); }
        @Override public boolean canStart() { return true; }
        @Override public boolean shouldRunEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                getLookControl().lookAt(target, 90, 90);
                if (--moveTicks <= 0) {
                    moveTicks = 12;
                    Vec3d away = getPos().subtract(target.getPos());
                    double angle = Math.atan2(away.z, away.x) + 0.25;
                    Vec3d destination = target.getPos().add(Math.cos(angle) * 12, 3 + Math.sin(age * 0.06), Math.sin(angle) * 12);
                    // Flight navigation routes around obstacles; the wisp never uses noClip.
                    getNavigation().startMovingTo(destination.x, destination.y, destination.z, 1.0);
                    if (getNavigation().isIdle()) getMoveControl().moveTo(destination.x, destination.y, destination.z, 1.0);
                }
                if (--fireTicks <= 0 && squaredDistanceTo(target) < 28 * 28 && getVisibilityCache().canSee(target)) {
                    fireTicks = 60;
                    RiftBoltEntity.fire((ServerWorld) getWorld(), RiftWispEntity.this,
                            target.getPos().add(0, target.getHeight() * 0.55, 0), 5, 0.6f);
                    playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 0.45f, 1.7f);
                }
            } else if (--moveTicks <= 0) {
                moveTicks = 50;
                getMoveControl().moveTo(getX() + random.nextInt(9) - 4,
                        getY() + random.nextInt(5) - 2, getZ() + random.nextInt(9) - 4, 0.6);
            }
        }
    }
}
