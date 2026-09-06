package dev.riftborn.awakening.entity;
import dev.riftborn.awakening.*;import dev.riftborn.entity.AbstractRiftHostileEntity;import dev.riftborn.entity.RiftWispEntity;
import net.minecraft.entity.*;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.entity.projectile.thrown.ThrownItemEntity;import net.minecraft.item.Item;import net.minecraft.nbt.NbtCompound;import net.minecraft.server.world.ServerWorld;import net.minecraft.util.hit.*;import net.minecraft.util.math.Vec3d;import net.minecraft.world.World;
public final class AbyssBoltEntity extends ThrownItemEntity {
    private float damage=7,knockback=0.2f;private int life=90;private boolean ignite;
    public AbyssBoltEntity(EntityType<? extends AbyssBoltEntity> type,World w){super(type,w);setNoGravity(true);}
    private AbyssBoltEntity(ServerWorld w,LivingEntity owner,float damage){super(AbyssEntities.BOLT,owner,w);this.damage=damage;setNoGravity(true);}
    public static AbyssBoltEntity fire(ServerWorld w,LivingEntity owner,Vec3d target,float damage,float speed){var b=new AbyssBoltEntity(w,owner,damage);var d=target.subtract(b.getPos()).normalize();b.setVelocity(d.x,d.y,d.z,speed,0.35f);w.spawnEntity(b);return b;}
    public void setImpact(float knockback,boolean ignite){this.knockback=knockback;this.ignite=ignite;}
    @Override protected Item getDefaultItem(){return AbyssItems.ABYSSAL_SHARD;}
    @Override protected boolean canHit(Entity e){return (getOwner() instanceof PlayerEntity||(!(e instanceof AbyssHostileEntity)&&!(e instanceof AbstractRiftHostileEntity)&&!(e instanceof RiftWispEntity)))&&super.canHit(e);}
    @Override public void tick(){super.tick();if(getWorld().isClient){if(age%2==0)getWorld().addParticle(AbyssFx.MOTE,getX(),getY(),getZ(),0,0,0);}else if(--life<=0)discard();}
    @Override protected void onEntityHit(EntityHitResult hit){super.onEntityHit(hit);if(!getWorld().isClient){var e=hit.getEntity();if(e.damage(getDamageSources().thrown(this,getOwner()),damage)){if(ignite)e.setOnFireFor(4);if(e instanceof LivingEntity target){Vec3d d=getVelocity().normalize();target.takeKnockback(knockback,-d.x,-d.z);}}}}
    @Override protected void onCollision(HitResult hit){super.onCollision(hit);if(getWorld() instanceof ServerWorld w){AbyssFx.burst(w,getPos(),8,0.2);discard();}}
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putFloat("AbyssDamage",damage);n.putFloat("AbyssKnockback",knockback);n.putBoolean("AbyssIgnite",ignite);n.putInt("AbyssBoltLife",life);}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);damage=Math.clamp(n.getFloat("AbyssDamage"),0,40);knockback=Math.clamp(n.getFloat("AbyssKnockback"),0,3);ignite=n.getBoolean("AbyssIgnite");life=Math.clamp(n.getInt("AbyssBoltLife"),1,90);setNoGravity(true);}
}
