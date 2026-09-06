package dev.riftborn.awakening.entity;
import dev.riftborn.awakening.AbyssFx;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
public final class AbyssalWardenEntity extends AbyssHostileEntity {
    private int cooldown = 80, charge;
    private Vec3d locked;
    public AbyssalWardenEntity(EntityType<? extends AbyssalWardenEntity> t, World w) {
        super(t, w);
        experiencePoints = 28;
    }
    public static DefaultAttributeContainer.Builder createAttributes() {
        return attributes(90, 0.25, 12, 10, 0.8).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7);
    }
    @Override
    public int visualState() {
        return charge > 0 ? 1 : 0;
    }
    @Override
    protected void mobTick() {
        super.mobTick();
        var target = getTarget();
        var w = (ServerWorld) getWorld();
        if (charge > 0) {
            if (charge % 10 == 0)
                AbyssFx.line(w, getEyePos(), locked, 12);
            if (--charge == 0 && target != null && target.getPos().squaredDistanceTo(locked) < 16
                && CombatEffects.visible(w, this, getEyePos(), target.getEyePos())) {
                target.damage(w.getDamageSources().mobAttack(this), 12);
                AbyssFx.line(w, getEyePos(), target.getEyePos(), 28);
            }
        }
        if (target != null && --cooldown <= 0 && squaredDistanceTo(target) < 28 * 28
            && getVisibilityCache().canSee(target)) {
            cooldown = 150;
            charge = 30;
            locked = target.getEyePos();
            playSound(SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 0.7f, 1.25f);
        }
    }
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WARDEN_HEARTBEAT;
    }
}
