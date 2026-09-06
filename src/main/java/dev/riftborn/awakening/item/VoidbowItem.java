package dev.riftborn.awakening.item;
import dev.riftborn.awakening.*;
import dev.riftborn.awakening.entity.AbyssBoltEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.enchantment.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.List;
public final class VoidbowItem extends BowItem {
    public VoidbowItem() {
        super(new Settings().maxDamage(1800).fireproof().rarity(Rarity.EPIC));
    }
    @Override
    public TypedActionResult<ItemStack> use(World w, PlayerEntity p, Hand h) {
        if (p.getItemCooldownManager().isCoolingDown(this))
            return TypedActionResult.fail(p.getStackInHand(h));
        p.setCurrentHand(h);
        return TypedActionResult.consume(p.getStackInHand(h));
    }
    @Override
    public void onStoppedUsing(ItemStack s, World w, LivingEntity e, int remaining) {
        if (!(e instanceof ServerPlayerEntity p) || p.getItemCooldownManager().isCoolingDown(this))
            return;
        float charge = BowItem.getPullProgress(getMaxUseTime(s, e) - remaining);
        if (charge < 0.15f)
            return;
        if (!AbyssGear.consume(p, AbyssItems.ABYSSAL_SHARD, 1)) {
            p.sendMessage(Text.translatable("awakening.riftborn.bow_ammo"), true);
            return;
        }
        var registry = w.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        int power = EnchantmentHelper.getLevel(registry.entryOf(Enchantments.POWER), s),
            punch = EnchantmentHelper.getLevel(registry.entryOf(Enchantments.PUNCH), s),
            flame = EnchantmentHelper.getLevel(registry.entryOf(Enchantments.FLAME), s);
        var bolt = AbyssBoltEntity.fire(p.getServerWorld(), p,
            p.getEyePos().add(Vec3d.fromPolar(p.getPitch(), p.getYaw()).multiply(40)), 4 + 10 * charge + power * 1.5f,
            1.4f + charge * 0.8f);
        bolt.setImpact(0.4f + punch * 0.3f, flame > 0);
        p.getItemCooldownManager().set(this, AbyssGear.cooldown(p, 16));
        s.damage(1, p, p.getActiveHand() == Hand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
        p.playSound(net.minecraft.sound.SoundEvents.ENTITY_ARROW_SHOOT, 0.8f, 0.75f);
    }
    @Override
    public void appendTooltip(ItemStack s, TooltipContext c, List<Text> t, TooltipType tt) {
        t.add(Text.translatable("item.riftborn.voidbow.tooltip").formatted(Formatting.AQUA));
    }
}
