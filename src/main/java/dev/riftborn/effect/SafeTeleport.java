package dev.riftborn.effect;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/** Collision checks use the entire entity, not just an eye ray or the destination block. */
public final class SafeTeleport {
    private static final double STEP = 0.2;

    private SafeTeleport() { }

    public static Optional<Vec3d> findBlinkDestination(ServerWorld world, Entity entity, Vec3d look, double range) {
        Vec3d direction = new Vec3d(look.x, 0, look.z);
        if (direction.lengthSquared() < 0.0001 || !Double.isFinite(range)) return Optional.empty();
        direction = direction.normalize();
        Vec3d best = null;
        // A horizontal, swept AABB prevents tunnelling through even panes, fences and corners.
        for (double distance = STEP; distance <= range + 0.001; distance += STEP) {
            Vec3d offset = direction.multiply(distance);
            Vec3d pos = entity.getPos().add(offset);
            Box box = entity.getBoundingBox().offset(offset);
            if (!isClear(world, entity, box) || !world.isChunkLoaded(BlockPos.ofFloored(pos))) break;
            // Do not strand the player over the void. The path may cross gaps, the landing may not.
            if (distance >= 1.0 && hasFloor(world, entity, box)) best = pos;
        }
        return Optional.ofNullable(best);
    }

    public static boolean isClear(ServerWorld world, Entity entity, Box box) {
        return box.minY >= world.getBottomY() && box.maxY < world.getTopY()
                && world.getWorldBorder().contains(box)
                && world.isSpaceEmpty(entity, box)
                && !world.containsFluid(box);
    }

    public static boolean hasFloor(ServerWorld world, Entity entity, Box box) {
        // An inset footprint avoids treating an adjacent wall as a floor.
        Box feet = new Box(box.minX + 0.05, box.minY - 0.25, box.minZ + 0.05,
                box.maxX - 0.05, box.minY, box.maxZ - 0.05);
        return !world.isSpaceEmpty(entity, feet) && !world.containsFluid(feet);
    }

    public static Optional<Vec3d> findLanding(ServerWorld world, Entity entity, BlockPos center, int radius) {
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    for (int dy = 2; dy >= -3; dy--) {
                        Vec3d pos = Vec3d.ofBottomCenter(center.add(dx, dy, dz));
                        world.getChunk(BlockPos.ofFloored(pos));
                        Box box = entity.getBoundingBox().offset(pos.subtract(entity.getPos()));
                        if (isClear(world, entity, box) && hasFloor(world, entity, box)) return Optional.of(pos);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
