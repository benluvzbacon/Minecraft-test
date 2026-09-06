package dev.riftborn.awakening.entity;
import net.minecraft.entity.*;import net.minecraft.entity.attribute.*;import net.minecraft.entity.ai.control.FlightMoveControl;import net.minecraft.entity.ai.pathing.*;import net.minecraft.entity.ai.goal.*;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.entity.damage.DamageSource;import net.minecraft.server.world.ServerWorld;import net.minecraft.sound.*;import net.minecraft.util.math.Vec3d;import net.minecraft.world.World;import java.util.EnumSet;
public final class VoidReaverEntity extends AbyssHostileEntity {
    public VoidReaverEntity(EntityType<? extends VoidReaverEntity> t,World w){super(t,w);moveControl=new FlightMoveControl(this,30,true);setNoGravity(true);}
    public static DefaultAttributeContainer.Builder createAttributes(){return attributes(36,0.28,6,2,0).add(EntityAttributes.GENERIC_FLYING_SPEED,0.4);}
    @Override protected EntityNavigation createNavigation(World w){var n=new BirdNavigation(this,w);n.setCanSwim(false);n.setCanPathThroughDoors(false);return n;}
    @Override protected void initGoals(){goalSelector.add(1,new StrafeGoal());targetSelector.add(1,new RevengeGoal(this));targetSelector.add(2,new ActiveTargetGoal<>(this,PlayerEntity.class,true));}
    @Override public void tick(){setNoGravity(true);super.tick();}
    @Override public boolean handleFallDamage(float d,float m,DamageSource s){return false;}
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.ENTITY_VEX_AMBIENT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.ENTITY_VEX_DEATH;}
    private final class StrafeGoal extends Goal {
        int move,shot=45;private StrafeGoal(){setControls(EnumSet.of(Control.MOVE,Control.LOOK));}
        @Override public boolean canStart(){return true;}@Override public boolean shouldRunEveryTick(){return true;}
        @Override public void tick(){var target=getTarget();if(target==null||!target.isAlive()){if(--move<=0){move=45;getMoveControl().moveTo(getX()+random.nextInt(13)-6,getY()+random.nextInt(7)-3,getZ()+random.nextInt(13)-6,0.6);}return;}
            getLookControl().lookAt(target,90,90);if(--move<=0){move=16+random.nextInt(14);double a=Math.atan2(getZ()-target.getZ(),getX()-target.getX())+(random.nextBoolean()?0.7:-0.7);double radius=12+random.nextInt(7);Vec3d destination=target.getPos().add(Math.cos(a)*radius,4+random.nextInt(4),Math.sin(a)*radius);getNavigation().startMovingTo(destination.x,destination.y,destination.z,1);if(getNavigation().isIdle())getMoveControl().moveTo(destination.x,destination.y,destination.z,1);}
            if(--shot<=0&&squaredDistanceTo(target)<32*32&&getVisibilityCache().canSee(target)){shot=65;var aim=target.getPos().add(0,target.getHeight()*0.5,0);AbyssBoltEntity.fire((ServerWorld)getWorld(),VoidReaverEntity.this,aim,6,0.75f);AbyssBoltEntity.fire((ServerWorld)getWorld(),VoidReaverEntity.this,aim.add(target.getVelocity().multiply(8)),6,0.7f);playSound(SoundEvents.ENTITY_ALLAY_HURT,0.6f,0.7f);}
        }
    }
}
