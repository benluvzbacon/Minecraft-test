package dev.riftborn.effect;

import dev.riftborn.registry.ModItems;
import dev.riftborn.registry.ModParticles;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.GameMode;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A server-owned, reversible ability grant, never a game-mode change.
 * Vanilla carries the controls/movement and S2C abilities packet. The full set is
 * rechecked on every server tick and before abilities/movement packets are honored.
 */
public final class RiftFlight {
    public static final float FLIGHT_SPEED = 0.15f; // Vanilla Creative uses 0.05: exactly 3x.
    private static final String RESUME_TAG = "RiftbornFlightResume";
    // Entity identity rather than UUID: a respawn/new connection must not inherit a stale grant.
    // Access is confined to the server thread. Weak keys also avoid retaining disconnected players.
    private static final Map<ServerPlayerEntity, Grant> GRANTS = new WeakHashMap<>();

    private static final class Grant {
        final boolean previousMayFly;
        final float previousSpeed;
        boolean wasFlying;
        int lastSoundTick = -100;
        Grant(ServerPlayerEntity player) {
            previousMayFly = player.getAbilities().allowFlying;
            previousSpeed = player.getAbilities().getFlySpeed();
        }
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerManager().getPlayerList().forEach(RiftFlight::tick));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> refresh(handler.player, true));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            GRANTS.remove(oldPlayer);
            refresh(newPlayer, true);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> refresh(player, true));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) refresh(player);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> GRANTS.clear());
    }

    public static boolean hasFullSet(ServerPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.RIFT_HELMET)
                && player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.RIFT_CHESTPLATE)
                && player.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.RIFT_LEGGINGS)
                && player.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.RIFT_BOOTS);
    }

    private static boolean eligible(ServerPlayerEntity player) {
        GameMode mode = player.interactionManager.getGameMode();
        return player.isAlive() && (mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE) && hasFullSet(player);
    }

    public static boolean ownsFlight(ServerPlayerEntity player) { return GRANTS.containsKey(player); }
    public static void refresh(ServerPlayerEntity player) { refresh(player, false); }
    public static void refresh(ServerPlayerEntity player, boolean forceSync) { update(player, true, forceSync); }

    private static void update(ServerPlayerEntity player, boolean send, boolean forceSync) {
        var abilities = player.getAbilities();
        boolean changed = false;
        Grant grant = GRANTS.get(player);
        if (eligible(player)) {
            if (grant == null) {
                grant = new Grant(player);
                GRANTS.put(player, grant);
            }
            changed = !abilities.allowFlying || Float.compare(abilities.getFlySpeed(), FLIGHT_SPEED) != 0;
            abilities.allowFlying = true;
            abilities.setFlySpeed(FLIGHT_SPEED);
        } else if (grant != null) {
            GRANTS.remove(player);
            // Never take Creative/Spectator flight away. Only undo the bonus we own.
            if (!player.isCreative() && !player.isSpectator()) {
                abilities.allowFlying = grant.previousMayFly;
                if (!abilities.allowFlying) abilities.flying = false;
            }
            abilities.setFlySpeed(grant.previousSpeed);
            changed = true;
        }
        if (send && (changed || forceSync) && player.networkHandler != null) player.sendAbilitiesUpdate();
    }

    public static void tick(ServerPlayerEntity player) {
        refresh(player);
        Grant grant = GRANTS.get(player);
        if (grant == null) return;
        boolean flying = player.getAbilities().flying && !player.hasVehicle();
        if (flying) {
            if (!grant.wasFlying && player.age - grant.lastSoundTick >= 20) {
                player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.35f, 1.4f);
                grant.lastSoundTick = player.age;
            }
            if (player.age % 6 == 0) player.getServerWorld().spawnParticles(ModParticles.RIFT_MOTE,
                    player.getX(), player.getY() + 0.8, player.getZ(), 2, 0.28, 0.35, 0.28, 0.008);
        }
        grant.wasFlying = flying;
    }

    /** Save baseline capabilities, not a permanent Survival mayfly/speed boost. */
    public static void writeSavedState(ServerPlayerEntity player, NbtCompound nbt) {
        Grant grant = GRANTS.get(player);
        nbt.remove(RESUME_TAG);
        if (grant == null || player.isCreative() || player.isSpectator()) return;
        NbtCompound abilities = nbt.getCompound("abilities");
        abilities.putBoolean("mayfly", grant.previousMayFly);
        abilities.putBoolean("flying", false);
        abilities.putFloat("flySpeed", grant.previousSpeed);
        nbt.put( "abilities", abilities);
        nbt.putBoolean(RESUME_TAG, eligible(player) && player.getAbilities().flying);
    }

    /** Inventory and saved game mode have already been loaded at this point. */
    public static void readSavedState(ServerPlayerEntity player, NbtCompound nbt) {
        GRANTS.remove(player);
        if (nbt.contains(RESUME_TAG)) {
            update(player, false, false);
            if (GRANTS.containsKey(player) && nbt.getBoolean(RESUME_TAG)) player.getAbilities().flying = true;
        }
    }
    private RiftFlight() { }
}
