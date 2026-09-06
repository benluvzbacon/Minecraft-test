package dev.riftborn.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.riftborn.Riftborn;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Server-authoritative settings. Clients never decide travel, damage, or cooldowns. */
public final class RiftbornConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public int blinkCooldownTicks = 100;
    public double blinkDistance = 8.0;
    public int compassCooldownTicks = 100;
    public int locateRadius = 32;
    public int overworldStalkerWeight = 6;
    public float riftFlightSpeed = 0.15f;
    public float abyssalFlightSpeed = 0.22f;
    public boolean abyssalDashEnabled = true;
    public int abyssalDashCooldownTicks = 160;
    public boolean realmEventsEnabled = true;
    public int stormIntervalTicks = 36000;
    public int collapseIntervalTicks = 144000;
    public int eventMobCap = 8;

    public static RiftbornConfig load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("riftborn.json");
        RiftbornConfig config = new RiftbornConfig();
        try {
            if (Files.exists(file)) {
                RiftbornConfig parsed = GSON.fromJson(Files.readString(file), RiftbornConfig.class);
                if (parsed != null) config = parsed;
            } else {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(config) + "\n");
            }
        } catch (IOException | RuntimeException exception) {
            Riftborn.LOGGER.warn("Could not read riftborn.json; using safe defaults", exception);
        }
        config.blinkCooldownTicks = Math.clamp(config.blinkCooldownTicks, 20, 1200);
        config.blinkDistance = Double.isFinite(config.blinkDistance)
                ? Math.clamp(config.blinkDistance, 2.0, 12.0) : 8.0;
        config.compassCooldownTicks = Math.clamp(config.compassCooldownTicks, 40, 1200);
        config.locateRadius = Math.clamp(config.locateRadius, 4, 64);
        config.overworldStalkerWeight = Math.clamp(config.overworldStalkerWeight, 0, 30);
        config.riftFlightSpeed = Float.isFinite(config.riftFlightSpeed) ? Math.clamp(config.riftFlightSpeed, 0.06f, 0.35f) : 0.15f;
        config.abyssalFlightSpeed = Float.isFinite(config.abyssalFlightSpeed) ? Math.clamp(config.abyssalFlightSpeed, config.riftFlightSpeed + 0.025f, 0.5f) : Math.max(0.22f, config.riftFlightSpeed + 0.025f);
        config.abyssalDashCooldownTicks = Math.clamp(config.abyssalDashCooldownTicks, 60, 1200);
        config.stormIntervalTicks = Math.clamp(config.stormIntervalTicks, 12000, 144000);
        config.collapseIntervalTicks = Math.clamp(config.collapseIntervalTicks, 72000, 720000);
        config.eventMobCap = Math.clamp(config.eventMobCap, 2, 12);
        return config;
    }
}
