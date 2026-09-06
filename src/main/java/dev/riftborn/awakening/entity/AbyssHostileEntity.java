package dev.riftborn.awakening.entity;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.*;
import net.minecraft.world.World;
import java.util.UUID;
/** Shared housekeeping only; combat roles are implemented by distinct subclasses. */
public abstract class AbyssHostileEntity extends HostileEntity {
    private UUID summoner;
    private int summonedLife = -1;
    protected AbyssHostileEntity(EntityType<? extends HostileEntity> type, World w) {
        super(type, w);
        experiencePoints = 12;
    }
    public static DefaultAttributeContainer.Builder attributes(
        double hp, double speed, double damage, double armor, double knockback) {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, hp)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, speed)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, damage)
            .add(EntityAttributes.GENERIC_ARMOR, armor)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40)
            .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, knockback);
    }
    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1, false));
        goalSelector.add(6, new WanderAroundFarGoal(this, 0.7));
        goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 14));
        goalSelector.add(8, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    public void summonedBy(UUID owner, int lifetime) {
        summoner = owner;
        summonedLife = Math.clamp(lifetime, 20, 2400);
        experiencePoints = 0;
        setPersistent();
    }
    public UUID summoner() {
        return summoner;
    }
    public int visualState() {
        return 0;
    }
    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && summonedLife > 0 && --summonedLife == 0)
            discard();
    }
    @Override
    public void writeCustomDataToNbt(NbtCompound n) {
        super.writeCustomDataToNbt(n);
        if (summoner != null)
            n.putUuid("AbyssSummoner", summoner);
        n.putInt("AbyssLife", summonedLife);
        n.putBoolean("AbyssSummoned", summoner != null);
    }
    @Override
    public void readCustomDataFromNbt(NbtCompound n) {
        super.readCustomDataFromNbt(n);
        summoner = n.containsUuid("AbyssSummoner") ? n.getUuid("AbyssSummoner") : null;
        summonedLife = n.contains("AbyssLife") ? n.getInt("AbyssLife") : -1;
        if (summoner != null)
            experiencePoints = 0;
    }
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE;
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.ENTITY_ENDERMAN_HURT;
    }
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK;
    }
}
