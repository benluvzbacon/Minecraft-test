package dev.riftborn.awakening.client;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;
public final class AbyssDimensionEffects extends DimensionEffects {
    public AbyssDimensionEffects() {
        super(Float.NaN, false, SkyType.END, true, false);
    }
    @Override
    public Vec3d adjustFogColor(Vec3d c, float h) {
        return AwakeningClient.eventKind == 2 ? new Vec3d(0.13, 0.025, 0.085) : new Vec3d(0.018, 0.055, 0.073);
    }
    @Override
    public boolean useThickFog(int x, int y) {
        return false;
    }
    @Override
    public float[] getFogColorOverride(float a, float d) {
        return null;
    }
}
