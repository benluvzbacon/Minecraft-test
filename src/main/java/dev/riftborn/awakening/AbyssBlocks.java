package dev.riftborn.awakening;
import dev.riftborn.Riftborn;
import dev.riftborn.awakening.block.*;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.sound.BlockSoundGroup;
public final class AbyssBlocks {
    public static final Block STONE=register("abyssal_stone",new Block(stone().strength(5,18).requiresTool()));
    public static final Block LUMINOUS_SHALE=register("luminous_shale",new Block(stone().strength(4,15).requiresTool().luminance(s->8)));
    public static final Block ORE=register("abyssal_crystal_ore",new Block(stone().strength(6,20).requiresTool().luminance(s->4)));
    public static final Block CRYSTAL=register("abyssal_crystal_block",new Block(stone().strength(4,18).luminance(s->13).sounds(BlockSoundGroup.AMETHYST_BLOCK)));
    public static final Block GATE=register("abyss_gate",new AbyssGateBlock(ritual().luminance(s->13)));
    public static final Block RESONATOR=register("resonator",new ResonatorBlock(ritual().luminance(s->6+s.get(ResonatorBlock.NOTE)*2)));
    public static final Block RESONANCE_LOCK=register("resonance_lock",new ResonanceLockBlock(ritual().luminance(s->8)));
    public static final Block STELE=register("ancient_stele",new InscriptionBlock(ritual().luminance(s->6)));
    public static final Block COLOSSUS_ALTAR=register("colossus_altar",new AbyssAltarBlock(ritual().luminance(s->10)));
    public static final Block ARCHITECT_ALTAR=register("architect_altar",new AbyssAltarBlock(ritual().luminance(s->10)));
    public static final Block SOVEREIGN_ALTAR=register("sovereign_altar",new AbyssAltarBlock(ritual().luminance(s->12)));
    public static final Block BARRIER=register("phase_barrier",new TemporaryBarrierBlock(stone().strength(0.7f,8).nonOpaque().luminance(s->10).pistonBehavior(PistonBehavior.BLOCK)));
    public static final Block ECHO_FERN=register("echo_fern",new AbyssPlantBlock(plant().luminance(s->5)));
    public static final Block ASHEN_REEDS=register("ashen_reeds",new AbyssPlantBlock(plant().luminance(s->3)));
    public static final Block BRAMBLE=register("abyssal_bramble",new AbyssPlantBlock(plant().luminance(s->6)));
    public static final Block TROPHY=register("sovereign_standard",new Block(stone().strength(4,20).luminance(s->15)));
    private static AbstractBlock.Settings stone(){return AbstractBlock.Settings.create().mapColor(MapColor.CYAN).sounds(BlockSoundGroup.DEEPSLATE);}
    private static AbstractBlock.Settings ritual(){return stone().strength(-1,3600000).pistonBehavior(PistonBehavior.BLOCK).sounds(BlockSoundGroup.AMETHYST_BLOCK);}
    private static AbstractBlock.Settings plant(){return stone().noCollision().nonOpaque().breakInstantly().sounds(BlockSoundGroup.AMETHYST_CLUSTER);}
    private static Block register(String id,Block b){Registry.register(Registries.BLOCK,Riftborn.id(id),b);Registry.register(Registries.ITEM,Riftborn.id(id),new BlockItem(b,new Item.Settings()));return b;}
    public static void initialize(){} private AbyssBlocks(){}
}
