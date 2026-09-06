package dev.riftborn.test;

import dev.riftborn.Riftborn;
import dev.riftborn.dimension.RiftDimensions;
import dev.riftborn.effect.RiftFlight;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.world.GameMode;
import dev.riftborn.registry.ModEntities;
import dev.riftborn.registry.ModBlocks;
import dev.riftborn.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
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
    private boolean handedOff;
    private int stage;
    private int stageTick;
    private int enteredTick;
    private boolean connecting;
    private Vec3d beforeBlink;
    private Vec3d afterBlink;
    private Vec3d beforeFlight;
    private Vec3d stoppedPosition;
    private int removedPiece;
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] ARMOR_NAMES = {"helmet", "chestplate", "leggings", "boots"};

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

    private static boolean fullArmor(MinecraftClient client) {
        return client.player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.RIFT_HELMET)
                && client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.RIFT_CHESTPLATE)
                && client.player.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.RIFT_LEGGINGS)
                && client.player.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.RIFT_BOOTS);
    }
    private static void fly(MinecraftClient client) {
        // Normal Creative-style takeoff starts with a jump. Otherwise vanilla correctly
        // turns flying back off when an on-ground player simply sends a flying flag.
        if (client.player.isOnGround()) client.player.jump();
        client.player.getAbilities().flying = true; client.player.sendAbilitiesUpdate();
    }
    private static void releaseKeys(MinecraftClient client) {
        client.options.forwardKey.setPressed(false); client.options.backKey.setPressed(false);
        client.options.jumpKey.setPressed(false); client.options.sneakKey.setPressed(false);
    }

    private void next(int stage) { this.stage = stage; stageTick = ticks; }

    private void tick(MinecraftClient client) {
        if (handedOff) return;
        if (++ticks > 7200) throw new IllegalStateException("RIFTBORN_SMOKE_FAILURE: client test timed out at stage " + stage);
        if (ticks % 200 == 0) Riftborn.LOGGER.info("Smoke stage {}, screen {}, world {}, player {}", stage,
                client.currentScreen == null ? "none" : client.currentScreen.getClass().getSimpleName(),
                client.world == null ? "none" : client.world.getRegistryKey().getValue(),
                client.player == null ? "none" : client.player.getPos());
        if (client.world == null && !connecting && client.getOverlay() == null && ticks > 30) {
            connecting = true;
            require(client.getSession().getUsername().equals("RiftbornTester"), "Smoke client username must be deterministic");
            ConnectScreen.connect(new TitleScreen(), client, ServerAddress.parse("127.0.0.1:25565"),
                    new ServerInfo("Riftborn isolated smoke", "127.0.0.1:25565", ServerInfo.ServerType.OTHER), false, null);
        }
        if (client.world == null || client.player == null || client.interactionManager == null) return;
        boolean inRift = client.world.getRegistryKey().equals(RiftDimensions.WORLD);
        switch (stage) {
            case 0 -> {
                if (client.world.getRegistryKey().equals(World.OVERWORLD)
                        && client.currentScreen == null && client.player.isOnGround()
                        && client.world.getBlockState(new BlockPos(0, 100, 0)).isOf(ModBlocks.RIFT_ANCHOR)
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
                if (inRift && client.currentScreen == null && client.player.isOnGround()
                        && client.player.getVelocity().lengthSquared() < 0.02
                        && client.player.getMainHandStack().isOf(ModItems.RIFTBLADE)
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
                    require(client.player.getPos().squaredDistanceTo(afterBlink) < 0.1, "Cooldown must block repeated network uses: before=" + afterBlink + ", after=" + client.player.getPos());
                    client.player.setYaw(160);
                    client.player.setPitch(8);
                    Riftborn.LOGGER.info("RIFTBORN_MULTIPLAYER_BLINK_OK");
                    next(5);
                }
            }
            case 5 -> {
                // Keep the captured scene free of first-login chat/tutorial/advancement overlays.
                client.getToastManager().clear();
                client.inGameHud.getChatHud().clear(false);
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
                    int floatingColumns = 0;
                    for (int x = -32; x <= 64; x += 8) {
                        for (int z = -32; z <= 64; z += 8) {
                            int solid = 0;
                            for (int y = 20; y <= 112; y += 4) {
                                if (!client.world.getBlockState(new BlockPos(x, y, z)).isAir()) solid++;
                            }
                            // The display structure is at Y=140; this samples real terrain below it.
                            if (solid > 0 && client.world.getBlockState(new BlockPos(x, 4, z)).isAir()
                                    && client.world.getBlockState(new BlockPos(x, 124, z)).isAir()) floatingColumns++;
                        }
                    }
                    require(floatingColumns > 8, "Natural floating-island terrain must reach the client: " + floatingColumns);
                    Riftborn.LOGGER.info("RIFTBORN_FLOATING_TERRAIN_OK: {} sampled floating columns", floatingColumns);
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
                    next(7);
                }
            }
            case 7 -> {
                if (fullArmor(client) && client.player.getAbilities().allowFlying && ticks - stageTick > 20) {
                    require(Math.abs(client.player.getAbilities().getFlySpeed() - RiftFlight.FLIGHT_SPEED) < 0.00001, "Fast flight must synchronize from server");
                    require(client.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL, "Flight must not change Survival mode");
                    fly(client); client.player.setYaw(-90); client.player.setPitch(0);
                    beforeFlight = client.player.getPos();
                    client.options.forwardKey.setPressed(true); client.options.jumpKey.setPressed(true);
                    next(8);
                }
            }
            case 8 -> {
                if (ticks - stageTick > 20) {
                    releaseKeys(client);
                    Vec3d travel = client.player.getPos().subtract(beforeFlight);
                    require(travel.x > 8 && travel.y > 3, "Fast flight must move horizontally and upward with normal input: " + travel);
                    Riftborn.LOGGER.info("RIFTBORN_FLIGHT_MOVEMENT_OK {}", travel);
                    next(9);
                }
            }
            case 9 -> {
                // Native Creative flight coasts with air drag; allow that normal braking period.
                if (ticks - stageTick == 60) stoppedPosition = client.player.getPos();
                if (ticks - stageTick > 80) {
                    require(client.player.getPos().squaredDistanceTo(stoppedPosition) < 0.05,
                            "Flight must settle after normal air-drag braking: position=" + client.player.getPos() + ", velocity=" + client.player.getVelocity());
                    beforeFlight = client.player.getPos();
                    client.options.backKey.setPressed(true); client.options.sneakKey.setPressed(true);
                    next(10);
                }
            }
            case 10 -> {
                if (ticks - stageTick > 10) {
                    releaseKeys(client);
                    Vec3d travel = client.player.getPos().subtract(beforeFlight);
                    require(travel.x < -3 && travel.y < -1, "Flight must reverse direction and descend: " + travel);
                    Riftborn.LOGGER.info("RIFTBORN_FLIGHT_CONTROLS_OK");
                    client.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                    client.options.hudHidden = true;
                    next(11);
                }
            }
            case 11 -> {
                client.getToastManager().clear(); client.inGameHud.getChatHud().clear(false);
                if (ticks - stageTick > 60) {
                    for (int layer : new int[]{1, 2}) require(client.getResourceManager().getResource(
                            Riftborn.id("textures/models/armor/rift_layer_" + layer + ".png")).isPresent(), "Armor model texture layer must load");
                    ScreenshotRecorder.saveScreenshot(client.runDirectory, "riftborn-armor.png", client.getFramebuffer(),
                            message -> Riftborn.LOGGER.info("Armor screenshot: {}", message.getString()));
                    client.options.setPerspective(Perspective.FIRST_PERSON); client.options.hudHidden = false;
                    Riftborn.LOGGER.info("RIFTBORN_ARMOR_RENDER_OK");
                    next(12);
                }
            }
            case 12 -> {
                if (client.player.getEquippedStack(ARMOR_SLOTS[removedPiece]).isEmpty()
                        && !client.player.getAbilities().allowFlying && !client.player.getAbilities().flying) {
                    // Deliberately send a forged vanilla abilities request after removing armor.
                    client.player.getAbilities().flying = true; client.player.sendAbilitiesUpdate();
                    next(13);
                }
            }
            case 13 -> {
                if (ticks - stageTick > 20) {
                    require(!client.player.getAbilities().allowFlying && !client.player.getAbilities().flying,
                            "Server must reject flight without " + ARMOR_NAMES[removedPiece]);
                    require(Math.abs(client.player.getAbilities().getFlySpeed() - 0.05f) < 0.00001, "Bonus speed must be revoked");
                    Riftborn.LOGGER.info("RIFTBORN_ARMOR_PIECE_REMOVED_OK {}", ARMOR_NAMES[removedPiece]);
                    if (++removedPiece < 4) next(12);
                    else { Riftborn.LOGGER.info("RIFTBORN_ARMOR_REMOVAL_OK"); next(14); }
                }
            }
            case 14 -> {
                if (fullArmor(client) && client.player.getAbilities().allowFlying
                        && client.interactionManager.getCurrentGameMode() == GameMode.ADVENTURE) {
                    fly(client); next(15);
                }
            }
            case 15 -> {
                if (ticks - stageTick > 20) {
                    require(client.player.getAbilities().flying && !client.player.getAbilities().allowModifyWorld, "Adventure flight keeps Adventure restrictions");
                    Riftborn.LOGGER.info("RIFTBORN_ADVENTURE_FLIGHT_OK"); next(16);
                }
            }
            case 16 -> {
                if (client.interactionManager.getCurrentGameMode() == GameMode.CREATIVE
                        && client.player.getAbilities().allowFlying && Math.abs(client.player.getAbilities().getFlySpeed() - 0.05f) < 0.00001) {
                    Riftborn.LOGGER.info("RIFTBORN_CREATIVE_FLIGHT_OK"); next(17);
                }
            }
            case 17 -> {
                if (client.player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                    require(client.player.getAbilities().allowFlying && Math.abs(client.player.getAbilities().getFlySpeed() - 0.05f) < 0.00001,
                            "Creative flight must survive armor removal");
                    Riftborn.LOGGER.info("RIFTBORN_CREATIVE_ARMOR_REMOVAL_OK"); next(18);
                }
            }
            case 18 -> {
                if (client.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL && !client.player.getAbilities().allowFlying) {
                    require(!client.player.getAbilities().flying, "Creative to unarmored Survival must not retain flight");
                    Riftborn.LOGGER.info("RIFTBORN_SURVIVAL_FLIGHT_RESET_OK"); next(19);
                }
            }
            case 19 -> {
                if (inRift && fullArmor(client) && client.player.getAbilities().allowFlying && client.currentScreen == null && ticks - stageTick > 30) {
                    fly(client); client.options.setPerspective(Perspective.THIRD_PERSON_FRONT); client.options.hudHidden = true;
                    next(20);
                }
            }
            case 20 -> {
                client.getToastManager().clear(); client.inGameHud.getChatHud().clear(false);
                if (ticks - stageTick > 60) {
                    require(inRift && client.player.getAbilities().flying, "Full-set flight must work after a dimension change");
                    ScreenshotRecorder.saveScreenshot(client.runDirectory, "riftborn-flight.png", client.getFramebuffer(),
                            message -> Riftborn.LOGGER.info("Flight screenshot: {}", message.getString()));
                    client.options.setPerspective(Perspective.FIRST_PERSON); client.options.hudHidden = false;
                    Riftborn.LOGGER.info("RIFTBORN_DIMENSION_FLIGHT_OK"); next(21);
                }
            }
            case 21 -> {
                if (!inRift && client.currentScreen == null && ticks - stageTick > 30) {
                    require(fullArmor(client) && client.player.getAbilities().allowFlying && client.player.getAbilities().flying,
                            "Returning dimensions must keep a valid armor flight grant");
                    Riftborn.LOGGER.info("RIFTBORN_FLIGHT_RECONNECT_START"); next(22);
                }
            }
            case 22 -> {
                if (ticks - stageTick > 20) {
                    releaseKeys(client); client.disconnect(new TitleScreen()); connecting = false; next(23);
                }
            }
            case 23 -> {
                if (client.currentScreen == null && ticks - stageTick > 40) {
                    require(fullArmor(client) && client.player.getAbilities().allowFlying && client.player.getAbilities().flying,
                            "Reconnect must revalidate equipped armor and safely resume a valid flight");
                    require(Math.abs(client.player.getAbilities().getFlySpeed() - RiftFlight.FLIGHT_SPEED) < 0.00001, "Reconnect must restore the validated bonus speed");
                    Riftborn.LOGGER.info("RIFTBORN_FLIGHT_RECONNECT_OK"); next(24);
                }
            }
            case 24 -> {
                if (client.player.getHealth() <= 0 && ticks - stageTick > 20) {
                    require(!client.player.getAbilities().allowFlying, "Death must revoke the flight grant");
                    client.player.requestRespawn(); client.setScreen(null); next(25);
                }
            }
            case 25 -> {
                if (client.player.getHealth() > 0 && client.currentScreen == null && ticks - stageTick > 30) {
                    require(!fullArmor(client) && !client.player.getAbilities().allowFlying && !client.player.getAbilities().flying,
                            "Respawning without armor must not inherit flight");
                    require(Math.abs(client.player.getAbilities().getFlySpeed() - 0.05f) < 0.00001, "Respawn must not inherit flight speed");
                    Riftborn.LOGGER.info("RIFTBORN_RESPAWN_FLIGHT_RESET_OK"); next(26);
                }
            }
            case 26 -> {
                if (ticks - stageTick > 30) {
                    Riftborn.LOGGER.info("RIFTBORN_LEGACY_SMOKE_OK"); next(27); handedOff=true; AwakeningSmoke.begin();
                }
            }
            default -> { }
        }
    }
}
