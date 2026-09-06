package dev.riftborn.awakening;
import dev.riftborn.Riftborn;import dev.riftborn.effect.SafeTeleport;
import net.minecraft.entity.effect.StatusEffectInstance;import net.minecraft.entity.effect.StatusEffects;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.server.world.ServerWorld;import net.minecraft.sound.*;import net.minecraft.text.Text;import net.minecraft.util.math.*;import java.util.Optional;
/** Ground-skimming or low aerial dash. It never chooses a destination over an unbounded void. */
public final class RiftDash {
    public static Optional<Vec3d> destination(ServerWorld w,ServerPlayerEntity p,Vec3d look,double range){
        if(!Double.isFinite(range)||!Double.isFinite(look.x)||!Double.isFinite(look.z))return Optional.empty();
        Vec3d direction=new Vec3d(look.x,0,look.z).normalize();if(direction.lengthSquared()<0.1)return Optional.empty();
        Vec3d best=null;Box previous=p.getBoundingBox();
        for(double d=0.2;d<=range+0.001;d+=0.2){Vec3d delta=direction.multiply(d);Box box=p.getBoundingBox().offset(delta),sweep=box.union(previous);
            if(!loaded(w,sweep)||!SafeTeleport.isClear(w,p,sweep))break;previous=box;
            if(d>=1&&(SafeTeleport.hasFloor(w,p,box)||(p.getAbilities().flying&&supportedBelow(w,p,box))))best=p.getPos().add(delta);
        }return Optional.ofNullable(best);
    }
    private static boolean loaded(ServerWorld w,Box box){for(int x=MathHelper.floor(box.minX)>>4;x<=MathHelper.floor(box.maxX)>>4;x++)for(int z=MathHelper.floor(box.minZ)>>4;z<=MathHelper.floor(box.maxZ)>>4;z++)if(!w.getChunkManager().isChunkLoaded(x,z))return false;return true;}
    private static boolean supportedBelow(ServerWorld w,ServerPlayerEntity p,Box box){
        Vec3d foot=new Vec3d((box.minX+box.maxX)/2,box.minY,(box.minZ+box.maxZ)/2);
        var hit=w.raycast(new net.minecraft.world.RaycastContext(foot,foot.add(0,-24,0),net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,p));
        if(hit.getType()!=net.minecraft.util.hit.HitResult.Type.BLOCK)return false;
        Box landing=box.offset(0,hit.getPos().y-box.minY,0);
        return SafeTeleport.isClear(w,p,landing)&&SafeTeleport.hasFloor(w,p,landing);
    }
    public static boolean use(ServerPlayerEntity p){
        if(!Riftborn.CONFIG.abyssalDashEnabled||!AbyssGear.fullSet(p)||!p.isAlive()||p.hasVehicle()||p.isSleeping()||p.isFallFlying()||AbyssGear.creative(p)||p.isSpectator())return false;
        var state=AwakeningState.get(p.getServer());var data=state.player(p.getUuid());long now=p.getServer().getOverworld().getTime();if(data.dashReady>now)return false;
        var result=destination(p.getServerWorld(),p,Vec3d.fromPolar(0,p.getYaw()),12);
        if(result.isEmpty()){p.sendMessage(Text.translatable("awakening.riftborn.dash_blocked"),true);return false;}
        Vec3d start=p.getPos(),end=result.get();p.networkHandler.requestTeleport(end.x,end.y,end.z,p.getYaw(),p.getPitch());p.setVelocity(Vec3d.ZERO);p.fallDistance=0;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,60,0,false,false,true));int cooldown=AbyssGear.cooldown(p,Riftborn.CONFIG.abyssalDashCooldownTicks);
        data.dashReady=now+cooldown;state.markDirty();p.getItemCooldownManager().set(AbyssItems.ABYSSAL_BOOTS,cooldown);
        AbyssFx.line(p.getServerWorld(),start.add(0,1,0),end.add(0,1,0),20);p.getServerWorld().playSound(null,p.getBlockPos(),SoundEvents.ENTITY_ENDERMAN_TELEPORT,SoundCategory.PLAYERS,0.7f,0.8f);return true;
    }
    private RiftDash(){}
}
