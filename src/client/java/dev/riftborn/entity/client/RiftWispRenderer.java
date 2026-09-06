package dev.riftborn.entity.client;

import dev.riftborn.Riftborn;
import dev.riftborn.client.RiftbornClient;
import dev.riftborn.entity.RiftWispEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class RiftWispRenderer extends MobEntityRenderer<RiftWispEntity, RiftWispModel> {
    private static final Identifier TEXTURE = Riftborn.id("textures/entity/rift_wisp.png");
    public RiftWispRenderer(EntityRendererFactory.Context context) {
        super(context, new RiftWispModel(context.getPart(RiftbornClient.WISP_LAYER)), 0.25f);
    }
    @Override public Identifier getTexture(RiftWispEntity entity) { return TEXTURE; }
    @Override protected int getBlockLight(RiftWispEntity entity, BlockPos pos) { return 15; }
}
