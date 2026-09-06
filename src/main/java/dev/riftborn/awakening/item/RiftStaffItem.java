package dev.riftborn.awakening.item;
import dev.riftborn.awakening.*;
import dev.riftborn.awakening.entity.AbyssBoltEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.List;
public final class RiftStaffItem extends AwakeningItem {
    public RiftStaffItem() {
        super(new Settings().maxDamage(1500).fireproof().rarity(Rarity.EPIC));
    }
    public static int spell(ItemStack stack) {
        return Math.floorMod(
            stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getInt("RiftSpell"), 3);
    }
    @Override
    public TypedActionResult<ItemStack> use(World w, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (!(user instanceof ServerPlayerEntity p))
            return TypedActionResult.success(stack);
        int mode = spell(stack);
        if (p.isSneaking()) {
            var data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
            mode = (mode + 1) % 3;
            data.putInt("RiftSpell", mode);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
            p.sendMessage(Text.translatable("spell.riftborn." + mode), true);
            return TypedActionResult.success(stack);
        }
        if (p.getItemCooldownManager().isCoolingDown(this))
            return TypedActionResult.fail(stack);
        if (!AbyssGear.consume(p, AbyssItems.ABYSSAL_ESSENCE, mode + 1)) {
            p.sendMessage(Text.translatable("awakening.riftborn.staff_ammo", mode + 1), true);
            return TypedActionResult.fail(stack);
        }
        if (mode == 0) {
            Vec3d look = Vec3d.fromPolar(p.getPitch(), p.getYaw());
            AbyssBoltEntity.fire(p.getServerWorld(), p, p.getEyePos().add(look.multiply(32)), 10, 1.3f);
        }
        if (mode == 1) {
            for (var e : w.getEntitiesByClass(
                     HostileEntity.class, p.getBoundingBox().expand(6), e -> e.isAlive() && p.canSee(e))) {
                e.damage(w.getDamageSources().playerAttack(p), 8);
                Vec3d d = e.getPos().subtract(p.getPos()).normalize();
                e.takeKnockback(1.8, -d.x, -d.z);
            }
            AbyssFx.ring(p.getServerWorld(), p.getPos().add(0, 0.2, 0), 6, 24);
        }
        if (mode == 2) {
            for (var ally : p.getServerWorld().getPlayers(
                     ally -> ally.squaredDistanceTo(p) < 36 && (ally == p || p.isTeammate(ally))))
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0));
            AbyssFx.burst(p.getServerWorld(), p.getPos().add(0, 1, 0), 24, 1);
        }
        p.getItemCooldownManager().set(this, AbyssGear.cooldown(p, new int[] {25, 100, 240}[mode]));
        stack.damage(mode + 1, p, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        p.playSound(net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE, 0.55f, 1.3f);
        return TypedActionResult.success(stack);
    }
    @Override
    public void appendTooltip(ItemStack s, TooltipContext c, List<Text> t, TooltipType tt) {
        super.appendTooltip(s, c, t, tt);
        t.add(Text.translatable("spell.riftborn." + spell(s)).formatted(Formatting.GOLD));
    }
}
