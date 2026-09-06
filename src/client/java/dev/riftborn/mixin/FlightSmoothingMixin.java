package dev.riftborn.mixin;
import dev.riftborn.awakening.AbyssGear;
import dev.riftborn.registry.ModItems;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Input prediction only: permissions, maximum speed, dash and cooldowns remain server-owned. */
@Mixin(ClientPlayerEntity.class)
abstract class FlightSmoothingMixin {
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void riftborn$brakeFlight(CallbackInfo ci) {
        var p = (ClientPlayerEntity) (Object) this;
        if (!p.getAbilities().flying || p.isCreative() || p.isSpectator())
            return;
        boolean rift = p.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.RIFT_HELMET)
            && p.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.RIFT_CHESTPLATE)
            && p.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.RIFT_LEGGINGS)
            && p.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.RIFT_BOOTS);
        if (!rift && !AbyssGear.fullSet(p))
            return;
        if (p.input.movementForward == 0 && p.input.movementSideways == 0) {
            var v = p.getVelocity();
            p.setVelocity(v.x * 0.75, v.y, v.z * 0.75);
        }
    }
}
