package dev.riftborn.awakening.entity;
import dev.riftborn.Riftborn;import dev.riftborn.awakening.AbyssFx;import net.minecraft.entity.*;import net.minecraft.entity.attribute.*;import net.minecraft.entity.data.*;import net.minecraft.entity.effect.*;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.item.Items;import net.minecraft.server.world.ServerWorld;import net.minecraft.sound.*;import net.minecraft.world.World;
public final class RiftEchoEntity extends AbyssHostileEntity {
    private static final TrackedData<Integer> MIRROR=DataTracker.registerData(RiftEchoEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final EntityAttributeModifier SPRINT=new EntityAttributeModifier(Riftborn.id("echo_sprint"),0.08,EntityAttributeModifier.Operation.ADD_VALUE);
    private int shot=80;
    public RiftEchoEntity(EntityType<? extends RiftEchoEntity> t,World w){super(t,w);}
    public static DefaultAttributeContainer.Builder createAttributes(){return attributes(46,0.29,7,4,0.3);}
    @Override protected void initDataTracker(DataTracker.Builder b){super.initDataTracker(b);b.add(MIRROR,0);}
    @Override public int visualState(){return dataTracker.get(MIRROR);}
    @Override protected void mobTick(){super.mobTick();if(!(getTarget() instanceof PlayerEntity p))return;boolean sprint=p.isSprinting();var speed=getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if(sprint&&!speed.hasModifier(SPRINT.id()))speed.addTemporaryModifier(SPRINT);if(!sprint)speed.removeModifier(SPRINT.id());
        if(!p.isOnGround()&&isOnGround()&&age%15==0)jump();
        boolean block=p.isUsingItem()&&p.getActiveItem().isOf(Items.SHIELD);dataTracker.set(MIRROR,block?2:sprint?1:0);
        if(block&&age%20==0)addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,30,0,false,false));
        if(--shot<=0&&p.isUsingItem()&&squaredDistanceTo(p)>16&&getVisibilityCache().canSee(p)){shot=90;AbyssBoltEntity.fire((ServerWorld)getWorld(),this,p.getEyePos(),7,0.8f);playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,0.6f,0.55f);}
    }
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM;}
}
