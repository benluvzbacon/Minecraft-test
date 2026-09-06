package dev.riftborn.client;

import dev.riftborn.Riftborn;
import dev.riftborn.dimension.RiftDimensions;
import dev.riftborn.entity.client.RiftHumanoidModel;
import dev.riftborn.entity.client.RiftHumanoidRenderer;
import dev.riftborn.entity.client.RiftWispModel;
import dev.riftborn.entity.client.RiftWispRenderer;
import dev.riftborn.registry.ModBlocks;
import dev.riftborn.registry.ModEntities;
import dev.riftborn.registry.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;

public final class RiftbornClient implements ClientModInitializer {
    public static final EntityModelLayer STALKER_LAYER = new EntityModelLayer(Riftborn.id("rift_stalker"), "main");
    public static final EntityModelLayer BRUTE_LAYER = new EntityModelLayer(Riftborn.id("void_brute"), "main");
    public static final EntityModelLayer GUARDIAN_LAYER = new EntityModelLayer(Riftborn.id("rift_guardian"), "main");
    public static final EntityModelLayer WISP_LAYER = new EntityModelLayer(Riftborn.id("rift_wisp"), "main");

    @Override public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(STALKER_LAYER, RiftHumanoidModel::texturedModelData);
        EntityModelLayerRegistry.registerModelLayer(BRUTE_LAYER, RiftHumanoidModel::texturedModelData);
        EntityModelLayerRegistry.registerModelLayer(GUARDIAN_LAYER, RiftHumanoidModel::texturedModelData);
        EntityModelLayerRegistry.registerModelLayer(WISP_LAYER, RiftWispModel::texturedModelData);
        EntityRendererRegistry.register(ModEntities.RIFT_STALKER, context -> new RiftHumanoidRenderer<>(context,
                STALKER_LAYER, Riftborn.id("textures/entity/rift_stalker.png"), 1.0f));
        EntityRendererRegistry.register(ModEntities.VOID_BRUTE, context -> new RiftHumanoidRenderer<>(context,
                BRUTE_LAYER, Riftborn.id("textures/entity/void_brute.png"), 1.45f));
        EntityRendererRegistry.register(ModEntities.RIFT_GUARDIAN, context -> new RiftHumanoidRenderer<>(context,
                GUARDIAN_LAYER, Riftborn.id("textures/entity/rift_guardian.png"), 1.7f));
        EntityRendererRegistry.register(ModEntities.RIFT_WISP, RiftWispRenderer::new);
        EntityRendererRegistry.register(ModEntities.RIFT_BOLT, context -> new FlyingItemEntityRenderer<>(context, 0.65f, true));
        ParticleFactoryRegistry.getInstance().register(ModParticles.RIFT_MOTE, RiftMoteParticle.Factory::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.VOID_BLOOM, RenderLayer.getCutout());
        DimensionRenderingRegistry.registerDimensionEffects(Riftborn.id("the_rift"), new RiftDimensionEffects());
        DimensionRenderingRegistry.registerSkyRenderer(RiftDimensions.WORLD, RiftSkyRenderer::render);
        Riftborn.LOGGER.info("Riftborn client renderers, particles and sky registered");
    }
}
