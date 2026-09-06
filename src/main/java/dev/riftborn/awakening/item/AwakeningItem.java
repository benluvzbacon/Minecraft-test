package dev.riftborn.awakening.item;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;
public class AwakeningItem extends Item {
    public AwakeningItem(Settings s) {
        super(s);
    }
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.DARK_AQUA));
    }
}
