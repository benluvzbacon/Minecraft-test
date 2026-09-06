package dev.riftborn.awakening.block;
import com.mojang.serialization.MapCodec;
import dev.riftborn.awakening.AbyssBlocks;
import dev.riftborn.registry.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
public final class AbyssPlantBlock extends PlantBlock {
    public static final MapCodec<AbyssPlantBlock> CODEC=createCodec(AbyssPlantBlock::new);
    public AbyssPlantBlock(Settings s){super(s);}
    @Override public MapCodec<AbyssPlantBlock> getCodec(){return CODEC;}
    @Override protected boolean canPlantOnTop(BlockState floor,BlockView world,BlockPos pos){return floor.isOf(AbyssBlocks.STONE)||floor.isOf(AbyssBlocks.LUMINOUS_SHALE)||floor.isOf(ModBlocks.RIFT_STONE)||floor.isOf(Blocks.BLACKSTONE);}
    @Override protected void onEntityCollision(BlockState state,World world,BlockPos pos,Entity entity){
        if(!world.isClient && this==AbyssBlocks.BRAMBLE && entity.age%20==0) entity.damage(world.getDamageSources().cactus(),2);
    }
}
