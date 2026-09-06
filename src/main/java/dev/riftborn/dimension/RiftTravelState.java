package dev.riftborn.dimension;

import dev.riftborn.Riftborn;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-save, per-player return anchors survive disconnects, deaths, and server restarts. */
public final class RiftTravelState extends PersistentState {
    private static final Type<RiftTravelState> TYPE = new Type<>(RiftTravelState::new, RiftTravelState::fromNbt, null);
    private final Map<UUID, GlobalPos> origins = new HashMap<>();
    @Nullable private BlockPos landing;

    public static RiftTravelState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "riftborn_travel");
    }
    @Nullable public GlobalPos getOrigin(UUID uuid) { return origins.get(uuid); }
    public void setOrigin(UUID uuid, GlobalPos pos) { origins.put(uuid, pos); markDirty(); }
    @Nullable public BlockPos getLanding() { return landing; }
    public void setLanding(BlockPos pos) { landing = pos.toImmutable(); markDirty(); }

    public static RiftTravelState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        RiftTravelState state = new RiftTravelState();
        NbtList entries = nbt.getList("Origins", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < entries.size(); i++) {
            NbtCompound entry = entries.getCompound(i);
            Identifier dimension = Identifier.tryParse(entry.getString("Dimension"));
            if (entry.containsUuid("Player") && dimension != null) {
                state.origins.put(entry.getUuid("Player"), GlobalPos.create(
                        RegistryKey.of(RegistryKeys.WORLD, dimension), BlockPos.fromLong(entry.getLong("Pos"))));
            } else Riftborn.LOGGER.warn("Skipping invalid Rift return-anchor entry");
        }
        if (nbt.contains("Landing", NbtElement.LONG_TYPE)) state.landing = BlockPos.fromLong(nbt.getLong("Landing"));
        return state;
    }

    @Override public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList entries = new NbtList();
        origins.forEach((uuid, pos) -> {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Player", uuid);
            entry.putString("Dimension", pos.dimension().getValue().toString());
            entry.putLong("Pos", pos.pos().asLong());
            entries.add(entry);
        });
        nbt.put("Origins", entries);
        if (landing != null) nbt.putLong("Landing", landing.asLong());
        return nbt;
    }
}
