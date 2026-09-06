package dev.riftborn.item;

import dev.riftborn.registry.ModArmorMaterials;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import java.util.List;

public final class RiftArmorItem extends ArmorItem {
    public RiftArmorItem(Type type) {
        super(ModArmorMaterials.RIFT, type, new Settings().maxDamage(type.getMaxDamage(45)).fireproof().rarity(Rarity.EPIC));
    }
    @Override public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.riftborn.rift_armor.tooltip").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("item.riftborn.rift_armor.controls").formatted(Formatting.GRAY));
    }
}
