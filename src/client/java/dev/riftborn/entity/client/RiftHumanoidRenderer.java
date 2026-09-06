package dev.riftborn.entity.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

public final class RiftHumanoidRenderer<T extends MobEntity> extends MobEntityRenderer<T, RiftHumanoidModel<T>> {
    private final Identifier texture;
    private final float modelScale;

    public RiftHumanoidRenderer(EntityRendererFactory.Context context, EntityModelLayer layer, Identifier texture, float scale) {
        super(context, new RiftHumanoidModel<>(context.getPart(layer)), 0.45f * scale);
        this.texture = texture;
        this.modelScale = scale;
    }
    @Override public Identifier getTexture(T entity) { return texture; }
    @Override protected void scale(T entity, MatrixStack matrices, float tickDelta) {
        matrices.scale(modelScale, modelScale, modelScale);
    }
}
