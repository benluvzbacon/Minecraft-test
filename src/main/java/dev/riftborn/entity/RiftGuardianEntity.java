package dev.riftborn.entity;

import dev.riftborn.Riftborn;
import dev.riftborn.effect.RiftEffects;
import dev.riftborn.registry.ModEntities;
import dev.riftborn.registry.ModParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Telegraphs area attacks, respects shields/armor, and never destroys terrain. */
public final class RiftGuardianEntity extends AbstractRiftHostileEntity {
    private static final TrackedData<Boolean> ENRAGED = DataTracker.registerData(RiftGuardianEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final EntityAttributeModifier PHASE_SPEED = new EntityAttributeModifier(
            Riftborn.id("guardian_second_phase"), 0.055, EntityAttributeModifier.Operation.ADD_VALUE);
    private final ServerBossBar bossBar = new ServerBossBar(Text.translatable("entity.riftborn.rift_guardian"),
            BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
    private int projectileTicks = 70;
    private int teleportTicks = 180;
    private int summonTicks = 240;
    private int pulseTicks = 140;
    private int pulseWindup;

    public RiftGuardianEntity(EntityType<? extends RiftGuardianEntity> type, World world) {
        super(type, world);
        experiencePoints = 120;
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 280)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.95)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48)
                .add(EntityAttributes.GENERIC_ARMOR, 8);
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ENRAGED, false);
    }
    public boolean isEnraged() { return dataTracker.get(ENRAGED); }
    public boolean isChargingPulse() { return pulseWindup > 0; }

    @Override protected void mobTick() {
        super.mobTick();
        ServerWorld world = (ServerWorld) getWorld();
        bossBar.setPercent(Math.clamp(getHealth() / getMaxHealth(), 0, 1));
        if (!isEnraged() && getHealth() <= getMaxHealth() * 0.5f) enterSecondPhase(world);
        if (isEnraged() && !getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).hasModifier(PHASE_SPEED.id())) {
            getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).addTemporaryModifier(PHASE_SPEED);
        }
        bossBar.setColor(isEnraged() ? BossBar.Color.PINK : BossBar.Color.PURPLE);
        if (pulseWindup > 0) {
            drawPulseWarning(world);
            if (--pulseWindup == 0) releasePulse(world);
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (--projectileTicks <= 0 && getVisibilityCache().canSee(target)) {
            projectileTicks = isEnraged() ? 45 : 75;
            Vec3d aim = target.getPos().add(0, target.getHeight() * 0.55, 0);
            RiftBoltEntity.fire(world, this, aim, isEnraged() ? 7 : 6, 0.7f);
            if (isEnraged()) {
                Vec3d side = target.getPos().subtract(getPos()).normalize().crossProduct(new Vec3d(0, 1, 0)).multiply(2);
                RiftBoltEntity.fire(world, this, aim.add(side), 6, 0.65f);
                RiftBoltEntity.fire(world, this, aim.subtract(side), 6, 0.65f);
            }
            playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.65f);
        }
        if (--teleportTicks <= 0 && pulseWindup == 0) {
            teleportTicks = isEnraged() ? 130 : 200;
            if (squaredDistanceTo(target) < 32 * 32) teleportNear(target, world);
        }
        if (--summonTicks <= 0) {
            summonTicks = isEnraged() ? 240 : 340;
            summonWisps(world, target);
        }
        if (--pulseTicks <= 0 && squaredDistanceTo(target) < 9 * 9 && pulseWindup == 0) {
            pulseTicks = isEnraged() ? 160 : 220;
            pulseWindup = 30; // 1.5 seconds to back away or jump.
            bossBar.setName(Text.translatable("boss.riftborn.pulse_warning"));
            playSound(SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, 1.3f, 0.7f);
        }
    }

    private void enterSecondPhase(ServerWorld world) {
        dataTracker.set(ENRAGED, true);
        projectileTicks = 35;
        summonTicks = 60;
        RiftEffects.burst(world, getPos().add(0, 2, 0), 100, 2.0);
        playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.3f);
        for (ServerPlayerEntity player : bossBar.getPlayers()) player.sendMessage(Text.translatable("boss.riftborn.second_phase"), true);
    }

    private void teleportNear(LivingEntity target, ServerWorld world) {
        Vec3d previous = getPos();
        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double x = target.getX() + Math.cos(angle) * 6;
            double z = target.getZ() + Math.sin(angle) * 6;
            if (teleport(x, target.getY() + 3, z, false)) {
                RiftEffects.teleport(world, previous);
                RiftEffects.teleport(world, getPos());
                return;
            }
        }
    }

    private void summonWisps(ServerWorld world, LivingEntity target) {
        long alive = world.getEntitiesByClass(RiftWispEntity.class, getBoundingBox().expand(48),
                wisp -> getUuid().equals(wisp.getSummoner()) && wisp.isAlive()).size();
        int limit = isEnraged() ? 5 : 3;
        for (int i = 0; i < (isEnraged() ? 2 : 1) && alive < limit; i++) {
            BlockPos pos = getBlockPos().add(random.nextInt(7) - 3, 3, random.nextInt(7) - 3);
            if (!world.isSpaceEmpty(ModEntities.RIFT_WISP.getDimensions().getBoxAt(Vec3d.ofBottomCenter(pos)))) continue;
            RiftWispEntity wisp = ModEntities.RIFT_WISP.spawn(world, pos, SpawnReason.MOB_SUMMONED);
            if (wisp != null) {
                wisp.setSummoner(getUuid());
                wisp.setTarget(target);
                RiftEffects.burst(world, wisp.getPos(), 20, 0.5);
                alive++;
            }
        }
    }

    private void drawPulseWarning(ServerWorld world) {
        if (pulseWindup % 3 != 0) return;
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI / 16;
            world.spawnParticles(ModParticles.RIFT_MOTE, getX() + Math.cos(angle) * 5.5,
                    getY() + 0.15, getZ() + Math.sin(angle) * 5.5, 1, 0, 0.02, 0, 0);
        }
    }

    private void releasePulse(ServerWorld world) {
        bossBar.setName(getDisplayName());
        RiftEffects.burst(world, getPos().add(0, 0.3, 0), 75, 2);
        playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 0.9f, 0.6f);
        for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(5.5, 1, 5.5),
                player -> !player.isCreative() && !player.isSpectator() && player.isOnGround())) {
            Vec3d delta = player.getPos().subtract(getPos());
            if (delta.horizontalLengthSquared() <= 5.5 * 5.5 && getVisibilityCache().canSee(player)) {
                player.damage(getDamageSources().mobAttack(this), isEnraged() ? 10 : 8);
                player.takeKnockback(1.2, -delta.x, -delta.z);
                player.velocityModified = true;
            }
        }
    }

    @Override public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bossBar.addPlayer(player);
    }
    @Override public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
    }
    @Override public void onDeath(DamageSource source) {
        if (getWorld() instanceof ServerWorld world) {
            RiftEffects.burst(world, getPos().add(0, 1.5, 0), 180, 3);
            playSound(SoundEvents.BLOCK_END_PORTAL_SPAWN, 0.8f, 1.2f);
            world.getEntitiesByClass(RiftWispEntity.class, getBoundingBox().expand(96),
                    wisp -> getUuid().equals(wisp.getSummoner())).forEach(RiftWispEntity::discard);
            for (ServerPlayerEntity player : bossBar.getPlayers()) player.sendMessage(Text.translatable("boss.riftborn.defeated"), false);
            bossBar.clearPlayers();
        }
        super.onDeath(source);
    }
    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("RiftEnraged", isEnraged());
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        dataTracker.set(ENRAGED, nbt.getBoolean("RiftEnraged"));
        bossBar.setName(getDisplayName());
    }
    @Override public boolean canImmediatelyDespawn(double distanceSquared) { return false; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_WARDEN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_WITHER_DEATH; }
}
