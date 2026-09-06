package dev.riftborn.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public final class RiftMoteParticle extends SpriteBillboardParticle {
    private final SpriteProvider sprites;

    private RiftMoteParticle(ClientWorld world, double x, double y, double z, double dx, double dy, double dz, SpriteProvider sprites) {
        super(world, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        velocityX = dx;
        velocityY = dy + 0.008;
        velocityZ = dz;
        maxAge = 28 + random.nextInt(24);
        scale = 0.07f + random.nextFloat() * 0.09f;
        collidesWithWorld = false;
        setColor(0.6f + random.nextFloat() * 0.25f, 0.3f + random.nextFloat() * 0.4f, 1.0f);
        setSpriteForAge(sprites);
    }
    @Override public void tick() {
        super.tick();
        setSpriteForAge(sprites);
        alpha = Math.max(0, 1.0f - (float) age / maxAge);
    }
    @Override public int getBrightness(float tint) { return LightmapTextureManager.MAX_LIGHT_COORDINATE; }
    @Override public ParticleTextureSheet getType() { return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT; }

    public static final class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;
        public Factory(SpriteProvider sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z,
                                                  double dx, double dy, double dz) {
            return new RiftMoteParticle(world, x, y, z, dx, dy, dz, sprites);
        }
    }
}
