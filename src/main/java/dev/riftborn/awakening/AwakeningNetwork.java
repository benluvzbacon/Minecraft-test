package dev.riftborn.awakening;
import dev.riftborn.Riftborn;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
public final class AwakeningNetwork {
    public record Dash() implements CustomPayload {
        public static final Id<Dash> ID = new Id<>(Riftborn.id("rift_dash"));
        public static final PacketCodec<RegistryByteBuf, Dash> CODEC = PacketCodec.unit(new Dash());
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public record Event(int kind, int remaining) implements CustomPayload {
        public static final Id<Event> ID = new Id<>(Riftborn.id("realm_event"));
        public static final PacketCodec<RegistryByteBuf, Event> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, Event::kind, PacketCodecs.VAR_INT, Event::remaining, Event::new);
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(Dash.ID, Dash.CODEC);
        PayloadTypeRegistry.playS2C().register(Event.ID, Event.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
            Dash.ID, (payload, ctx) -> ctx.server().execute(() -> RiftDash.use(ctx.player())));
    }
    public static void event(ServerPlayerEntity p, int kind, int remaining) {
        if (ServerPlayNetworking.canSend(p, Event.ID))
            ServerPlayNetworking.send(p, new Event(kind, remaining));
    }
    private AwakeningNetwork() {}
}
