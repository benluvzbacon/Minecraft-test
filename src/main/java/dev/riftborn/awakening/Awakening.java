package dev.riftborn.awakening;
import dev.riftborn.Riftborn;import dev.riftborn.dimension.RiftDimensions;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;import net.minecraft.registry.RegistryKeys;import net.minecraft.world.gen.GenerationStep;
/** Additive entry point: no legacy mob, worldgen, recipe, or teleport implementation is replaced. */
public final class Awakening {
    public static void initialize(){AbyssFx.initialize();AbyssBlocks.initialize();AbyssEntities.initialize();AbyssGear.initialize();AbyssItems.initialize();AwakeningNetwork.initialize();AwakeningProgress.initialize();
        BiomeModifications.addFeature(context->context.getBiomeKey().equals(RegistryKey.of(RegistryKeys.BIOME,Riftborn.id("void_reaches"))),GenerationStep.Feature.VEGETAL_DECORATION,RegistryKey.of(RegistryKeys.PLACED_FEATURE,Riftborn.id("awakening/echo_ferns")));
        ServerTickEvents.END_SERVER_TICK.register(AwakeningEvents::tick);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->AwakeningEvents.sync(handler.player));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((p,oldWorld,newWorld)->AwakeningEvents.sync(p));
    }
    private Awakening(){}
}
