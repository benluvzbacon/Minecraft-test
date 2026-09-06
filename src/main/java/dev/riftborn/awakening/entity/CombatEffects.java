package dev.riftborn.awakening.entity;
import dev.riftborn.awakening.*;import net.minecraft.entity.*;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.server.world.ServerWorld;import net.minecraft.util.hit.HitResult;import net.minecraft.util.math.*;import net.minecraft.world.RaycastContext;
/** Bounded, line-of-sight checked combat effects. Never edits terrain. */
public final class CombatEffects {
    public static boolean visible(ServerWorld w,LivingEntity caster,Vec3d a,Vec3d b){return w.raycast(new RaycastContext(a,b,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,caster)).getType()==HitResult.Type.MISS;}
    public static void pulse(ServerWorld w,LivingEntity caster,Vec3d center,double radius,float damage,boolean groundOnly,double safeInner){
        AbyssFx.ring(w,center.add(0,0.2,0),radius,28);
        for(var p:w.getEntitiesByClass(PlayerEntity.class,new Box(center,center).expand(radius,12,radius),p->p.isAlive()&&!p.isSpectator()&&!p.getAbilities().invulnerable)) {
            Vec3d offset=p.getPos().subtract(center);double horizontal=offset.x*offset.x+offset.z*offset.z;
            if(horizontal>radius*radius||horizontal<safeInner*safeInner||(groundOnly&&!p.isOnGround()))continue;
            if(!visible(w,caster,center.add(0,1.2,0),p.getEyePos()))continue;
            if(p.damage(w.getDamageSources().mobAttack(caster),damage)){Vec3d direction=offset.normalize();p.takeKnockback(0.7,-direction.x,-direction.z);p.velocityModified=true;}
        }
    }
    private CombatEffects(){}
}
