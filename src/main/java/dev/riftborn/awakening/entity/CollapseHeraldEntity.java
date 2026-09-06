package dev.riftborn.awakening.entity;
import dev.riftborn.awakening.AbyssEntities;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
public final class CollapseHeraldEntity extends AbyssBossEntity {
    public CollapseHeraldEntity(EntityType<? extends CollapseHeraldEntity> t, World w) {
        super(t, w);
        experiencePoints = 150;
    }
    public static DefaultAttributeContainer.Builder createAttributes() {
        return attributes(260, 0.3, 12, 8, 0.8).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9);
    }
    @Override
    public int bossKind() {
        return 3;
    }
    @Override
    protected void attackPattern(ServerWorld w, LivingEntity target) {
        if (battleTicks % 55 == 0)
            bolts(w, target, 3, 8, 0.75f);
        if (battleTicks % 160 == 0)
            pulse(getPos(), 9, 12, true, 0, 35);
        if (phase() > 1 && battleTicks % 240 == 0)
            summon(w, AbyssEntities.ECHO, 2);
    }
}
