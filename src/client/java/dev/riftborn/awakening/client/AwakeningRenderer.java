package dev.riftborn.awakening.client;
import dev.riftborn.Riftborn;
import dev.riftborn.awakening.entity.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
public final class AwakeningRenderer<T extends AbyssHostileEntity> extends MobEntityRenderer<T, AwakeningModel<T>> {
    private final Identifier texture, awakened;
    private final int kind;
    public AwakeningRenderer(EntityRendererFactory.Context context, EntityModelLayer layer, String name, int kind) {
        super(context, new AwakeningModel<>(context.getPart(layer), kind), kind == 5 ? 1.8f : kind == 2 ? 0.9f : 0.55f);
        this.kind = kind;
        texture = Riftborn.id("textures/entity/awakening/" + name + ".png");
        awakened = Riftborn.id("textures/entity/awakening/abyss_sovereign_awakened.png");
        if (kind == 0)
            addFeature(new EyesFeatureRenderer<T, AwakeningModel<T>>(this) {
                @Override
                public RenderLayer getEyesTexture() {
                    return RenderLayer.getEyes(Riftborn.id("textures/entity/awakening/abyss_stalker_eyes.png"));
                }
            });
    }
    @Override
    public Identifier getTexture(T e) {
        return kind == 7 && e.visualState() >= 3 ? awakened : texture;
    }
    @Override
    protected int getBlockLight(T e, BlockPos p) {
        return kind == 7 && e.visualState() >= 3 ? 15 : Math.max(kind == 0 ? 10 : 12, super.getBlockLight(e, p));
    }
    @Override
    protected void scale(T e, MatrixStack matrices, float delta) {
        float scale = kind == 5 ? 1.15f : kind == 7 && e.visualState() >= 3 ? 1.2f : 1;
        matrices.scale(scale, scale, scale);
    }
}
