package dev.riftborn.awakening.item;
import dev.riftborn.awakening.AbyssGear;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import java.util.List;
public final class AbyssArmorItem extends ArmorItem {
    public AbyssArmorItem(Type type) {
        super(
            AbyssGear.MATERIAL, type, new Settings().maxDamage(type.getMaxDamage(58)).fireproof().rarity(Rarity.EPIC));
    }
    @Override
    public void appendTooltip(ItemStack s, TooltipContext c, List<Text> lines, TooltipType type) {
        lines.add(Text.translatable("awakening.riftborn.ascension").formatted(Formatting.AQUA));
        lines.add(Text.translatable("awakening.riftborn.dash_hint").formatted(Formatting.GRAY));
    }
}
