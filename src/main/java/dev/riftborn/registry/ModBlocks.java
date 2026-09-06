package dev.riftborn.registry;

import dev.riftborn.Riftborn;
import dev.riftborn.block.GuardianAltarBlock;
import dev.riftborn.block.RiftAnchorBlock;
import dev.riftborn.block.VoidBloomBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
    public static final Block RIFT_STONE = register("rift_stone", new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.PURPLE).strength(4.0f, 12.0f).requiresTool().sounds(BlockSoundGroup.DEEPSLATE)));
    public static final Block RIFT_ANCHOR = register("rift_anchor", new RiftAnchorBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.PURPLE).strength(-1.0f, 3600000.0f).luminance(state -> 10)
            .sounds(BlockSoundGroup.AMETHYST_BLOCK)));
    public static final Block GUARDIAN_ALTAR = register("guardian_altar", new GuardianAltarBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.BLACK).strength(-1.0f, 3600000.0f).luminance(state -> 7)
            .sounds(BlockSoundGroup.AMETHYST_BLOCK)));
    public static final Block VOID_BLOOM = register("void_bloom", new VoidBloomBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.MAGENTA).noCollision().nonOpaque().breakInstantly().luminance(state -> 7)
            .sounds(BlockSoundGroup.AMETHYST_CLUSTER)));

    private static Block register(String name, Block block) {
        Registry.register(Registries.BLOCK, Riftborn.id(name), block);
        Registry.register(Registries.ITEM, Riftborn.id(name), new BlockItem(block, new Item.Settings()));
        return block;
    }

    private ModBlocks() { }
    public static void initialize() { }
}
