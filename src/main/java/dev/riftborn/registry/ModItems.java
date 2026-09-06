package dev.riftborn.registry;

import dev.riftborn.Riftborn;
import dev.riftborn.item.DescribedItem;
import dev.riftborn.item.RiftCompassItem;
import dev.riftborn.item.RiftbladeItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static final Item RIFT_SHARD = register("rift_shard", new DescribedItem(new Item.Settings()));
    public static final Item VOID_FRAGMENT = register("void_fragment", new DescribedItem(new Item.Settings().rarity(Rarity.UNCOMMON)));
    public static final Item RIFT_DUST = register("rift_dust", new DescribedItem(new Item.Settings()));
    public static final Item RIFT_CORE = register("rift_core", new DescribedItem(new Item.Settings().rarity(Rarity.UNCOMMON)));
    public static final Item RIFT_HEART = register("rift_heart", new DescribedItem(new Item.Settings().fireproof().rarity(Rarity.EPIC)));
    public static final Item RIFT_COMPASS = register("rift_compass", new RiftCompassItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
    public static final Item RIFTBLADE = register("riftblade", new RiftbladeItem(new Item.Settings().fireproof().rarity(Rarity.EPIC)));
    public static final Item RIFT_STALKER_SPAWN_EGG = register("rift_stalker_spawn_egg", new SpawnEggItem(ModEntities.RIFT_STALKER, 0x261631, 0xc77aff, new Item.Settings()));
    public static final Item VOID_BRUTE_SPAWN_EGG = register("void_brute_spawn_egg", new SpawnEggItem(ModEntities.VOID_BRUTE, 0x12101d, 0x864dbe, new Item.Settings()));
    public static final Item RIFT_WISP_SPAWN_EGG = register("rift_wisp_spawn_egg", new SpawnEggItem(ModEntities.RIFT_WISP, 0x532d80, 0x9afff3, new Item.Settings()));
    public static final Item RIFT_GUARDIAN_SPAWN_EGG = register("rift_guardian_spawn_egg", new SpawnEggItem(ModEntities.RIFT_GUARDIAN, 0x171421, 0xebbcff, new Item.Settings()));

    public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, Riftborn.id("riftborn"), FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.riftborn"))
            .icon(() -> new ItemStack(RIFT_CORE))
            .entries((context, entries) -> {
                entries.add(ModBlocks.RIFT_STONE); entries.add(ModBlocks.RIFT_ANCHOR);
                entries.add(ModBlocks.GUARDIAN_ALTAR); entries.add(ModBlocks.VOID_BLOOM);
                entries.add(RIFT_SHARD); entries.add(VOID_FRAGMENT); entries.add(RIFT_DUST);
                entries.add(RIFT_CORE); entries.add(RIFT_COMPASS); entries.add(RIFT_HEART); entries.add(RIFTBLADE);
                entries.add(RIFT_STALKER_SPAWN_EGG); entries.add(VOID_BRUTE_SPAWN_EGG);
                entries.add(RIFT_WISP_SPAWN_EGG); entries.add(RIFT_GUARDIAN_SPAWN_EGG);
            }).build());

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Riftborn.id(name), item);
    }

    private ModItems() { }
    public static void initialize() { }
}
