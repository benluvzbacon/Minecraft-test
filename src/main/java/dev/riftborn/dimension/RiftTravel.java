package dev.riftborn.dimension;

import dev.riftborn.block.RiftAnchorBlock;
import dev.riftborn.effect.RiftEffects;
import dev.riftborn.effect.SafeTeleport;
import dev.riftborn.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import java.util.Optional;

public final class RiftTravel {
    private RiftTravel() { }

    public static boolean useAnchor(ServerPlayerEntity player, BlockPos anchor) {
        if (player.hasVehicle() || player.hasPassengers() || player.hasPortalCooldown()) {
            player.sendMessage(Text.translatable("message.riftborn.anchor.wait"), true);
            return false;
        }
        MinecraftServer server = player.getServer();
        ServerWorld originWorld = player.getServerWorld();
        boolean returning = originWorld.getRegistryKey().equals(RiftDimensions.WORLD);
        if (!returning && !originWorld.getRegistryKey().equals(World.OVERWORLD)) {
            player.sendMessage(Text.translatable("message.riftborn.anchor.wrong_dimension"), true);
            return false;
        }
        RiftTravelState state = RiftTravelState.get(server);
        ServerWorld destination;
        BlockPos near;
        if (returning) {
            GlobalPos saved = state.getOrigin(player.getUuid());
            destination = saved == null ? server.getOverworld() : server.getWorld(saved.dimension());
            if (destination == null) destination = server.getOverworld();
            near = saved == null ? destination.getSpawnPos() : saved.pos();
        } else {
            destination = server.getWorld(RiftDimensions.WORLD);
            if (destination == null) {
                player.sendMessage(Text.translatable("message.riftborn.anchor.dimension_missing"), false);
                return false;
            }
            near = prepareArrival(destination, state);
        }
        Optional<Vec3d> landing = SafeTeleport.findLanding(destination, player, near, 6);
        if (landing.isEmpty()) {
            // Returning must never trap a player after an Overworld anchor is obstructed.
            if (returning) {
                destination = server.getOverworld();
                BlockPos spawn = destination.getSpawnPos();
                near = destination.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawn);
                landing = SafeTeleport.findLanding(destination, player, near, 8);
            }
            if (landing.isEmpty()) {
                player.sendMessage(Text.translatable("message.riftborn.anchor.obstructed"), false);
                return false;
            }
        }
        Vec3d oldPos = player.getPos();
        Entity result = player.teleportTo(new TeleportTarget(destination, landing.get(), Vec3d.ZERO,
                player.getYaw(), player.getPitch(), TeleportTarget.ADD_PORTAL_CHUNK_TICKET));
        if (!(result instanceof ServerPlayerEntity moved)) return false;
        if (!returning) state.setOrigin(player.getUuid(), GlobalPos.create(originWorld.getRegistryKey(), anchor.up()));
        moved.resetPortalCooldown();
        moved.fallDistance = 0;
        RiftEffects.teleport(originWorld, oldPos);
        RiftEffects.teleport(destination, landing.get());
        moved.sendMessage(Text.translatable(returning ? "message.riftborn.anchor.returned" : "message.riftborn.anchor.arrived"), false);
        return true;
    }

    /** The only intentionally placed platform: a one-time safe portal arrival, never a per-chunk hook. */
    public static BlockPos prepareArrival(ServerWorld world, RiftTravelState state) {
        if (state.getLanding() != null) return state.getLanding();
        BlockPos center = new BlockPos(0, 72, 0);
        search:
        for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = dx * 16, z = dz * 16;
                    int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
                    if (y > 32 && y < 120) {
                        center = new BlockPos(x, y, z);
                        break search;
                    }
                }
            }
        }
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos floor = center.add(x, -1, z);
                world.setBlockState(floor, ModBlocks.RIFT_STONE.getDefaultState(), Block.NOTIFY_ALL);
                for (int y = 1; y <= 4; y++) world.setBlockState(floor.up(y), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                if ((Math.abs(x) == 5 || Math.abs(z) == 5) && (Math.abs(x) > 1 && Math.abs(z) > 1)) {
                    world.setBlockState(floor.up(), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState(), Block.NOTIFY_ALL);
                }
            }
        }
        world.setBlockState(center.add(0, 0, -3), ModBlocks.RIFT_ANCHOR.getDefaultState().with(RiftAnchorBlock.OPEN, true), Block.NOTIFY_ALL);
        world.setBlockState(center.add(-3, 0, -3), ModBlocks.VOID_BLOOM.getDefaultState(), Block.NOTIFY_ALL);
        world.setBlockState(center.add(3, 0, -3), ModBlocks.VOID_BLOOM.getDefaultState(), Block.NOTIFY_ALL);
        state.setLanding(center);
        return center;
    }
}
