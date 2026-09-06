package dev.riftborn.world;

import dev.riftborn.Riftborn;
import dev.riftborn.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.structure.Structure;

public final class RiftWorldgen {
    public static final RegistryKey<PlacedFeature> RIFT_STONE_ORE = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Riftborn.id("rift_stone_ore"));
    public static final TagKey<Structure> RIFT_SIGNALS = TagKey.of(RegistryKeys.STRUCTURE, Riftborn.id("rift_signals"));
    public static final TagKey<Structure> GUARDIAN_SHRINES = TagKey.of(RegistryKeys.STRUCTURE, Riftborn.id("guardian_shrines"));

    public static void initialize() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.UNDERGROUND_ORES, RIFT_STONE_ORE);
        if (Riftborn.CONFIG.overworldStalkerWeight > 0) {
            BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER,
                    ModEntities.RIFT_STALKER, Riftborn.CONFIG.overworldStalkerWeight, 1, 2);
        }
    }
    private RiftWorldgen() { }
}
