package dev.riftborn.awakening;

import dev.riftborn.Riftborn;
import dev.riftborn.awakening.entity.AbyssBossEntity;
import dev.riftborn.entity.RiftGuardianEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public final class AwakeningProgress {
    public static void grant(ServerPlayerEntity p, String name) {
        var entry = p.getServer().getAdvancementLoader().get(Riftborn.id("awakening/" + name));
        if (entry != null)
            for (String criterion : entry.value().criteria().keySet())
                p.getAdvancementTracker().grantCriterion(entry, criterion);
    }
    public static void unlock(ServerPlayerEntity p, int flag, String advancement) {
        AwakeningState.get(p.getServer()).unlock(p.getUuid(), flag);
        grant(p, advancement);
        int all = AwakeningState.GUARDIAN | AwakeningState.ABYSS | AwakeningState.COLOSSUS | AwakeningState.ARCHITECT
            | AwakeningState.SOVEREIGN;
        if (AwakeningState.get(p.getServer()).has(p.getUuid(), all))
            grant(p, "rift_master");
    }
    public static void legacyGuardian(ServerPlayerEntity p) {
        var entry = p.getServer().getAdvancementLoader().get(Riftborn.id("guardian"));
        if (entry != null && p.getAdvancementTracker().getProgress(entry).isDone()) {
            var state = AwakeningState.get(p.getServer());
            state.awakened = true;
            state.markDirty();
            unlock(p, AwakeningState.GUARDIAN, "guardian_fallen");
        }
    }
    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> legacyGuardian(handler.player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.getWorld() instanceof ServerWorld world))
                return;
            if (entity instanceof RiftGuardianEntity) {
                var state = AwakeningState.get(world.getServer());
                state.awakened = true;
                state.markDirty();
                for (var p : world.getPlayers(p -> p.squaredDistanceTo(entity) < 128 * 128 && !p.isSpectator())) {
                    unlock(p, AwakeningState.GUARDIAN, "guardian_fallen");
                    p.sendMessage(Text.translatable("awakening.riftborn.guardian_secret"), false);
                }
            }
            if (entity instanceof AbyssBossEntity boss) {
                for (var p : world.getPlayers(p -> p.squaredDistanceTo(entity) < 128 * 128 && !p.isSpectator())) {
                    int flag = boss.bossKind() == 0 ? AwakeningState.COLOSSUS
                        : boss.bossKind() == 1      ? AwakeningState.ARCHITECT
                        : boss.bossKind() == 2      ? AwakeningState.SOVEREIGN
                                                    : AwakeningState.COLLAPSE;
                    unlock(p, flag,
                        new String[] {
                            "colossal", "reality_breaker", "sovereign_slayer", "the_collapse"}[boss.bossKind()]);
                }
                if (boss.bossKind() == 3)
                    AwakeningEvents.finishCollapse(world);
            }
            if (entity.getCommandTags().contains("riftborn_storm"))
                world.spawnEntity(new ItemEntity(
                    world, entity.getX(), entity.getY() + 0.5, entity.getZ(), new ItemStack(AbyssItems.STORMGLASS)));
        });
    }
    private AwakeningProgress() {}
}
