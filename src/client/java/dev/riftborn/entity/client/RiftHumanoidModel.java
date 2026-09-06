package dev.riftborn.entity.client;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

/** Original, articulated stone-body model shared by three differently scaled/textured mobs. */
public final class RiftHumanoidModel<T extends MobEntity> extends SinglePartEntityModel<T> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public RiftHumanoidModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static TexturedModelData texturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData head = root.addChild("head", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4, -8, -4, 8, 8, 8), ModelTransform.NONE);
        head.addChild("crown", ModelPartBuilder.create().uv(0, 48)
                .cuboid(-5, -11, -1, 2, 5, 2).cuboid(3, -11, -1, 2, 5, 2), ModelTransform.NONE);
        root.addChild("body", ModelPartBuilder.create().uv(16, 16)
                .cuboid(-4, 0, -3, 8, 12, 6), ModelTransform.NONE);
        root.addChild("right_arm", ModelPartBuilder.create().uv(40, 16)
                .cuboid(-3, -2, -2, 4, 14, 4), ModelTransform.pivot(-5, 2, 0));
        root.addChild("left_arm", ModelPartBuilder.create().uv(40, 36).mirrored()
                .cuboid(-1, -2, -2, 4, 14, 4), ModelTransform.pivot(5, 2, 0));
        root.addChild("right_leg", ModelPartBuilder.create().uv(0, 16)
                .cuboid(-2, 0, -2, 4, 12, 4), ModelTransform.pivot(-2, 12, 0));
        root.addChild("left_leg", ModelPartBuilder.create().uv(0, 32)
                .cuboid(-2, 0, -2, 4, 12, 4), ModelTransform.pivot(2, 12, 0));
        return TexturedModelData.of(data, 64, 64);
    }

    @Override public ModelPart getPart() { return root; }
    @Override public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        root.traverse().forEach(ModelPart::resetTransform);
        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
        head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
        rightLeg.pitch = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
        leftLeg.pitch = MathHelper.cos(limbAngle * 0.6662f + MathHelper.PI) * 1.2f * limbDistance;
        rightArm.pitch = leftLeg.pitch * 0.7f;
        leftArm.pitch = rightLeg.pitch * 0.7f;
        rightArm.roll = 0.06f + MathHelper.cos(animationProgress * 0.07f) * 0.04f;
        leftArm.roll = -rightArm.roll;
        if (handSwingProgress > 0) {
            rightArm.pitch -= MathHelper.sin(handSwingProgress * MathHelper.PI) * 1.8f;
            leftArm.pitch -= MathHelper.sin(handSwingProgress * MathHelper.PI) * 0.5f;
        }
    }
}
