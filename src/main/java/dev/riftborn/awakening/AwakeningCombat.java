package dev.riftborn.awakening;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.*;
/** Bounded, cooldown-based mitigation. Never cancels a hit or changes invulnerability. */
public final class AwakeningCombat {
    public static float damage(ServerPlayerEntity p, DamageSource source, float amount) {
        if (amount <= 0 || !p.isAlive() || AbyssGear.creative(p) || p.isSpectator()
            || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY))
            return amount;
        boolean ascended = AbyssGear.fullSet(p),
                ward = AbyssGear.artifact(p, AbyssItems.WARDING_ANCHOR) && p.isOnGround()
            && p.getVelocity().horizontalLengthSquared() < 0.015;
        if (!ascended && !ward)
            return amount;
        var state = AwakeningState.get(p.getServer());
        var data = state.player(p.getUuid());
        long now = p.getServer().getOverworld().getTime();
        float result = amount;
        if (ascended && source.isIn(DamageTypeTags.IS_FALL))
            result *= 0.4f;
        if (ascended && amount >= 3 && now >= data.aegisReady) {
            result -= Math.min(result * 0.5f, 6);
            data.aegisReady = now + 200;
            p.getItemCooldownManager().set(AbyssItems.ABYSSAL_CHESTPLATE, 200);
            AbyssFx.burst(p.getServerWorld(), p.getPos().add(0, 1, 0), 16, 0.5);
            state.markDirty();
            p.getServerWorld().playSound(
                null, p.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.65f, 0.8f);
        }
        if (ward && amount >= 2 && now >= data.wardReady) {
            result -= Math.min(result * 0.25f, 3);
            data.wardReady = now + 200;
            state.markDirty();
        }
        return Math.max(0.5f, result);
    }
    private AwakeningCombat() {}
}
