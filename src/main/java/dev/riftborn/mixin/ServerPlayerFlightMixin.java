package dev.riftborn.mixin;

import dev.riftborn.effect.RiftFlight;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
abstract class ServerPlayerFlightMixin {
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void riftborn$saveTransientFlight(NbtCompound nbt, CallbackInfo ci) {
        RiftFlight.writeSavedState((ServerPlayerEntity) (Object) this, nbt);
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void riftborn$validateSavedFlight(NbtCompound nbt, CallbackInfo ci) {
        RiftFlight.readSavedState((ServerPlayerEntity) (Object) this, nbt);
    }
    @Inject(method = "changeGameMode", at = @At("RETURN"))
    private void riftborn$refreshGameMode(GameMode mode, CallbackInfoReturnable<Boolean> cir) {
        RiftFlight.refresh((ServerPlayerEntity) (Object) this);
    }
}
