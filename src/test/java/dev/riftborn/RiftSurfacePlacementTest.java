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
    @Test void circularShrinesIgnoreAirOutsideTheirActualFoundation() {
        var box = new BlockBox(0, 127, 0, 32, 140, 32);
        var site = RiftSurfacePlacement.find(box, (x,z) -> {
            int dx = x - 16, dz = z - 16;
            return dx * dx + dz * dz <= 256 ? 72 : 110;
        }, 32, 240, 0, true).orElseThrow();
        assertEquals(71, site.floorY());
    }
    @Test void aFlatLargeShrineDoesNotResampleEveryTerrainColumn() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var box = new BlockBox(0, 127, 0, 32, 140, 32);
        assertTrue(RiftSurfacePlacement.find(box, (x,z) -> { calls.incrementAndGet(); return 72; }, 32, 240, 64, true).isPresent());
        assertTrue(calls.get() < 100, "Avoid a per-block height-query explosion: " + calls);
    }
    @Test void aSingleNeedleInVoidIsNotAnIslandFoundation() {
        assertTrue(RiftSurfacePlacement.find(FOOTPRINT, (x,z) -> x == 8 && z == 8 ? 72 : 0, 32, 240, 48).isEmpty());
    }
}
