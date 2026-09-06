package dev.riftborn;

import dev.riftborn.world.RiftSurfacePlacement;
import net.minecraft.util.math.BlockBox;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiftSurfacePlacementTest {
    private static final BlockBox FOOTPRINT = new BlockBox(0, 127, 0, 16, 136, 16);
    @Test void voidHeightZeroNeverBecomesAMinusOneFoundation() {
        assertTrue(RiftSurfacePlacement.find(FOOTPRINT, (x,z) -> 0, 32, 240, 48).isEmpty());
    }
    @Test void foundationFollowsTerrainRatherThanAFixedHeight() {
        for (int height : new int[]{52, 67, 91}) {
            var site = RiftSurfacePlacement.find(FOOTPRINT, (x,z) -> height, 32, 240, 48).orElseThrow();
            assertEquals(height - 1, site.floorY()); assertEquals(0, site.offsetX()); assertEquals(0, site.offsetZ());
        }
    }
    @Test void nearbyIslandRescuesPlacementWhoseOriginalCenterIsOverVoid() {
        var site = RiftSurfacePlacement.find(FOOTPRINT, (x,z) -> x >= 20 && x <= 60 ? 72 : 0, 32, 240, 48).orElseThrow();
        assertTrue(site.offsetX() > 0); assertEquals(71, site.floorY());
    }
    @Test void highestFootprintTerrainPreventsBurial() {
        var site = RiftSurfacePlacement.find(FOOTPRINT, (x,z) -> 65 + Math.floorMod(x, 5), 32, 240, 48).orElseThrow();
        assertEquals(68, site.floorY());
    }
    @Test void aSingleNeedleInVoidIsNotAnIslandFoundation() {
        assertTrue(RiftSurfacePlacement.find(FOOTPRINT, (x,z) -> x == 8 && z == 8 ? 72 : 0, 32, 240, 48).isEmpty());
    }
}
