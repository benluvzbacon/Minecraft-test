package dev.riftborn.awakening.item;
import net.minecraft.item.*;import net.minecraft.item.tooltip.TooltipType;import net.minecraft.text.Text;import net.minecraft.util.Formatting;import java.util.List;
public class ArtifactItem extends AwakeningItem {
    public ArtifactItem(Settings s){super(s.maxCount(1).fireproof());}
    @Override public void appendTooltip(ItemStack s,TooltipContext c,List<Text> text,TooltipType t){super.appendTooltip(s,c,text,t);text.add(Text.translatable("awakening.riftborn.artifact_slot").formatted(Formatting.GRAY));}
}
