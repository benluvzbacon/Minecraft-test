package dev.riftborn.entity.client;

import dev.riftborn.entity.RiftWispEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public final class RiftWispModel extends SinglePartEntityModel<RiftWispEntity> {
    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart ring;

    public RiftWispModel(ModelPart root) {
        this.root = root;
        core = root.getChild("core");
        ring = root.getChild("ring");
    }
    public static TexturedModelData texturedModelData() {
        ModelData data = new ModelData();
        data.getRoot().addChild("core", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4, -4, -4, 8, 8, 8), ModelTransform.pivot(0, 17, 0));
        data.getRoot().addChild("ring", ModelPartBuilder.create().uv(0, 18)
                .cuboid(-9, -1, -1, 3, 2, 2).cuboid(6, -1, -1, 3, 2, 2)
                .cuboid(-1, -1, -9, 2, 2, 3).cuboid(-1, -1, 6, 2, 2, 3), ModelTransform.pivot(0, 17, 0));
        return TexturedModelData.of(data, 32, 32);
    }
    @Override public ModelPart getPart() { return root; }
    @Override public void setAngles(RiftWispEntity entity, float limbAngle, float limbDistance, float age, float yaw, float pitch) {
        core.pivotY = 17 + MathHelper.sin(age * 0.1f) * 1.2f;
        core.yaw = yaw * MathHelper.RADIANS_PER_DEGREE;
        core.roll = MathHelper.sin(age * 0.05f) * 0.15f;
        ring.pivotY = core.pivotY;
        ring.yaw = age * 0.055f;
        ring.roll = MathHelper.sin(age * 0.06f) * 0.3f;
    }
}
