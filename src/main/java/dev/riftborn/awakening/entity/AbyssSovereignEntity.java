package dev.riftborn.awakening.entity;
import dev.riftborn.awakening.*;import net.minecraft.entity.*;import net.minecraft.entity.attribute.*;import net.minecraft.nbt.NbtCompound;import net.minecraft.server.world.ServerWorld;import net.minecraft.text.Text;import net.minecraft.util.math.Vec3d;import net.minecraft.world.World;
public final class AbyssSovereignEntity extends AbyssBossEntity {
    private boolean finalAttack;
    public AbyssSovereignEntity(EntityType<? extends AbyssSovereignEntity> t,World w){super(t,w);experiencePoints=700;}
    public static DefaultAttributeContainer.Builder createAttributes(){return attributes(1000,0.27,20,14,1.3).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,1);}
    @Override public int bossKind(){return 2;}
    @Override protected int phaseForHealth(){return getHealth()<=getMaxHealth()*0.33f?3:getHealth()<=getMaxHealth()*0.67f?2:1;}
    public boolean usedFinalAttack(){return finalAttack;}
    @Override protected void attackPattern(ServerWorld w,LivingEntity target){
        if(phase()==3&&!finalAttack&&getHealth()<=getMaxHealth()*0.18f){finalAttack=true;Vec3d center=Vec3d.ofBottomCenter(arenaCenter());pulse(center,28,30,false,3.5,100);
            for(var p:w.getPlayers(p->p.squaredDistanceTo(center)<40*40))p.sendMessage(Text.translatable("boss.riftborn.last_silence_hint"),false);return;}
        if(battleTicks%(phase()==3?35:phase()==2?50:70)==0)bolts(w,target,phase()==3?5:3,phase()==3?11:9,0.9f);
        if(battleTicks%(phase()==3?130:200)==0){teleportNear(w,target);if(phase()>1)barriers(w);}
        if(battleTicks%170==0)pulse(target.getPos(),phase()==3?8:6,16,false,0,45);
        if(phase()>1&&battleTicks%180==0)summon(w,phase()==3?AbyssEntities.REAVER:AbyssEntities.ECHO,phase()==3?5:3);
    }
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putBoolean("AbyssLastSilence",finalAttack);}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);finalAttack=n.getBoolean("AbyssLastSilence");}
}
