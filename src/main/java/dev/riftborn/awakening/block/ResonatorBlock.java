package dev.riftborn.awakening.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.state.StateManager;import net.minecraft.state.property.IntProperty;
import net.minecraft.sound.SoundCategory;import net.minecraft.sound.SoundEvents;import net.minecraft.util.ActionResult;import net.minecraft.util.hit.BlockHitResult;import net.minecraft.util.math.BlockPos;import net.minecraft.world.World;
public final class ResonatorBlock extends Block {
    public static final IntProperty NOTE=IntProperty.of("note",0,3);
    public static final MapCodec<ResonatorBlock> CODEC=createCodec(ResonatorBlock::new);
    public ResonatorBlock(Settings s){super(s);setDefaultState(getDefaultState().with(NOTE,0));}
    @Override public MapCodec<ResonatorBlock> getCodec(){return CODEC;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState>b){b.add(NOTE);}
    @Override protected ActionResult onUse(BlockState state,World w,BlockPos pos,PlayerEntity p,BlockHitResult hit){
        if(!w.isClient){int note=(state.get(NOTE)+1)%4;w.setBlockState(pos,state.with(NOTE,note),Block.NOTIFY_ALL);w.playSound(null,pos,SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),SoundCategory.BLOCKS,0.7f,0.6f+note*0.25f);}
        return ActionResult.success(w.isClient);
    }
}
