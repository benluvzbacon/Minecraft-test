package dev.riftborn.awakening.block;
import com.mojang.serialization.MapCodec;import dev.riftborn.awakening.*;import dev.riftborn.awakening.entity.AbyssBossEntity;
import net.minecraft.block.*;import net.minecraft.entity.*;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.item.*;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.server.world.ServerWorld;import net.minecraft.text.Text;import net.minecraft.util.*;import net.minecraft.util.hit.BlockHitResult;import net.minecraft.util.math.*;import net.minecraft.world.*;
public final class AbyssAltarBlock extends Block {
    public static final MapCodec<AbyssAltarBlock> CODEC=createCodec(AbyssAltarBlock::new);
    public AbyssAltarBlock(Settings s){super(s);}@Override public MapCodec<AbyssAltarBlock> getCodec(){return CODEC;}
    private int tier(){return this==AbyssBlocks.COLOSSUS_ALTAR?0:this==AbyssBlocks.ARCHITECT_ALTAR?1:2;}
    @Override protected ActionResult onUse(BlockState s,World w,BlockPos pos,PlayerEntity p,BlockHitResult h){if(!w.isClient)p.sendMessage(Text.translatable("awakening.riftborn.altar_hint_"+tier()),false);return ActionResult.success(w.isClient);}
    @Override protected ItemActionResult onUseWithItem(ItemStack stack,BlockState s,World world,BlockPos pos,PlayerEntity user,Hand hand,BlockHitResult hit){
        if(!(user instanceof ServerPlayerEntity p)||!(world instanceof ServerWorld w))return ItemActionResult.SUCCESS;
        if(!w.getRegistryKey().equals(AbyssWorlds.ABYSS)){p.sendMessage(Text.translatable("awakening.riftborn.altar_world"),true);return ItemActionResult.CONSUME;}
        if(w.getDifficulty()==Difficulty.PEACEFUL){p.sendMessage(Text.translatable("awakening.riftborn.altar_peaceful"),true);return ItemActionResult.CONSUME;}
        if(tier()==2&&stack.isOf(AbyssItems.COLLAPSE_CATALYST)){
            if(AwakeningEvents.startCollapse(w,pos,p)){if(!AbyssGear.creative(p))stack.decrement(1);}return ItemActionResult.CONSUME;}
        Item offering=tier()==0?AbyssItems.ABYSSAL_CORE:tier()==1?AbyssItems.TITAN_CORE:AbyssItems.SOVEREIGN_SIGIL;
        if(!stack.isOf(offering))return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var state=AwakeningState.get(p.getServer());int required=tier()==0?AwakeningState.ABYSS:tier()==1?AwakeningState.COLOSSUS:AwakeningState.COLOSSUS|AwakeningState.ARCHITECT;
        if(!AbyssGear.creative(p)&&!state.has(p.getUuid(),required)){p.sendMessage(Text.translatable("awakening.riftborn.altar_progress"),false);return ItemActionResult.CONSUME;}
        if(!w.getEntitiesByClass(AbyssBossEntity.class,new Box(pos).expand(128),AbyssBossEntity::isAlive).isEmpty()){p.sendMessage(Text.translatable("awakening.riftborn.altar_busy"),true);return ItemActionResult.CONSUME;}
        EntityType<? extends AbyssBossEntity> type=tier()==0?AbyssEntities.COLOSSUS:tier()==1?AbyssEntities.ARCHITECT:AbyssEntities.SOVEREIGN;
        BlockPos spawn=pos.add(0,1,7);var boss=type.create(w);if(boss==null)return ItemActionResult.CONSUME;
        boss.refreshPositionAndAngles(Vec3d.ofBottomCenter(spawn),0,0);
        if(!w.isSpaceEmpty(boss,boss.getBoundingBox())||!w.getOtherEntities(null,boss.getBoundingBox(),Entity::isAlive).isEmpty()){boss.discard();p.sendMessage(Text.translatable("awakening.riftborn.altar_space"),true);return ItemActionResult.CONSUME;}
        boss.setArena(pos.up());boss.setTarget(p);if(w.spawnEntity(boss)){if(!AbyssGear.creative(p))stack.decrement(1);AbyssFx.burst(w,boss.getPos().add(0,2,0),48,2);}
        return ItemActionResult.CONSUME;
    }
}
