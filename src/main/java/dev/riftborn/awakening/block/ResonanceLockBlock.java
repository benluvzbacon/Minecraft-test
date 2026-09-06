package dev.riftborn.awakening.block;
import com.mojang.serialization.MapCodec;
import dev.riftborn.awakening.*;
import dev.riftborn.registry.ModItems;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
public final class ResonanceLockBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.of("open");
    public static final MapCodec<ResonanceLockBlock> CODEC = createCodec(ResonanceLockBlock::new);
    public ResonanceLockBlock(Settings s) {
        super(s);
        setDefaultState(getDefaultState().with(OPEN, false));
    }
    @Override
    public MapCodec<ResonanceLockBlock> getCodec() {
        return CODEC;
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
        b.add(OPEN);
    }
    public static boolean solved(World w, BlockPos pos) {
        for (BlockRotation rotation : BlockRotation.values()) {
            BlockPos[] offsets = {new BlockPos(-3, 0, 0), new BlockPos(0, 0, -3), new BlockPos(3, 0, 0)};
            int[] tones = {1, 3, 2};
            boolean ok = true;
            for (int i = 0; i < 3; i++) {
                var s = w.getBlockState(pos.add(offsets[i].rotate(rotation)));
                if (!s.isOf(AbyssBlocks.RESONATOR) || s.get(ResonatorBlock.NOTE) != tones[i])
                    ok = false;
            }
            if (ok)
                return true;
        }
        return false;
    }
    @Override
    protected ActionResult onUse(BlockState s, World w, BlockPos pos, PlayerEntity p, BlockHitResult hit) {
        if (w instanceof ServerWorld sw && p instanceof ServerPlayerEntity player) {
            if (!s.get(OPEN) && solved(w, pos)) {
                w.setBlockState(pos, s.with(OPEN, true), Block.NOTIFY_ALL);
                dropStack(w, pos.up(), new ItemStack(AbyssItems.RESONANT_SIGIL));
                AwakeningProgress.unlock(player, AwakeningState.TEMPLE, "three_voices");
                AbyssFx.burst(sw, Vec3d.ofCenter(pos), 28, 1);
            } else
                p.sendMessage(
                    Text.translatable(s.get(OPEN) ? "awakening.riftborn.lock_open" : "awakening.riftborn.lock_hint"),
                    false);
        }
        return ActionResult.success(w.isClient);
    }
    @Override
    protected ItemActionResult onUseWithItem(
        ItemStack stack, BlockState s, World w, BlockPos pos, PlayerEntity p, Hand hand, BlockHitResult hit) {
        if (!s.get(OPEN) || !stack.isOf(ModItems.RIFT_CORE))
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!w.isClient) {
            if (!p.isCreative())
                stack.decrement(1);
            dropStack(w, pos.up(), new ItemStack(AbyssItems.RESONANT_SIGIL));
        }
        return ItemActionResult.success(w.isClient);
    }
}
