package dev.riftborn.mixin;

import dev.riftborn.effect.RiftFlight;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
abstract class ServerPlayFlightMixin {
    @Shadow public ServerPlayerEntity player;

    private boolean riftborn$onServerThread() { return player.getServerWorld().getServer().isOnThread(); }

    // HEAD is visited on Netty and again after vanilla reschedules onto the server thread.
    @Inject(method = {"onUpdatePlayerAbilities", "onPlayerMove"}, at = @At("HEAD"))
    private void riftborn$checkBeforeFlightPacket(CallbackInfo ci) {
        if (riftborn$onServerThread()) RiftFlight.refresh(player);
    }
    @Inject(method = {"onClickSlot", "onCreativeInventoryAction", "onPlayerInteractItem", "onPlayerAction"}, at = @At("RETURN"))
    private void riftborn$checkAfterInventoryPacket(CallbackInfo ci) {
        if (riftborn$onServerThread()) RiftFlight.refresh(player);
    }
    @Inject(method = "onUpdatePlayerAbilities", at = @At("RETURN"))
    private void riftborn$correctUnauthorizedFlight(CallbackInfo ci) {
        if (riftborn$onServerThread() && !player.getAbilities().allowFlying) player.sendAbilitiesUpdate();
    }
}
