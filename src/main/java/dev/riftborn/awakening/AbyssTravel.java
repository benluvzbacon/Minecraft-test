package dev.riftborn.awakening;
import dev.riftborn.awakening.block.AbyssGateBlock;
import dev.riftborn.dimension.RiftDimensions;
import dev.riftborn.effect.SafeTeleport;
import net.minecraft.block.*;import net.minecraft.entity.Entity;import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.server.world.ServerWorld;import net.minecraft.text.Text;
import net.minecraft.util.math.*;import net.minecraft.world.*;import java.util.Optional;
public final class AbyssTravel {
    public static boolean use(ServerPlayerEntity p,BlockPos gate,ItemStack key){
        ServerWorld source=p.getServerWorld();boolean returning=source.getRegistryKey().equals(AbyssWorlds.ABYSS);
        if(!returning&&!source.getRegistryKey().equals(RiftDimensions.WORLD))return fail(p,"gate_wrong_world");
        if(p.hasVehicle()||p.hasPassengers()||p.hasPortalCooldown())return fail(p,"gate_settling");
        var state=AwakeningState.get(p.getServer());var data=state.player(p.getUuid());
        boolean unlock=!state.has(p.getUuid(),AwakeningState.ABYSS);
        if(!returning&&unlock&&!AbyssGear.creative(p)&&!key.isOf(AbyssItems.ABYSSAL_KEY))return fail(p,"gate_key");
        ServerWorld destination;BlockPos center;
        if(returning){destination=data.origin==null?p.getServer().getWorld(RiftDimensions.WORLD):p.getServer().getWorld(data.origin.dimension());
            if(destination==null)destination=p.getServer().getOverworld();center=data.origin==null?destination.getSpawnPos():data.origin.pos();}
        else {destination=p.getServer().getWorld(AbyssWorlds.ABYSS);if(destination==null)return fail(p,"gate_missing");center=arrival(destination,state);}
        Optional<Vec3d> landing=SafeTeleport.findLanding(destination,p,center,8);
        if(landing.isEmpty()&&returning){destination=p.getServer().getOverworld();landing=SafeTeleport.findLanding(destination,p,destination.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,destination.getSpawnPos()),8);}
        if(landing.isEmpty())return fail(p,"gate_blocked");
        Vec3d before=p.getPos();Entity moved=p.teleportTo(new TeleportTarget(destination,landing.get(),Vec3d.ZERO,p.getYaw(),p.getPitch(),TeleportTarget.ADD_PORTAL_CHUNK_TICKET));
        if(!(moved instanceof ServerPlayerEntity player))return false;
        if(!returning){data.origin=GlobalPos.create(source.getRegistryKey(),gate.up());if(unlock&&!AbyssGear.creative(p))key.decrement(1);
            AwakeningProgress.unlock(player,AwakeningState.ABYSS,"the_abyss");state.awakened=true;state.markDirty();
            source.setBlockState(gate,source.getBlockState(gate).with(AbyssGateBlock.ACTIVE,true),Block.NOTIFY_ALL);}
        player.resetPortalCooldown();player.fallDistance=0;AbyssFx.burst(source,before,28,0.7);AbyssFx.burst(destination,landing.get(),28,0.7);
        player.sendMessage(Text.translatable(returning?"awakening.riftborn.gate_return":"awakening.riftborn.gate_arrive"),false);return true;
    }
    public static BlockPos arrival(ServerWorld w,AwakeningState state){
        if(state.abyssLanding!=null)return state.abyssLanding;
        BlockPos center=new BlockPos(0,224,0);
        search:for(int r=0;r<=6;r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;int y=w.getTopY(Heightmap.Type.WORLD_SURFACE,x*16,z*16);
            if(y>48&&y<w.getTopY()-12){center=new BlockPos(x*16,y,z*16);break search;}}
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){var floor=center.add(x,-1,z);w.setBlockState(floor,AbyssBlocks.STONE.getDefaultState(),Block.NOTIFY_ALL);
            for(int y=0;y<6;y++)w.setBlockState(center.add(x,y,z),Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL);
            if((Math.abs(x)==6||Math.abs(z)==6)&&Math.abs(x)>1&&Math.abs(z)>1)w.setBlockState(floor.up(),Blocks.POLISHED_DEEPSLATE_WALL.getDefaultState(),Block.NOTIFY_ALL);}
        w.setBlockState(center.add(0,0,-4),AbyssBlocks.GATE.getDefaultState().with(AbyssGateBlock.ACTIVE,true),Block.NOTIFY_ALL);
        for(int x:new int[]{-4,4})for(int y=0;y<7;y++)w.setBlockState(center.add(x,y,-4),AbyssBlocks.LUMINOUS_SHALE.getDefaultState(),Block.NOTIFY_ALL);
        state.abyssLanding=center;state.markDirty();return center;
    }
    private static boolean fail(ServerPlayerEntity p,String key){p.sendMessage(Text.translatable("awakening.riftborn."+key),true);return false;}
    private AbyssTravel(){}
}
