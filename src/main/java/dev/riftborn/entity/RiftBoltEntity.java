package dev.riftborn.entity;

import dev.riftborn.effect.RiftEffects;
import dev.riftborn.registry.ModEntities;
import dev.riftborn.registry.ModItems;
import dev.riftborn.registry.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** A non-explosive, non-griefing energy projectile with vanilla projectile networking. */
public final class RiftBoltEntity extends ThrownItemEntity {
    private float damage = 5;
    private int remainingLife = 100;

    public RiftBoltEntity(EntityType<? extends RiftBoltEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    public RiftBoltEntity(ServerWorld world, LivingEntity owner, float damage) {
        super(ModEntities.RIFT_BOLT, owner, world);
        this.damage = damage;
        setNoGravity(true);
    }

    public static void fire(ServerWorld world, LivingEntity owner, Vec3d aim, float damage, float speed) {
        RiftBoltEntity bolt = new RiftBoltEntity(world, owner, damage);
        Vec3d direction = aim.subtract(bolt.getPos()).normalize();
        bolt.setVelocity(direction.x, direction.y, direction.z, speed, 1.0f);
        world.spawnEntity(bolt);
    }

    @Override protected Item getDefaultItem() { return ModItems.RIFT_DUST; }

    @Override public void tick() {
        super.tick();
        if (getWorld().isClient) {
            getWorld().addParticle(ModParticles.RIFT_MOTE, getX(), getY(), getZ(), 0, 0, 0);
        } else if (--remainingLife <= 0) discard();
    }

    @Override protected boolean canHit(Entity entity) {
        return !(entity instanceof AbstractRiftHostileEntity) && !(entity instanceof RiftWispEntity)
                && super.canHit(entity);
    }

    @Override protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        if (!getWorld().isClient) {
            hit.getEntity().damage(getDamageSources().thrown(this, getOwner()), damage);
        }
    }

    @Override protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (getWorld() instanceof ServerWorld world) {
            RiftEffects.burst(world, getPos(), 14, 0.25);
            discard();
        }
    }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putFloat("RiftDamage", damage);
        nbt.putInt("RiftLife", remainingLife);
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("RiftDamage")) damage = Math.clamp(nbt.getFloat("RiftDamage"), 0, 20);
        if (nbt.contains("RiftLife")) remainingLife = Math.clamp(nbt.getInt("RiftLife"), 1, 100);
        setNoGravity(true);
    }
}
