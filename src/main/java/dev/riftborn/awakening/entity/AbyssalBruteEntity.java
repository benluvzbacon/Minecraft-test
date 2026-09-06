package dev.riftborn.awakening.entity;
import dev.riftborn.awakening.AbyssFx;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.world.World;
public final class AbyssalBruteEntity extends AbyssHostileEntity {
    private int stomp = 100, charge;
    public AbyssalBruteEntity(EntityType<? extends AbyssalBruteEntity> t, World w) {
        super(t, w);
        experiencePoints = 22;
    }
    public static DefaultAttributeContainer.Builder createAttributes() {
        return attributes(150, 0.17, 16, 8, 2.2).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.95);
    }
    @Override
    protected void mobTick() {
        super.mobTick();
        var w = (ServerWorld) getWorld();
        if (charge > 0) {
            if (charge % 8 == 0)
                AbyssFx.ring(w, getPos().add(0, 0.15, 0), 5, 12);
            if (--charge == 0)
                CombatEffects.pulse(w, this, getPos(), 5, 10, true, 0);
        }
        if (getTarget() != null && --stomp <= 0 && squaredDistanceTo(getTarget()) < 64) {
            stomp = 140;
            charge = 28;
            playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 0.8f, 0.7f);
        }
    }
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_RAVAGER_AMBIENT;
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.ENTITY_IRON_GOLEM_HURT;
    }
}
