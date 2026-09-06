package dev.riftborn.block;

import com.mojang.serialization.MapCodec;
import dev.riftborn.dimension.RiftDimensions;
import dev.riftborn.effect.RiftEffects;
import dev.riftborn.entity.RiftGuardianEntity;
import dev.riftborn.registry.ModEntities;
import dev.riftborn.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

public final class GuardianAltarBlock extends Block {
    public static final MapCodec<GuardianAltarBlock> CODEC = createCodec(GuardianAltarBlock::new);
    public GuardianAltarBlock(Settings settings) { super(settings); }
    @Override public MapCodec<GuardianAltarBlock> getCodec() { return CODEC; }

    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) player.sendMessage(Text.translatable("message.riftborn.altar.core_required"), true);
        return ActionResult.success(world.isClient);
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                            PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!stack.isOf(ModItems.RIFT_CORE)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(world instanceof ServerWorld serverWorld)) return ItemActionResult.SUCCESS;
        if (!world.getRegistryKey().equals(RiftDimensions.WORLD)) {
            player.sendMessage(Text.translatable("message.riftborn.altar.wrong_dimension"), true);
        } else if (world.getDifficulty() == Difficulty.PEACEFUL) {
            player.sendMessage(Text.translatable("message.riftborn.altar.peaceful"), true);
        } else if (!world.getEntitiesByClass(RiftGuardianEntity.class, new Box(pos).expand(96), RiftGuardianEntity::isAlive).isEmpty()) {
            player.sendMessage(Text.translatable("message.riftborn.altar.occupied"), true);
        } else {
            Vec3d spawn = Vec3d.ofBottomCenter(pos.up(2));
            Box box = ModEntities.RIFT_GUARDIAN.getDimensions().getBoxAt(spawn);
            if (!world.isSpaceEmpty(box)) {
                player.sendMessage(Text.translatable("message.riftborn.altar.obstructed"), true);
            } else {
                RiftGuardianEntity boss = ModEntities.RIFT_GUARDIAN.spawn(serverWorld, pos.up(2), SpawnReason.TRIGGERED);
                if (boss != null) {
                    boss.setPersistent();
                    boss.setTarget(player);
                    if (!player.isCreative()) stack.decrement(1);
                    RiftEffects.burst(serverWorld, boss.getPos().add(0, 1.5, 0), 100, 2);
                    player.sendMessage(Text.translatable("message.riftborn.altar.awakened"), false);
                }
            }
        }
        return ItemActionResult.CONSUME;
    }
}
