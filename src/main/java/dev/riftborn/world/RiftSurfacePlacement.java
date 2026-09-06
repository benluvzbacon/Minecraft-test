package dev.riftborn.world;

import net.minecraft.util.math.BlockBox;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntBinaryOperator;

/** Terrain-only queries: safe during STRUCTURE_STARTS, with no chunk-loading recursion. */
public final class RiftSurfacePlacement {
    public record Site(int offsetX, int floorY, int offsetZ) { }

    public static Optional<Site> find(BlockBox box, IntBinaryOperator surfaceHeight, int minSurface, int maxSurface, int radius) {
        Map<Long, Integer> cache = new HashMap<>();
        IntBinaryOperator height = (x, z) -> cache.computeIfAbsent(((long) x << 32) ^ (z & 0xffffffffL),
                key -> surfaceHeight.applyAsInt(x, z));
        for (int ring = 0; ring <= radius; ring += 8) {
            for (int dx = -ring; dx <= ring; dx += 8) for (int dz = -ring; dz <= ring; dz += 8) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                int center = height.applyAsInt(box.getCenter().getX() + dx, box.getCenter().getZ() + dz);
                if (center < minSurface || center > maxSurface) continue;
                // Reject obvious void/cliff candidates cheaply before checking the complete footprint.
                int supports = 0, low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
                for (int x : new int[]{box.getMinX() + 2, box.getCenter().getX(), box.getMaxX() - 2}) {
                    for (int z : new int[]{box.getMinZ() + 2, box.getCenter().getZ(), box.getMaxZ() - 2}) {
                        int y = height.applyAsInt(x + dx, z + dz);
                        if (y >= minSurface && y <= maxSurface) { supports++; low = Math.min(low, y); high = Math.max(high, y); }
                    }
                }
                if (supports < 5 || high - low > 12) continue;
                int count = 0, supported = 0;
                low = Integer.MAX_VALUE; high = Integer.MIN_VALUE;
                for (int x = box.getMinX(); x <= box.getMaxX(); x++) for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                    count++;
                    int y = height.applyAsInt(x + dx, z + dz);
                    if (y >= minSurface && y <= maxSurface) { supported++; low = Math.min(low, y); high = Math.max(high, y); }
                    else if (y > maxSurface) { high = y; }
                }
                // Highest terrain in the footprint sets the foundation, never the bottom of a void column.
                // Beard-thin terrain adaptation blends the modest remaining height variation underneath.
                if (supported * 5 >= count * 3 && high <= maxSurface && high - low <= 12 && high - center <= 10)
                    return Optional.of(new Site(dx, high - 1, dz));
            }
        }
        return Optional.empty(); // A placement cell with no nearby island is not a valid building site.
    }
    private RiftSurfacePlacement() { }
}
