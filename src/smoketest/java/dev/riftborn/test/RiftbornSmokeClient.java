package dev.riftborn.test;

import dev.riftborn.Riftborn;
import dev.riftborn.dimension.RiftDimensions;
import dev.riftborn.registry.ModEntities;
import dev.riftborn.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

/** Runs only from runClientSmoke; never packaged into Riftborn's production jar. */
public final class RiftbornSmokeClient implements ClientModInitializer {
    private int ticks;
    private int stage;
    private int stageTick;
    private int enteredTick;
    private Vec3d beforeBlink;
    private Vec3d afterBlink;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("RIFTBORN_SMOKE_FAILURE: " + message);
    }

    private static void useBlock(MinecraftClient client, BlockPos pos) {
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
    }

    private void next(int stage) { this.stage = stage; stageTick = ticks; }

    private void tick(MinecraftClient client) {
        if (++ticks > 2400) throw new IllegalStateException("RIFTBORN_SMOKE_FAILURE: client test timed out at stage " + stage);
        if (client.world == null || client.player == null || client.interactionManager == null) return;
        boolean inRift = client.world.getRegistryKey().equals(RiftDimensions.WORLD);
        switch (stage) {
            case 0 -> {
                if (client.world.getRegistryKey().equals(World.OVERWORLD)
                        && client.player.getMainHandStack().isOf(ModItems.RIFT_CORE)
                        && client.player.squaredDistanceTo(0.5, 100, 2.5) < 4) {
                    useBlock(client, new BlockPos(0, 100, 0));
                    next(1);
                }
            }
            case 1 -> {
                if (inRift && ticks - stageTick > 15) {
                    require(client.player.getMainHandStack().getCount() == 2, "Entering consumes exactly one Rift Core");
                    enteredTick = ticks;
                    Riftborn.LOGGER.info("RIFTBORN_TRAVEL_ENTRY_OK");
                    next(2);
                }
            }
            case 2 -> {
                if (inRift && client.player.getMainHandStack().isOf(ModItems.RIFTBLADE)
                        && client.player.squaredDistanceTo(16.5, 141, 40.5) < 1 && ticks - stageTick > 25) {
                    client.player.setYaw(-90);
                    client.player.setPitch(0);
                    beforeBlink = client.player.getPos();
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    next(3);
                }
            }
            case 3 -> {
                if (ticks - stageTick > 20) {
                    require(client.player.getX() > beforeBlink.x + 7.5, "Blink must move the networked player forward");
                    require(client.player.getItemCooldownManager().isCoolingDown(ModItems.RIFTBLADE), "Server must synchronize the cooldown");
                    require(client.player.getMainHandStack().getDamage() == 2, "Server must synchronize durability");
                    afterBlink = client.player.getPos();
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    next(4);
                }
            }
            case 4 -> {
                if (ticks - stageTick > 15) {
                    require(client.player.getPos().squaredDistanceTo(afterBlink) < 0.1, "Cooldown must block repeated network uses");
                    client.player.setYaw(160);
                    client.player.setPitch(8);
                    Riftborn.LOGGER.info("RIFTBORN_MULTIPLAYER_BLINK_OK");
                    next(5);
                }
            }
            case 5 -> {
                if (ticks - stageTick > 100) {
                    Set<EntityType<?>> visibleTypes = new HashSet<>();
                    for (var entity : client.world.getEntities()) {
                        visibleTypes.add(entity.getType());
                        if (entity.getType().toString().contains("riftborn")) {
                            require(client.getEntityRenderDispatcher().getRenderer(entity) != null, "Missing entity renderer");
                        }
                    }
                    require(visibleTypes.containsAll(Set.of(ModEntities.RIFT_STALKER, ModEntities.VOID_BRUTE,
                            ModEntities.RIFT_WISP, ModEntities.RIFT_GUARDIAN)), "All four mob types must reach the client");
                    ScreenshotRecorder.saveScreenshot(client.runDirectory, "riftborn-smoke.png", client.getFramebuffer(),
                            message -> Riftborn.LOGGER.info("Smoke screenshot: {}", message.getString()));
                    Riftborn.LOGGER.info("RIFTBORN_SCENE_RENDER_OK");
                    next(6);
                }
            }
            case 6 -> {
                if (inRift && client.player.squaredDistanceTo(16.5, 141, 6.5) < 2
                        && ticks - enteredTick > 330 && ticks % 30 == 0) {
                    useBlock(client, new BlockPos(16, 141, 4));
                } else if (client.world.getRegistryKey().equals(World.OVERWORLD)) {
                    require(client.player.squaredDistanceTo(0.5, 100, 0.5) < 100, "Return anchor must lead back to the player's origin");
                    Riftborn.LOGGER.info("RIFTBORN_TRAVEL_RETURN_OK");
                    Riftborn.LOGGER.info("RIFTBORN_MULTIPLAYER_SMOKE_OK");
                    next(7);
                    client.scheduleStop();
                }
            }
            default -> { }
        }
    }
}
