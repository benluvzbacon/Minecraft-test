package dev.riftborn.awakening.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
public final class TemporaryBarrierBlock extends Block {
    public static final MapCodec<TemporaryBarrierBlock> CODEC = createCodec(TemporaryBarrierBlock::new);
    public TemporaryBarrierBlock(Settings s) {
        super(s);
    }
    @Override
    public MapCodec<TemporaryBarrierBlock> getCodec() {
        return CODEC;
    }
    @Override
    protected void onBlockAdded(
        BlockState s, net.minecraft.world.World w, BlockPos pos, BlockState old, boolean notify) {
        if (!w.isClient)
            w.scheduleBlockTick(pos, this, 120);
    }
    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        world.removeBlock(pos, false);
    }
}
