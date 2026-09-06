package dev.riftborn.item;

import dev.riftborn.Riftborn;
import dev.riftborn.dimension.RiftDimensions;
import dev.riftborn.registry.ModParticles;
import dev.riftborn.world.RiftWorldgen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

/** An explicit coordinate/direction locator, not a misleading static custom compass needle. */
public final class RiftCompassItem extends DescribedItem {
    public RiftCompassItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);
        if (!(user instanceof ServerPlayerEntity player) || player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        player.getItemCooldownManager().set(this, Riftborn.CONFIG.compassCooldownTicks);
        boolean inRift = world.getRegistryKey().equals(RiftDimensions.WORLD);
        if (!inRift && !world.getRegistryKey().equals(World.OVERWORLD)) {
            player.sendMessage(Text.translatable("message.riftborn.compass.wrong_dimension"), true);
            return TypedActionResult.fail(stack);
        }
        boolean guardian = inRift && player.isSneaking();
        TagKey<Structure> tag = guardian ? RiftWorldgen.GUARDIAN_SHRINES : RiftWorldgen.RIFT_SIGNALS;
        ServerWorld serverWorld = player.getServerWorld();
        BlockPos target = serverWorld.locateStructure(tag, player.getBlockPos(), Riftborn.CONFIG.locateRadius, false);
        if (target == null) {
            player.sendMessage(Text.translatable("message.riftborn.compass.not_found"), false);
            return TypedActionResult.fail(stack);
        }
        Vec3d delta = Vec3d.ofCenter(target).subtract(player.getPos());
        long distance = Math.round(Math.sqrt(delta.x * delta.x + delta.z * delta.z));
        Text eastWest = Text.translatable(delta.x >= 0 ? "direction.riftborn.east" : "direction.riftborn.west");
        Text northSouth = Text.translatable(delta.z >= 0 ? "direction.riftborn.south" : "direction.riftborn.north");
        Text direction = Math.abs(delta.x) > Math.abs(delta.z) * 2 ? eastWest
                : Math.abs(delta.z) > Math.abs(delta.x) * 2 ? northSouth
                : Text.translatable("direction.riftborn.diagonal", northSouth, eastWest);
        player.sendMessage(Text.translatable("message.riftborn.compass.found",
                Text.translatable(guardian ? "location.riftborn.shrine" : "location.riftborn.rift"),
                target.getX(), target.getZ(), distance, direction), false);
        Vec3d step = new Vec3d(delta.x, 0, delta.z).normalize();
        for (int i = 1; i <= 12; i++) {
            Vec3d point = player.getPos().add(0, 1.2, 0).add(step.multiply(i * 0.6));
            serverWorld.spawnParticles(player, ModParticles.RIFT_MOTE, true, point.x, point.y, point.z,
                    2, 0.05, 0.1, 0.05, 0);
        }
        serverWorld.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS, 0.8f, 0.8f);
        return TypedActionResult.success(stack);
    }
}
