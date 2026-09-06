package dev.riftborn.mixin;
import dev.riftborn.awakening.AwakeningCombat;import net.minecraft.entity.LivingEntity;import net.minecraft.entity.damage.DamageSource;import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(LivingEntity.class)
abstract class AwakeningDamageMixin {
    @ModifyVariable(method="damage",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float riftborn$limitedAegis(float amount,DamageSource source,float original){return (Object)this instanceof ServerPlayerEntity player?AwakeningCombat.damage(player,source,amount):amount;}
}
