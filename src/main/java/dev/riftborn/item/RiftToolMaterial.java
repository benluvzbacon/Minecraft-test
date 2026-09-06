package dev.riftborn.item;

import dev.riftborn.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

public enum RiftToolMaterial implements ToolMaterial {
    INSTANCE;

    @Override public int getDurability() { return 2031; }
    @Override public float getMiningSpeedMultiplier() { return 9.0f; }
    @Override public float getAttackDamage() { return 6.0f; }
    @Override public TagKey<Block> getInverseTag() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
    @Override public int getEnchantability() { return 18; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.ofItems(ModItems.VOID_FRAGMENT); }
}
