package dev.riftborn.client;

import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;

public final class RiftDimensionEffects extends DimensionEffects {
    public RiftDimensionEffects() { super(Float.NaN, false, SkyType.END, true, false); }
    @Override public Vec3d adjustFogColor(Vec3d color, float sunHeight) { return new Vec3d(0.085, 0.025, 0.15); }
    @Override public boolean useThickFog(int camX, int camY) { return false; }
    @Override public float[] getFogColorOverride(float skyAngle, float tickDelta) { return null; }
}
