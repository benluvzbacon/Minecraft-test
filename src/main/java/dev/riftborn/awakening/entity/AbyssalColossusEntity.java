package dev.riftborn.awakening.entity;
import dev.riftborn.awakening.AbyssEntities;import net.minecraft.entity.*;import net.minecraft.entity.attribute.*;import net.minecraft.server.world.ServerWorld;import net.minecraft.world.World;
public final class AbyssalColossusEntity extends AbyssBossEntity {
    public AbyssalColossusEntity(EntityType<? extends AbyssalColossusEntity> t,World w){super(t,w);experiencePoints=300;}
    public static DefaultAttributeContainer.Builder createAttributes(){return attributes(520,0.18,24,12,2.2).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,1);}
    @Override public int bossKind(){return 0;}
    @Override protected void attackPattern(ServerWorld w,LivingEntity target){if(battleTicks%(phase()==2?55:80)==0)bolts(w,target,phase()==2?3:1,10,0.75f);
        if(battleTicks%150==0)pulse(getPos(),12,18,true,0,40);
        if(battleTicks%240==0)summon(w,AbyssEntities.STALKER,phase()==2?4:2);
        if(phase()==2&&battleTicks%210==0)pulse(target.getPos(),6,14,false,0,40);}
}
