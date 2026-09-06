package dev.riftborn.awakening.client;
import net.minecraft.client.particle.*;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
public final class AbyssParticle extends SpriteBillboardParticle {
    private final SpriteProvider sprites;
    private AbyssParticle(ClientWorld w, double x, double y, double z, double vx, double vy, double vz,
        SpriteProvider sprites, boolean rune) {
        super(w, x, y, z);
        this.sprites = sprites;
        velocityX = vx;
        velocityY = vy + 0.004;
        velocityZ = vz;
        maxAge = rune ? 35 : 60 + random.nextInt(35);
        scale = rune ? 0.25f : 0.09f + random.nextFloat() * 0.13f;
        collidesWithWorld = false;
        setColor(rune ? 1 : 0.35f, rune ? 0.78f : 0.9f, rune ? 0.45f : 1);
        setSpriteForAge(sprites);
    }
    @Override
    public void tick() {
        super.tick();
        setSpriteForAge(sprites);
        alpha = Math.min(1, Math.max(0, (maxAge - age) / 18f));
    }
    @Override
    public int getBrightness(float delta) {
        return LightmapTextureManager.MAX_LIGHT_COORDINATE;
    }
    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }
    public static final class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;
        private final boolean rune;
        public Factory(SpriteProvider s, boolean r) {
            sprites = s;
            rune = r;
        }
        @Override
        public Particle createParticle(
            SimpleParticleType t, ClientWorld w, double x, double y, double z, double vx, double vy, double vz) {
            return new AbyssParticle(w, x, y, z, vx, vy, vz, sprites, rune);
        }
    }
}
