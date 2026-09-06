package dev.riftborn.block;

import com.mojang.serialization.MapCodec;
import dev.riftborn.dimension.RiftDimensions;
import dev.riftborn.dimension.RiftTravel;
import dev.riftborn.registry.ModItems;
import dev.riftborn.registry.ModParticles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** All anchors emit Rift signals; OPEN records whether the crossing has been stabilized. */
public final class RiftAnchorBlock extends Block {
    public static final MapCodec<RiftAnchorBlock> CODEC = createCodec(RiftAnchorBlock::new);
    public static final BooleanProperty OPEN = BooleanProperty.of("open");

    public RiftAnchorBlock(Settings settings) {
        super(settings);
        setDefaultState(stateManager.getDefaultState().with(OPEN, false));
    }
    @Override public MapCodec<RiftAnchorBlock> getCodec() { return CODEC; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(OPEN); }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                            PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!stack.isOf(ModItems.RIFT_CORE)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        interact(state, world, pos, player, stack);
        return ItemActionResult.success(world.isClient);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        return interact(state, world, pos, player, player.getMainHandStack());
    }

    private ActionResult interact(BlockState state, World world, BlockPos pos, PlayerEntity player, ItemStack stack) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.FAIL;
        boolean needsCore = !world.getRegistryKey().equals(RiftDimensions.WORLD) && !state.get(OPEN);
        if (needsCore && !stack.isOf(ModItems.RIFT_CORE)) {
            player.sendMessage(Text.translatable("message.riftborn.anchor.core_required"), true);
            return ActionResult.CONSUME;
        }
        if (RiftTravel.useAnchor(serverPlayer, pos)) {
            if (needsCore) {
                world.setBlockState(pos, state.with(OPEN, true), Block.NOTIFY_ALL);
                if (!player.isCreative()) stack.decrement(1);
            }
            return ActionResult.CONSUME;
        }
        return ActionResult.FAIL;
    }

    @Override public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        for (int i = 0; i < 3; i++) world.addParticle(ModParticles.RIFT_MOTE,
                pos.getX() + 0.5 + random.nextGaussian() * 0.45,
                pos.getY() + 1 + random.nextDouble(), pos.getZ() + 0.5 + random.nextGaussian() * 0.45,
                0, 0.025, 0);
    }
}
