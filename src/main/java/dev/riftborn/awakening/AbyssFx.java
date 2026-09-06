package dev.riftborn.awakening;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import dev.riftborn.Riftborn;
public final class AbyssFx {
    public static final SimpleParticleType MOTE=Registry.register(Registries.PARTICLE_TYPE,Riftborn.id("abyss_mote"),FabricParticleTypes.simple());
    public static final SimpleParticleType RUNE=Registry.register(Registries.PARTICLE_TYPE,Riftborn.id("abyss_rune"),FabricParticleTypes.simple());
    public static void burst(ServerWorld w,Vec3d p,int count,double spread){w.spawnParticles(MOTE,p.x,p.y,p.z,Math.clamp(count,1,64),spread,spread,spread,0.035);}
    public static void ring(ServerWorld w,Vec3d p,double radius,int count){for(int i=0;i<Math.min(count,32);i++){double a=i*Math.PI*2/count;w.spawnParticles(RUNE,p.x+Math.cos(a)*radius,p.y,p.z+Math.sin(a)*radius,1,0,0,0,0);}}
    public static void line(ServerWorld w,Vec3d a,Vec3d b,int count){for(int i=0;i<Math.min(count,32);i++){var p=a.lerp(b,(double)i/count);w.spawnParticles(MOTE,p.x,p.y,p.z,1,0,0,0,0);}}
    public static void initialize(){} private AbyssFx(){}
}
