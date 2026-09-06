package dev.riftborn.awakening.block;
import com.mojang.serialization.MapCodec;
import dev.riftborn.awakening.*;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public final class AbyssGateBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    public static final MapCodec<AbyssGateBlock> CODEC = createCodec(AbyssGateBlock::new);
    public AbyssGateBlock(Settings s) {
        super(s);
        setDefaultState(getDefaultState().with(ACTIVE, false));
    }
    @Override
    public MapCodec<AbyssGateBlock> getCodec() {
        return CODEC;
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
        b.add(ACTIVE);
    }
    @Override
    protected ActionResult onUse(BlockState s, World w, BlockPos pos, PlayerEntity p, BlockHitResult hit) {
        if (p instanceof ServerPlayerEntity player)
            AbyssTravel.use(player, pos, ItemStack.EMPTY);
        return ActionResult.success(w.isClient);
    }
    @Override
    protected ItemActionResult onUseWithItem(
        ItemStack stack, BlockState s, World w, BlockPos pos, PlayerEntity p, Hand hand, BlockHitResult hit) {
        if (p instanceof ServerPlayerEntity player)
            AbyssTravel.use(player, pos, stack);
        return ItemActionResult.success(w.isClient);
    }
}
