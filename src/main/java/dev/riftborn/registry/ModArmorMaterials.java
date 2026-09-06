package dev.riftborn.registry;

import dev.riftborn.Riftborn;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import java.util.List;
import java.util.Map;

/** Native 1.21.1 armor material; vanilla's fitted biped armor models use our two texture layers. */
public final class ModArmorMaterials {
    public static final RegistryEntry<ArmorMaterial> RIFT = Registry.registerReference(Registries.ARMOR_MATERIAL,
            Riftborn.id("rift"), new ArmorMaterial(Map.of(
                    ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 8,
                    ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3, ArmorItem.Type.BODY, 0),
                    18, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, () -> Ingredient.ofItems(ModItems.VOID_FRAGMENT),
                    List.of(new ArmorMaterial.Layer(Riftborn.id("rift"))), 3.0f, 0.1f));
    private ModArmorMaterials() { }
}
