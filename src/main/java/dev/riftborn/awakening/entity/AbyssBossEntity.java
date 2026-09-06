package dev.riftborn.awakening.entity;
import dev.riftborn.awakening.*;
import net.minecraft.block.Block;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.boss.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.*;
/** Shared encounter lifecycle, telegraphs, bounded summons and terrain-safe temporary geometry. */
public abstract class AbyssBossEntity extends AbyssHostileEntity {
    private static final TrackedData<Integer> PHASE =
        DataTracker.registerData(AbyssBossEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CHARGE =
        DataTracker.registerData(AbyssBossEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private final ServerBossBar bar = new ServerBossBar(Text.empty(), BossBar.Color.BLUE, BossBar.Style.NOTCHED_12);
    public int shotsFired, pulsesReleased, summonsMade, barriersMade;
    protected int battleTicks;
    private BlockPos home;
    private boolean musicPlayed, scaled;
    private final List<Pulse> pulses = new ArrayList<>();
    private final Set<BlockPos> barriers = new HashSet<>();
    private static final class Pulse {
        final Vec3d center;
        final double radius, inner;
        final float damage;
        final boolean grounded;
        int remaining;
        Pulse(Vec3d c, double r, float d, boolean g, double i, int t) {
            center = c;
            radius = r;
            damage = d;
            grounded = g;
            inner = i;
            remaining = t;
        }
    }
    protected AbyssBossEntity(EntityType<? extends AbyssBossEntity> t, World w) {
        super(t, w);
        setPersistent();
        experiencePoints = 250;
    }
    public abstract int bossKind();
    protected int phaseForHealth() {
        return getHealth() <= getMaxHealth() * 0.5f ? 2 : 1;
    }
    public int phase() {
        return dataTracker.get(PHASE);
    }
    public int chargeTicks() {
        return dataTracker.get(CHARGE);
    }
    @Override
    public int visualState() {
        return phase();
    }
    @Override
    protected void initDataTracker(DataTracker.Builder b) {
        super.initDataTracker(b);
        b.add(PHASE, 1);
        b.add(CHARGE, 0);
    }
    public BlockPos arenaCenter() {
        return home == null ? getBlockPos() : home;
    }
    public void setArena(BlockPos pos) {
        home = pos.toImmutable();
    }
    @Override
    public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld w))
            return;
        if (home == null)
            home = getBlockPos();
        int stage = phaseForHealth();
        if (stage > phase()) {
            dataTracker.set(PHASE, stage);
            AbyssFx.burst(w, getPos().add(0, getHeight() / 2, 0), 48, 2);
            playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.9f, 0.7f + stage * 0.12f);
        }
        bar.setName(chargeTicks() > 0
                ? Text.translatable(bossKind() == 2 && chargeTicks() > 50 ? "boss.riftborn.last_silence"
                                                                          : "boss.riftborn.abyss_warning",
                      getDisplayName())
                : getDisplayName());
        bar.setPercent(Math.clamp(getHealth() / getMaxHealth(), 0, 1));
        bar.setColor(phase() == 3 ? BossBar.Color.RED : phase() == 2 ? BossBar.Color.PURPLE : BossBar.Color.BLUE);
    }
    @Override
    protected void mobTick() {
        super.mobTick();
        var w = (ServerWorld) getWorld();
        int charge = 0;
        for (var iterator = pulses.iterator(); iterator.hasNext();) {
            var p = iterator.next();
            if (p.remaining % 10 == 0) {
                AbyssFx.ring(w, p.center.add(0, 0.2, 0), p.radius, 16);
                if (p.inner > 0)
                    AbyssFx.ring(w, p.center.add(0, 0.3, 0), p.inner, 12);
            }
            if (--p.remaining <= 0) {
                CombatEffects.pulse(w, this, p.center, p.radius, p.damage, p.grounded, p.inner);
                pulsesReleased++;
                iterator.remove();
            } else
                charge = Math.max(charge, p.remaining);
        }
        dataTracker.set(CHARGE, charge);
        var target = getTarget();
        if (target == null || !target.isAlive())
            return;
        if (!scaled) {
            scaled = true;
            int count = w.getPlayers(p -> p.squaredDistanceTo(this) < 96 * 96 && !p.isSpectator()).size();
            if (count > 1) {
                double base = getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
                getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                    .setBaseValue(base * (1 + 0.25 * Math.min(count - 1, 3)));
                setHealth(getMaxHealth());
            }
        }
        if (!musicPlayed) {
            musicPlayed = true;
            SoundEvent music = Registries.SOUND_EVENT.get(Identifier.of("minecraft", "music.end"));
            if (music != null)
                w.playSound(null, getX(), getY(), getZ(), music, SoundCategory.MUSIC, 0.45f, 1);
        }
        if (squaredDistanceTo(Vec3d.ofCenter(arenaCenter())) > 80 * 80) {
            safeTeleport(w, Vec3d.ofCenter(arenaCenter()));
            return;
        }
        if (chargeTicks() == 0) {
            battleTicks++;
            attackPattern(w, target);
        }
    }
    protected abstract void attackPattern(ServerWorld w, LivingEntity target);
    protected void pulse(Vec3d center, double radius, float damage, boolean groundOnly, double inner, int warning) {
        if (pulses.size() >= 3)
            return;
        pulses.add(new Pulse(center, radius, damage, groundOnly, inner, warning));
        dataTracker.set(CHARGE, warning);
        addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, warning, 3, false, false));
        playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.65f);
    }
    protected void bolts(ServerWorld w, LivingEntity target, int count, float damage, float speed) {
        if (!getVisibilityCache().canSee(target))
            return;
        Vec3d aim = target.getEyePos();
        shotsFired += Math.min(count, 5);
        for (int i = 0; i < Math.min(count, 5); i++)
            AbyssBoltEntity.fire(w, this, aim.add((i - (count - 1) / 2.0) * 1.1, 0, (i % 2) * 0.5), damage, speed);
        playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 0.7f, 0.75f);
    }
    protected void summon(ServerWorld w, EntityType<? extends AbyssHostileEntity> type, int cap) {
        int alive = w.getEntitiesByClass(
                         AbyssHostileEntity.class, getBoundingBox().expand(96), m -> getUuid().equals(m.summoner()))
                        .size();
        if (alive >= cap)
            return;
        for (int attempt = 0; attempt < 10; attempt++) {
            double a = random.nextDouble() * Math.PI * 2;
            int x = (int) Math.floor(getX() + Math.cos(a) * 9), z = (int) Math.floor(getZ() + Math.sin(a) * 9);
            if (!w.getChunkManager().isChunkLoaded(x >> 4, z >> 4))
                continue;
            int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y < w.getBottomY() + 32 || Math.abs(y - getY()) > 16)
                continue;
            var mob = type.create(w);
            if (mob == null)
                return;
            mob.refreshPositionAndAngles(x + 0.5, y, z + 0.5, random.nextFloat() * 360, 0);
            if (!w.isSpaceEmpty(mob, mob.getBoundingBox())) {
                mob.discard();
                continue;
            }
            mob.summonedBy(getUuid(), 600);
            mob.setTarget(getTarget());
            w.spawnEntity(mob);
            summonsMade++;
            AbyssFx.burst(w, mob.getPos(), 16, 0.5);
            return;
        }
    }
    protected void teleportNear(ServerWorld w, LivingEntity target) {
        for (int i = 0; i < 6; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            if (safeTeleport(w, target.getPos().add(Math.cos(a) * 10, 0, Math.sin(a) * 10)))
                break;
        }
    }
    private boolean safeTeleport(ServerWorld w, Vec3d dest) {
        int x = MathHelper.floor(dest.x), z = MathHelper.floor(dest.z);
        if (!w.getChunkManager().isChunkLoaded(x >> 4, z >> 4))
            return false;
        int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y < w.getBottomY() + 32 || Math.abs(y - getY()) > 24)
            return false;
        Vec3d p = new Vec3d(x + 0.5, y, z + 0.5);
        Box b = getBoundingBox().offset(p.subtract(getPos()));
        if (!w.isSpaceEmpty(this, b) || !w.getOtherEntities(this, b, e -> e.isAlive()).isEmpty())
            return false;
        AbyssFx.burst(w, getPos(), 24, 1);
        requestTeleport(p.x, p.y, p.z);
        AbyssFx.burst(w, p, 24, 1);
        return true;
    }
    protected void barriers(ServerWorld w) {
        barriers.removeIf(p
            -> !w.getChunkManager().isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)
                || !w.getBlockState(p).isOf(AbyssBlocks.BARRIER));
        if (barriers.size() > 40)
            return;
        BlockPos base = arenaCenter();
        for (int side = 0; side < 4; side++)
            for (int x = -2; x <= 2; x++) {
                BlockPos column =
                    base.add(new BlockPos(x, 0, 10).rotate(net.minecraft.util.BlockRotation.values()[side]));
                if (!w.getChunkManager().isChunkLoaded(column.getX() >> 4, column.getZ() >> 4))
                    continue;
                int floor = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
                if (Math.abs(floor - base.getY()) > 5)
                    continue;
                for (int y = 0; y < 3; y++) {
                    BlockPos p = new BlockPos(column.getX(), floor + y, column.getZ());
                    if (!w.getWorldBorder().contains(p) || !w.getBlockState(p).isAir()
                        || !w.getOtherEntities(null, new Box(p), Entity::isAlive).isEmpty())
                        continue;
                    if (barriers.size() >= 60)
                        return;
                    w.setBlockState(p, AbyssBlocks.BARRIER.getDefaultState(), Block.NOTIFY_ALL);
                    barriers.add(p.toImmutable());
                    barriersMade++;
                }
            }
    }
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity p) {
        super.onStartedTrackingBy(p);
        bar.addPlayer(p);
    }
    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity p) {
        super.onStoppedTrackingBy(p);
        bar.removePlayer(p);
    }
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() instanceof net.minecraft.entity.player.PlayerEntity p
            && squaredDistanceTo(p) > 64 * 64)
            return false;
        return super.damage(source, amount);
    }
    @Override
    public void onDeath(DamageSource source) {
        if (getWorld() instanceof ServerWorld w) {
            for (var p : barriers)
                if (w.getChunkManager().isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)
                    && w.getBlockState(p).isOf(AbyssBlocks.BARRIER))
                    w.removeBlock(p, false);
            for (var m : w.getEntitiesByClass(
                     AbyssHostileEntity.class, getBoundingBox().expand(128), e -> getUuid().equals(e.summoner())))
                m.discard();
            AbyssFx.burst(w, getPos().add(0, 2, 0), 64, 3);
            for (var player : bar.getPlayers())
                player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.StopSoundS2CPacket(
                    Identifier.of("minecraft", "music.end"), SoundCategory.MUSIC));
            bar.clearPlayers();
        }
        super.onDeath(source);
    }
    @Override
    public boolean canImmediatelyDespawn(double d) {
        return false;
    }
    @Override
    public void writeCustomDataToNbt(NbtCompound n) {
        super.writeCustomDataToNbt(n);
        n.putInt("AbyssPhase", phase());
        if (home != null)
            n.putLong("AbyssArena", home.asLong());
        n.putBoolean("AbyssScaled", scaled);
        n.putBoolean("AbyssMusic", musicPlayed);
    }
    @Override
    public void readCustomDataFromNbt(NbtCompound n) {
        super.readCustomDataFromNbt(n);
        dataTracker.set(PHASE, Math.clamp(n.getInt("AbyssPhase"), 1, 3));
        if (n.contains("AbyssArena"))
            home = BlockPos.fromLong(n.getLong("AbyssArena"));
        scaled = n.getBoolean("AbyssScaled");
        musicPlayed = n.getBoolean("AbyssMusic");
    }
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WARDEN_AMBIENT;
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.ENTITY_IRON_GOLEM_HURT;
    }
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ENDER_DRAGON_DEATH;
    }
}
