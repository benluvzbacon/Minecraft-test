package dev.riftborn.awakening;
import dev.riftborn.Riftborn;import dev.riftborn.dimension.RiftDimensions;import dev.riftborn.registry.ModEntities;import dev.riftborn.awakening.entity.*;
import net.minecraft.entity.*;import net.minecraft.entity.attribute.*;import net.minecraft.entity.effect.*;import net.minecraft.entity.mob.MobEntity;import net.minecraft.server.MinecraftServer;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;import net.minecraft.text.Text;import net.minecraft.util.math.*;import net.minecraft.world.*;import java.util.*;
/** One bounded event per realm; no tick-time structure searches or unloaded-chunk spawning. */
public final class AwakeningEvents {
    public static int kind(ServerWorld w){return AbyssWorlds.isRealm(w)?AwakeningState.get(w.getServer()).event(w.getRegistryKey()).kind:0;}
    public static void sync(ServerPlayerEntity p){var state=AwakeningState.get(p.getServer());var e=state.event(p.getWorld().getRegistryKey());long now=p.getServer().getOverworld().getTime();AwakeningNetwork.event(p,e.kind,(int)Math.max(0,e.ends-now));}
    public static void tick(MinecraftServer server){long now=server.getOverworld().getTime();if(now%20!=0)return;var state=AwakeningState.get(server);
        for(var key:List.of(RiftDimensions.WORLD,AbyssWorlds.ABYSS)){var w=server.getWorld(key);if(w==null)continue;var e=state.event(key);var players=w.getPlayers(p->p.isAlive()&&!p.isSpectator()&&!AbyssGear.creative(p));
            if(e.kind!=0&&now>=e.ends){cleanup(w,e.center);e.kind=0;e.herald=null;state.markDirty();for(var p:w.getPlayers())sync(p);continue;}
            if(players.isEmpty())continue;
            if(e.nextStorm==0){e.nextStorm=now+Riftborn.CONFIG.stormIntervalTicks+w.random.nextInt(Riftborn.CONFIG.stormIntervalTicks);e.nextCollapse=now+Riftborn.CONFIG.collapseIntervalTicks;state.markDirty();}
            if(e.kind==0&&Riftborn.CONFIG.realmEventsEnabled&&state.awakened&&now>=e.nextStorm&&w.getDifficulty()!=Difficulty.PEACEFUL){start(w,players.getFirst().getBlockPos(),1,2400);e.nextStorm=now+Riftborn.CONFIG.stormIntervalTicks+w.random.nextInt(Riftborn.CONFIG.stormIntervalTicks);state.markDirty();}
            if(e.kind==0&&key.equals(AbyssWorlds.ABYSS)&&Riftborn.CONFIG.realmEventsEnabled&&now>=e.nextCollapse){
                for(var p:players)if(state.has(p.getUuid(),AwakeningState.COLOSSUS|AwakeningState.ARCHITECT)&&w.getDifficulty()!=Difficulty.PEACEFUL){start(w,p.getBlockPos(),2,1800);e.nextCollapse=now+Riftborn.CONFIG.collapseIntervalTicks;state.markDirty();break;}}
            if(e.kind!=0&&now>=e.nextWave){e.nextWave=now+(e.kind==2?240:400);state.markDirty();var focus=players.getFirst();
                var nearby=w.getEntitiesByClass(MobEntity.class,new Box(e.center).expand(128),m->m.getCommandTags().contains("riftborn_event")&&m.isAlive());
                if(nearby.size()<Riftborn.CONFIG.eventMobCap){int remaining=Math.min(3,Riftborn.CONFIG.eventMobCap-nearby.size());for(int i=0;i<remaining;i++){var type=key.equals(AbyssWorlds.ABYSS)?(w.random.nextBoolean()?AbyssEntities.WARDEN:AbyssEntities.REAVER):ModEntities.RIFT_STALKER;spawn(w,type,focus,e.center,e.kind==1);}}
                if(e.kind==2&&e.herald==null){var herald=spawn(w,AbyssEntities.HERALD,focus,e.center,false);if(herald!=null){e.herald=herald.getUuid();((AbyssBossEntity)herald).setArena(e.center);state.markDirty();}}
            }
            if(e.kind!=0&&now%100==0)for(var p:w.getPlayers())sync(p);
        }
        for(var p:server.getPlayerManager().getPlayerList()){
            if(AbyssGear.artifact(p,AbyssItems.ABYSSAL_EYE)&&AbyssWorlds.isRealm(p.getWorld()))p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,240,0,false,false));
        }
    }
    private static void start(ServerWorld w,BlockPos center,int kind,int duration){var state=AwakeningState.get(w.getServer());var e=state.event(w.getRegistryKey());long now=w.getServer().getOverworld().getTime();e.kind=kind;e.center=center;e.ends=now+duration;e.nextWave=now+100;e.herald=null;state.markDirty();
        for(var p:w.getPlayers()){p.sendMessage(Text.translatable(kind==2?"awakening.riftborn.collapse_begins":"awakening.riftborn.storm_begins"),false);sync(p);}w.playSound(null,center,SoundEvents.ENTITY_WARDEN_ROAR,SoundCategory.AMBIENT,0.65f,kind==2?0.5f:0.8f);}
    public static boolean startStorm(ServerWorld w,BlockPos center){if(!AbyssWorlds.isRealm(w)||kind(w)!=0)return false;start(w,center,1,2400);return true;}
    public static boolean startCollapse(ServerWorld w,BlockPos center,ServerPlayerEntity p){var state=AwakeningState.get(w.getServer());var e=state.event(w.getRegistryKey());long now=w.getServer().getOverworld().getTime();
        if(!w.getRegistryKey().equals(AbyssWorlds.ABYSS)||e.kind!=0||(!AbyssGear.creative(p)&&!state.has(p.getUuid(),AwakeningState.COLOSSUS|AwakeningState.ARCHITECT))){p.sendMessage(Text.translatable("awakening.riftborn.collapse_requirements"),false);return false;}
        // Ritual activation is an expensive deterministic alternative to waiting for the rare natural event.
        start(w,center.up(),2,1800);e.nextCollapse=now+Riftborn.CONFIG.collapseIntervalTicks;state.markDirty();return true;}
    public static void finishCollapse(ServerWorld w){var state=AwakeningState.get(w.getServer());var e=state.event(w.getRegistryKey());if(e.kind==2){cleanup(w,e.center);e.kind=0;e.ends=0;e.herald=null;state.markDirty();for(var p:w.getPlayers()){AwakeningProgress.grant(p,"the_collapse");p.sendMessage(Text.translatable("awakening.riftborn.collapse_won"),false);sync(p);}}}
    private static void cleanup(ServerWorld w,BlockPos center){
        for(var mob:w.getEntitiesByClass(MobEntity.class,new Box(center).expand(128),m->m.getCommandTags().contains("riftborn_event")&&m.isAlive()))mob.discard();
    }
    private static MobEntity spawn(ServerWorld w,EntityType<? extends MobEntity> type,ServerPlayerEntity target,BlockPos center,boolean storm){
        for(int i=0;i<12;i++){double angle=w.random.nextDouble()*Math.PI*2;int x=center.getX()+(int)(Math.cos(angle)*20),z=center.getZ()+(int)(Math.sin(angle)*20);
            if(!w.getChunkManager().isChunkLoaded(x>>4,z>>4))continue;int y=w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z);if(y<w.getBottomY()+32||Math.abs(y-target.getY())>48)continue;
            var mob=type.create(w);if(mob==null)return null;mob.refreshPositionAndAngles(x+0.5,y,z+0.5,0,0);if(!w.isSpaceEmpty(mob,mob.getBoundingBox())||!w.getOtherEntities(null,mob.getBoundingBox(),Entity::isAlive).isEmpty()){mob.discard();continue;}
            mob.addCommandTag("riftborn_event");if(storm){mob.addCommandTag("riftborn_storm");var hp=mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);if(hp!=null){hp.setBaseValue(hp.getBaseValue()*1.35);mob.setHealth(mob.getMaxHealth());}}
            mob.setTarget(target);w.spawnEntity(mob);AbyssFx.burst(w,mob.getPos(),16,0.8);return mob;
        }return null;
    }
    private AwakeningEvents(){}
}
