package dev.riftborn;

import dev.riftborn.config.RiftbornConfig;
import dev.riftborn.effect.RiftFlight;
import dev.riftborn.registry.ModBlocks;
import dev.riftborn.registry.ModEntities;
import dev.riftborn.registry.ModItems;
import dev.riftborn.registry.ModParticles;
import dev.riftborn.registry.ModPointsOfInterest;
import dev.riftborn.world.RiftWorldgen;
import dev.riftborn.world.RiftStructures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Riftborn implements ModInitializer {
    public static final String MOD_ID = "riftborn";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static RiftbornConfig CONFIG;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CONFIG = RiftbornConfig.load();
        ModParticles.initialize();
        ModBlocks.initialize();
        ModPointsOfInterest.initialize();
        ModEntities.initialize();
        ModItems.initialize();
        RiftStructures.initialize();
        RiftWorldgen.initialize();
        dev.riftborn.awakening.Awakening.initialize();
        RiftFlight.initialize();
        LOGGER.info("Riftborn initialized: follow the fractures.");
    }
}
