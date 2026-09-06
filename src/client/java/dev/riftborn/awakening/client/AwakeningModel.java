package dev.riftborn.awakening.client;
import dev.riftborn.awakening.entity.*;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;
/** Separate articulated silhouettes: prowler, manta, tank, fractured echo, sentinel, and three titans. */
public final class AwakeningModel<T extends AbyssHostileEntity> extends SinglePartEntityModel<T> {
    private final ModelPart root, head, rightArm, leftArm, rightLeg, leftLeg, ornaments;
    private final int kind;
    public AwakeningModel(ModelPart root, int kind) {
        this.root = root;
        this.kind = kind;
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        ornaments = root.getChild("ornaments");
    }
    private static ModelPartData part(ModelPartData root, String id, int u, int v, float x, float y, float z,
        float width, float height, float depth, float px, float py, float pz) {
        return root.addChild(id, ModelPartBuilder.create().uv(u, v).cuboid(x, y, z, width, height, depth),
            ModelTransform.pivot(px, py, pz));
    }
    public static TexturedModelData data(int kind) {
        var d = new ModelData();
        var r = d.getRoot();
        var o = r.addChild("ornaments", ModelPartBuilder.create(), ModelTransform.NONE);
        if (kind == 0) {
            part(r, "body", 0, 32, -7, -4, -9, 14, 8, 18, 0, 11, 0);
            part(r, "head", 0, 0, -5, -5, -7, 10, 8, 9, 0, 10, -9);
            part(r, "right_arm", 48, 32, -2, 0, -2, 4, 11, 4, -6, 13, -6);
            part(r, "left_arm", 48, 32, -2, 0, -2, 4, 11, 4, 6, 13, -6);
            part(r, "right_leg", 48, 48, -2, 0, -2, 4, 11, 4, -6, 13, 7);
            part(r, "left_leg", 48, 48, -2, 0, -2, 4, 11, 4, 6, 13, 7);
            for (int i = 0; i < 4; i++) part(o, "spine" + i, 96, 0, -1, -6, -1, 2, 6, 2, 0, 7, -5 + i * 4);
        } else if (kind == 1) {
            part(r, "body", 0, 32, -5, -5, -6, 10, 10, 12, 0, 13, 0);
            part(r, "head", 0, 0, -4, -3, -5, 8, 6, 6, 0, 12, -5);
            part(r, "right_arm", 64, 0, -20, 0, -7, 20, 2, 18, -4, 13, 0);
            part(r, "left_arm", 64, 0, 0, 0, -7, 20, 2, 18, 4, 13, 0);
            part(r, "right_leg", 64, 32, -12, 0, -2, 12, 2, 12, -3, 16, 4);
            part(r, "left_leg", 64, 32, 0, 0, -2, 12, 2, 12, 3, 16, 4);
            part(o, "tail", 96, 60, -1, 0, 0, 2, 2, 18, 0, 14, 7);
        } else {
            boolean huge = kind == 5;
            float bodyY = huge ? -34 : kind == 2 ? -12 : kind >= 6 ? -10 : 0;
            float bodyH = huge ? 34 : kind == 2 ? 24 : 20;
            float width = huge ? 26 : kind == 2 ? 18 : 12;
            float depth = huge ? 16 : kind == 2 ? 12 : 8;
            part(r, "body", 0, 32, -width / 2, 0, -depth / 2, width, bodyH, depth, 0, bodyY, 0);
            var h = part(r, "head", 0, 0, huge ? -8 : -5, huge ? -16 : -10, huge ? -7 : -5, huge ? 16 : 10,
                huge ? 16 : 10, huge ? 14 : 10, 0, bodyY, 0);
            float legY = bodyY + bodyH;
            float legH = 24 - legY;
            part(r, "right_leg", 64, 64, huge ? -5 : -3, 0, huge ? -5 : -3, huge ? 10 : 6, legH, huge ? 10 : 6,
                -width / 4, legY, 0);
            part(r, "left_leg", 88, 64, huge ? -5 : -3, 0, huge ? -5 : -3, huge ? 10 : 6, legH, huge ? 10 : 6,
                width / 4, legY, 0);
            float aw = huge ? 10 : kind == 2 ? 8 : 4, ah = huge ? 43 : kind == 2 ? 29 : 22;
            part(r, "right_arm", 64, 32, -aw, -2, -aw / 2, aw, ah, aw, -width / 2, bodyY + 4, 0);
            part(r, "left_arm", 88, 32, 0, -2, -aw / 2, aw, ah, aw, width / 2, bodyY + 4, 0);
            if (kind == 2 || huge) {
                part(o, "shoulder_r", 72, 0, -7, -5, -7, 14, 8, 14, -width / 2 - 4, bodyY + 1, 0);
                part(o, "shoulder_l", 72, 0, -7, -5, -7, 14, 8, 14, width / 2 + 4, bodyY + 1, 0);
                for (int i = -1; i <= 1; i += 2)
                    part(h, "horn" + i, 112, 0, -2, -8, -2, 4, 10, 4, i * 7, huge ? -14 : -8, 0);
            }
            if (kind == 3) {
                part(o, "echo_shard_r", 100, 90, -2, -3, -2, 4, 6, 4, -11, 3, 0);
                part(o, "echo_shard_l", 100, 90, -2, -3, -2, 4, 6, 4, 11, 9, 0);
            }
            if (kind == 4) {
                part(r.getChild("left_arm"), "shield", 72, 90, -3, 2, -5, 12, 18, 3, 0, 0, 0);
                part(r.getChild("right_arm"), "spear", 112, 0, -1, -13, -1, 2, 26, 2, -3, 7, 0);
            }
            if (kind >= 4 && kind != 5) {
                int count = kind == 7 ? 12 : 8;
                for (int i = 0; i < count; i++) {
                    double a = i * Math.PI * 2 / count;
                    part(o, "halo" + i, 112, 16, -1, -3, -1, 2, 6, 2, (float) Math.cos(a) * 14,
                        bodyY - 12 + (float) Math.sin(a) * 14, 0);
                }
            }
            if (kind == 6) {
                part(o, "robe", 0, 90, -10, 0, -6, 20, 15, 12, 0, 7, 0);
            }
            if (kind == 7) {
                for (int i = 0; i < 6; i++)
                    part(o, "ascendant" + i, 100, 100, -2, -8, -2, 4, 16, 4, (i - 2.5f) * 8, -12, 7);
            }
        }
        return TexturedModelData.of(d, 128, 128);
    }
    @Override
    public ModelPart getPart() {
        return root;
    }
    @Override
    public void setAngles(T e, float limb, float amount, float age, float yaw, float pitch) {
        root.traverse().forEach(ModelPart::resetTransform);
        head.yaw = yaw * MathHelper.RADIANS_PER_DEGREE;
        head.pitch = pitch * MathHelper.RADIANS_PER_DEGREE;
        if (kind == 1) {
            rightArm.roll = 0.18f + MathHelper.sin(age * 0.22f) * 0.25f;
            leftArm.roll = -rightArm.roll;
            rightLeg.roll = rightArm.roll * 0.6f;
            leftLeg.roll = -rightLeg.roll;
            root.pivotY = MathHelper.sin(age * 0.1f) * 1.5f;
        } else {
            rightLeg.pitch = MathHelper.cos(limb * 0.6f) * amount;
            leftLeg.pitch = -rightLeg.pitch;
            rightArm.pitch = leftLeg.pitch * 0.65f;
            leftArm.pitch = rightLeg.pitch * 0.65f;
            if (kind == 0) {
                rightArm.pitch = rightLeg.pitch;
                leftArm.pitch = leftLeg.pitch;
            }
            if (handSwingProgress > 0)
                rightArm.pitch -= MathHelper.sin(handSwingProgress * MathHelper.PI) * 1.5f;
            if (e instanceof AbyssBossEntity boss && boss.chargeTicks() > 0) {
                rightArm.pitch = -2.2f;
                leftArm.pitch = -2.2f;
            }
            if (kind == 3 || kind >= 6) {
                ornaments.yaw = age * 0.025f;
                root.pivotY = MathHelper.sin(age * 0.06f) * 1.2f;
            }
            if (kind == 7)
                for (int i = 0; i < 6; i++) ornaments.getChild("ascendant" + i).visible = e.visualState() >= 3;
        }
    }
}
