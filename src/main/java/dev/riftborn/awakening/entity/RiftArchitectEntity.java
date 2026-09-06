package dev.riftborn.awakening.entity;
import dev.riftborn.awakening.AbyssEntities;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
public final class RiftArchitectEntity extends AbyssBossEntity {
    public RiftArchitectEntity(EntityType<? extends RiftArchitectEntity> t, World w) {
        super(t, w);
        experiencePoints = 400;
    }
    public static DefaultAttributeContainer.Builder createAttributes() {
        return attributes(650, 0.25, 14, 10, 1).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.95);
    }
    @Override
    public int bossKind() {
        return 1;
    }
    @Override
    protected void attackPattern(ServerWorld w, LivingEntity target) {
        if (battleTicks % (phase() == 2 ? 40 : 65) == 0)
            bolts(w, target, 3, 8, 0.8f);
        if (battleTicks % 180 == 0)
            teleportNear(w, target);
        if (battleTicks % 220 == 0) {
            barriers(w);
            pulse(target.getPos(), 5, 14, false, 0, 35);
        }
        if (battleTicks % 160 == 0)
            summon(w, AbyssEntities.ECHO, phase() == 2 ? 5 : 3);
    }
}
