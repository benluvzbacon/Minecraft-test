package dev.riftborn.awakening.entity;
import dev.riftborn.awakening.AbyssFx;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
public final class AbyssStalkerEntity extends AbyssHostileEntity {
    private int vanish = 130, invisibleTicks;
    public AbyssStalkerEntity(EntityType<? extends AbyssStalkerEntity> t, World w) {
        super(t, w);
    }
    public static DefaultAttributeContainer.Builder createAttributes() {
        return attributes(42, 0.38, 7, 3, 0.3);
    }
    @Override
    protected void mobTick() {
        super.mobTick();
        if (invisibleTicks > 0 && --invisibleTicks == 0)
            setInvisible(false);
        var target = getTarget();
        if (target == null || !target.isAlive())
            return;
        if (--vanish <= 0) {
            vanish = 160;
            if (squaredDistanceTo(target) > 9) {
                invisibleTicks = 40;
                setInvisible(true);
                AbyssFx.burst((ServerWorld) getWorld(), getPos().add(0, 0.6, 0), 8, 0.4);
                playSound(SoundEvents.ENTITY_PHANTOM_FLAP, 0.5f, 0.7f);
            }
        }
        if (invisibleTicks > 0 && squaredDistanceTo(target) < 25 && isOnGround()
            && getVisibilityCache().canSee(target)) {
            Vec3d d = target.getPos().subtract(getPos()).normalize();
            setVelocity(d.x * 0.4, 0.32, d.z * 0.4);
        }
        if (squaredDistanceTo(target) < 4) {
            invisibleTicks = 0;
            setInvisible(false);
        }
    }
    @Override
    public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound n) {
        super.writeCustomDataToNbt(n);
        n.putInt("AbyssVanishTicks", invisibleTicks);
    }
    @Override
    public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound n) {
        super.readCustomDataFromNbt(n);
        invisibleTicks = Math.clamp(n.getInt("AbyssVanishTicks"), 0, 40);
        setInvisible(invisibleTicks > 0);
    }
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PHANTOM_AMBIENT;
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.ENTITY_PHANTOM_HURT;
    }
}
