package dev.riftborn.item;

import dev.riftborn.Riftborn;
import dev.riftborn.effect.RiftEffects;
import dev.riftborn.effect.SafeTeleport;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public final class RiftbladeItem extends SwordItem {
    public RiftbladeItem(Settings settings) {
        super(RiftToolMaterial.INSTANCE, settings.attributeModifiers(
                SwordItem.createAttributeModifiers(RiftToolMaterial.INSTANCE, 3, -2.4f)));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);
        if (!(user instanceof ServerPlayerEntity player) || user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        if (player.hasVehicle() || player.isFallFlying() || player.isSleeping()) {
            player.sendMessage(Text.translatable("message.riftborn.blink.unavailable"), true);
            return TypedActionResult.fail(stack);
        }
        ServerWorld serverWorld = player.getServerWorld();
        Optional<Vec3d> target = SafeTeleport.findBlinkDestination(serverWorld, player,
                // Read authoritative body yaw, not interpolated head rotation (which may
                // still represent the previous tick when a turn and use arrive together).
                Vec3d.fromPolar(0, player.getYaw()), Riftborn.CONFIG.blinkDistance);
        if (target.isEmpty()) {
            player.sendMessage(Text.translatable("message.riftborn.blink.blocked"), true);
            user.getItemCooldownManager().set(this, 10);
            return TypedActionResult.fail(stack);
        }
        Vec3d origin = player.getPos();
        Vec3d destination = target.get();
        // Vanilla's server network handler teleports and synchronizes the authoritative position.
        player.networkHandler.requestTeleport(destination.x, destination.y, destination.z, player.getYaw(), player.getPitch());
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0;
        user.getItemCooldownManager().set(this, Riftborn.CONFIG.blinkCooldownTicks);
        stack.damage(2, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        RiftEffects.teleport(serverWorld, origin);
        RiftEffects.teleport(serverWorld, destination);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.riftborn.riftblade.tooltip").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("item.riftborn.riftblade.safety").formatted(Formatting.GRAY));
    }
}
