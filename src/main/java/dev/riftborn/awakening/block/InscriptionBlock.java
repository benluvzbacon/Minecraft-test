package dev.riftborn.awakening.block;
import com.mojang.serialization.MapCodec;
import dev.riftborn.awakening.*;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public final class InscriptionBlock extends Block {
    public static final IntProperty VERSE = IntProperty.of("verse", 0, 7);
    public static final MapCodec<InscriptionBlock> CODEC = createCodec(InscriptionBlock::new);
    public InscriptionBlock(Settings s) {
        super(s);
        setDefaultState(getDefaultState().with(VERSE, 0));
    }
    @Override
    public MapCodec<InscriptionBlock> getCodec() {
        return CODEC;
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
        b.add(VERSE);
    }
    @Override
    protected ActionResult onUse(BlockState s, World w, BlockPos pos, PlayerEntity p, BlockHitResult hit) {
        if (p instanceof ServerPlayerEntity player) {
            p.sendMessage(Text.translatable("lore.riftborn.verse_" + s.get(VERSE)), false);
            AwakeningProgress.unlock(player, AwakeningState.DISCOVERY, "deeper_than_darkness");
        }
        return ActionResult.success(w.isClient);
    }
}
