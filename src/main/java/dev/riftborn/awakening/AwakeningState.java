package dev.riftborn.awakening;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Saved separately from the original travel state; old player return points are untouched. */
public final class AwakeningState extends PersistentState {
    public static final int GUARDIAN=1, ABYSS=2, COLOSSUS=4, ARCHITECT=8, SOVEREIGN=16, TEMPLE=32, DISCOVERY=64, COLLAPSE=128;
    public static final Type<AwakeningState> TYPE = new Type<>(AwakeningState::new, AwakeningState::read, null);
    public static final class PlayerData {
        public int flags;
        public long dashReady, aegisReady, wardReady;
        public GlobalPos origin;
    }
    public static final class EventData {
        public int kind; // 0 quiet, 1 storm, 2 collapse
        public long ends, nextStorm, nextCollapse, nextWave;
        public BlockPos center = BlockPos.ORIGIN;
        public UUID herald;
    }
    private final Map<UUID,PlayerData> players = new HashMap<>();
    private final Map<RegistryKey<World>,EventData> events = new HashMap<>();
    public boolean awakened;
    public BlockPos abyssLanding;
    public PlayerData player(UUID id) { return players.computeIfAbsent(id, key -> new PlayerData()); }
    public EventData event(RegistryKey<World> world) { return events.computeIfAbsent(world, key -> new EventData()); }
    public boolean has(UUID id, int flag) { return (player(id).flags & flag) == flag; }
    public void unlock(UUID id, int flag) { player(id).flags |= flag; markDirty(); }
    public static AwakeningState get(MinecraftServer server) { return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "riftborn_awakening"); }
    public static AwakeningState read(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        var state = new AwakeningState(); state.awakened=nbt.getBoolean("Awakened");
        if(nbt.contains("Landing")) state.abyssLanding=BlockPos.fromLong(nbt.getLong("Landing"));
        var entries=nbt.getList("Players",10);
        for(int i=0;i<entries.size();i++) {
            var row=entries.getCompound(i); if(!row.containsUuid("Id"))continue;
            var p=state.player(row.getUuid("Id")); p.flags=row.getInt("Flags");
            p.dashReady=row.getLong("Dash");p.aegisReady=row.getLong("Aegis");p.wardReady=row.getLong("Ward");
            Identifier dimension=Identifier.tryParse(row.getString("OriginWorld"));
            if(dimension!=null && row.contains("OriginPos")) p.origin=GlobalPos.create(RegistryKey.of(RegistryKeys.WORLD, dimension),BlockPos.fromLong(row.getLong("OriginPos")));
        }
        entries=nbt.getList("Events",10);
        for(int i=0;i<entries.size();i++) {
            var row=entries.getCompound(i); Identifier id=Identifier.tryParse(row.getString("World")); if(id==null)continue;
            var e=state.event(RegistryKey.of(RegistryKeys.WORLD,id)); e.kind=Math.clamp(row.getInt("Kind"),0,2);
            e.ends=row.getLong("Ends"); e.nextStorm=row.getLong("Storm");e.nextCollapse=row.getLong("Collapse");e.nextWave=row.getLong("Wave");
            e.center=BlockPos.fromLong(row.getLong("Center")); if(row.containsUuid("Herald"))e.herald=row.getUuid("Herald");
        }
        return state;
    }
    @Override public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putBoolean("Awakened",awakened); if(abyssLanding!=null)nbt.putLong("Landing",abyssLanding.asLong());
        var list=new NbtList();players.forEach((id,p)->{var row=new NbtCompound();row.putUuid("Id",id);row.putInt("Flags",p.flags);
            row.putLong("Dash",p.dashReady);row.putLong("Aegis",p.aegisReady);row.putLong("Ward",p.wardReady);
            if(p.origin!=null){row.putString("OriginWorld",p.origin.dimension().getValue().toString());row.putLong("OriginPos",p.origin.pos().asLong());}list.add(row);});nbt.put("Players",list);
        var eventList=new NbtList();events.forEach((world,e)->{var row=new NbtCompound();row.putString("World",world.getValue().toString());
            row.putInt("Kind",e.kind);row.putLong("Ends",e.ends);row.putLong("Storm",e.nextStorm);row.putLong("Collapse",e.nextCollapse);row.putLong("Wave",e.nextWave);
            row.putLong("Center",e.center.asLong());if(e.herald!=null)row.putUuid("Herald",e.herald);eventList.add(row);});nbt.put("Events",eventList);return nbt;
    }
}
