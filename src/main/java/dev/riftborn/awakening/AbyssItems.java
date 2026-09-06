package dev.riftborn.awakening;
import dev.riftborn.Riftborn;import dev.riftborn.awakening.item.*;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;import net.minecraft.item.*;import net.minecraft.registry.*;import net.minecraft.text.Text;import net.minecraft.util.Rarity;
public final class AbyssItems {
    public static final Item ABYSSAL_SHARD=material("abyssal_shard",Rarity.UNCOMMON), ABYSSAL_CRYSTAL=material("abyssal_crystal",Rarity.RARE), ABYSSAL_ESSENCE=material("abyssal_essence",Rarity.UNCOMMON), ABYSSAL_CORE=material("abyssal_core",Rarity.RARE);
    public static final Item RESONANT_SIGIL=material("resonant_sigil",Rarity.RARE), STORMGLASS=material("stormglass",Rarity.RARE), TITAN_CORE=material("titan_core",Rarity.EPIC), REALITY_SPINDLE=material("reality_spindle",Rarity.EPIC), SOVEREIGN_SIGIL=material("sovereign_sigil",Rarity.EPIC), SOVEREIGN_HEART=material("sovereign_heart",Rarity.EPIC);
    public static final Item ABYSSAL_SCHEMATIC=material("abyssal_schematic",Rarity.RARE), COLLAPSE_CATALYST=material("collapse_catalyst",Rarity.EPIC);
    public static final Item ABYSSAL_KEY=register("abyssal_key",new LocatorItem(new Item.Settings().rarity(Rarity.EPIC),true));
    public static final Item HEART_OF_THE_RIFT=register("heart_of_the_rift",new ArtifactItem(new Item.Settings().rarity(Rarity.EPIC)));
    public static final Item ABYSSAL_EYE=register("abyssal_eye",new LocatorItem(new Item.Settings().rarity(Rarity.RARE),false));
    public static final Item VOID_CORE=register("void_core",new ArtifactItem(new Item.Settings().rarity(Rarity.RARE)));
    public static final Item WARDING_ANCHOR=register("warding_anchor",new ArtifactItem(new Item.Settings().rarity(Rarity.RARE)));
    public static final Item ABYSSAL_HELMET=register("abyssal_helmet",new AbyssArmorItem(ArmorItem.Type.HELMET)), ABYSSAL_CHESTPLATE=register("abyssal_chestplate",new AbyssArmorItem(ArmorItem.Type.CHESTPLATE)), ABYSSAL_LEGGINGS=register("abyssal_leggings",new AbyssArmorItem(ArmorItem.Type.LEGGINGS)), ABYSSAL_BOOTS=register("abyssal_boots",new AbyssArmorItem(ArmorItem.Type.BOOTS));
    public static final Item GREATBLADE=register("abyssal_greatblade",new AbyssalGreatbladeItem());
    public static final Item VOIDBOW=register("voidbow",new VoidbowItem());
    public static final Item RIFT_STAFF=register("rift_staff",new RiftStaffItem());
    public static final Item STALKER_EGG=egg("abyss_stalker",AbyssEntities.STALKER,0x112532,0x45e8df), REAVER_EGG=egg("void_reaver",AbyssEntities.REAVER,0x161e35,0xdcb56e), BRUTE_EGG=egg("abyssal_brute",AbyssEntities.BRUTE,0x1c2734,0x449eaa), ECHO_EGG=egg("rift_echo",AbyssEntities.ECHO,0x153349,0x9af6ee), WARDEN_EGG=egg("abyssal_warden",AbyssEntities.WARDEN,0x152337,0xddb768);
    public static final Item COLOSSUS_EGG=egg("abyssal_colossus",AbyssEntities.COLOSSUS,0x13232a,0x7beded), ARCHITECT_EGG=egg("rift_architect",AbyssEntities.ARCHITECT,0x211c37,0xe6ca96), SOVEREIGN_EGG=egg("abyss_sovereign",AbyssEntities.SOVEREIGN,0x09151e,0xeccd7b), HERALD_EGG=egg("collapse_herald",AbyssEntities.HERALD,0x322443,0xff9666);
    public static final ItemGroup GROUP=Registry.register(Registries.ITEM_GROUP,Riftborn.id("awakening"),FabricItemGroup.builder().displayName(Text.translatable("itemGroup.riftborn.awakening")).icon(()->new ItemStack(ABYSSAL_KEY)).entries((ctx,e)->{
        for(Item i:new Item[]{ABYSSAL_SHARD,ABYSSAL_CRYSTAL,ABYSSAL_ESSENCE,ABYSSAL_CORE,RESONANT_SIGIL,ABYSSAL_KEY,ABYSSAL_SCHEMATIC,STORMGLASS,TITAN_CORE,REALITY_SPINDLE,COLLAPSE_CATALYST,SOVEREIGN_SIGIL,SOVEREIGN_HEART,ABYSSAL_HELMET,ABYSSAL_CHESTPLATE,ABYSSAL_LEGGINGS,ABYSSAL_BOOTS,GREATBLADE,VOIDBOW,RIFT_STAFF,HEART_OF_THE_RIFT,ABYSSAL_EYE,VOID_CORE,WARDING_ANCHOR})e.add(i);
        for(var b:new net.minecraft.block.Block[]{AbyssBlocks.STONE,AbyssBlocks.LUMINOUS_SHALE,AbyssBlocks.ORE,AbyssBlocks.CRYSTAL,AbyssBlocks.GATE,AbyssBlocks.RESONATOR,AbyssBlocks.RESONANCE_LOCK,AbyssBlocks.STELE,AbyssBlocks.COLOSSUS_ALTAR,AbyssBlocks.ARCHITECT_ALTAR,AbyssBlocks.SOVEREIGN_ALTAR,AbyssBlocks.ECHO_FERN,AbyssBlocks.ASHEN_REEDS,AbyssBlocks.BRAMBLE,AbyssBlocks.TROPHY})e.add(b);
        for(Item i:new Item[]{STALKER_EGG,REAVER_EGG,BRUTE_EGG,ECHO_EGG,WARDEN_EGG,COLOSSUS_EGG,ARCHITECT_EGG,SOVEREIGN_EGG,HERALD_EGG})e.add(i);
    }).build());
    private static Item material(String id,Rarity rarity){return register(id,new AwakeningItem(new Item.Settings().fireproof().rarity(rarity)));}
    private static Item register(String id,Item i){return Registry.register(Registries.ITEM,Riftborn.id(id),i);}
    private static Item egg(String id,net.minecraft.entity.EntityType<? extends net.minecraft.entity.mob.MobEntity> type,int a,int b){return register(id+"_spawn_egg",new SpawnEggItem(type,a,b,new Item.Settings()));}
    public static void initialize(){} private AbyssItems(){}
}
