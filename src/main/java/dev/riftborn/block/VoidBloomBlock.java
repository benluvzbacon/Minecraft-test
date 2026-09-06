package dev.riftborn.block;

import com.mojang.serialization.MapCodec;
import dev.riftborn.registry.ModBlocks;
import dev.riftborn.registry.ModParticles;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class VoidBloomBlock extends PlantBlock {
    public static final MapCodec<VoidBloomBlock> CODEC = createCodec(VoidBloomBlock::new);
    private static final VoxelShape SHAPE = createCuboidShape(3, 0, 3, 13, 13, 13);

    public VoidBloomBlock(Settings settings) { super(settings); }
    @Override public MapCodec<VoidBloomBlock> getCodec() { return CODEC; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) { return SHAPE; }
    @Override protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(ModBlocks.RIFT_STONE) || floor.isOf(Blocks.BLACKSTONE)
                || floor.isOf(Blocks.END_STONE) || super.canPlantOnTop(floor, world, pos);
    }
    @Override public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(3) == 0) world.addParticle(ModParticles.RIFT_MOTE,
                pos.getX() + random.nextDouble(), pos.getY() + 0.6, pos.getZ() + random.nextDouble(), 0, 0.015, 0);
    }
}
