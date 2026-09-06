package dev.riftborn.registry;

import dev.riftborn.Riftborn;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.poi.PointOfInterestType;

/** Vanilla POI storage tracks real anchors, including the arrival platform and player-placed ones. */
public final class ModPointsOfInterest {
    public static final RegistryKey<PointOfInterestType> ANCHOR = RegistryKey.of(
            RegistryKeys.POINT_OF_INTEREST_TYPE, Riftborn.id("rift_anchor"));

    public static void initialize() {
        // Zero tickets: this is a portal marker, never a villager workstation.
        PointOfInterestHelper.register(ANCHOR.getValue(), 0, 1, ModBlocks.RIFT_ANCHOR);
    }
    private ModPointsOfInterest() { }
}
