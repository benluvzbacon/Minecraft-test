package dev.riftborn.registry;

import dev.riftborn.Riftborn;
import dev.riftborn.entity.RiftBoltEntity;
import dev.riftborn.entity.RiftGuardianEntity;
import dev.riftborn.entity.RiftStalkerEntity;
import dev.riftborn.entity.RiftWispEntity;
import dev.riftborn.entity.VoidBruteEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;

public final class ModEntities {
    public static final EntityType<RiftStalkerEntity> RIFT_STALKER = register("rift_stalker",
            EntityType.Builder.create(RiftStalkerEntity::new, SpawnGroup.MONSTER).dimensions(0.65f, 2.2f).maxTrackingRange(8));
    public static final EntityType<VoidBruteEntity> VOID_BRUTE = register("void_brute",
            EntityType.Builder.create(VoidBruteEntity::new, SpawnGroup.MONSTER).dimensions(1.3f, 3.1f).maxTrackingRange(10));
    public static final EntityType<RiftWispEntity> RIFT_WISP = register("rift_wisp",
            EntityType.Builder.create(RiftWispEntity::new, SpawnGroup.MONSTER).dimensions(0.8f, 0.8f).maxTrackingRange(10));
    public static final EntityType<RiftGuardianEntity> RIFT_GUARDIAN = register("rift_guardian",
            EntityType.Builder.create(RiftGuardianEntity::new, SpawnGroup.MONSTER).dimensions(1.5f, 3.6f).maxTrackingRange(12).fireImmune());
    public static final EntityType<RiftBoltEntity> RIFT_BOLT = register("rift_bolt",
            EntityType.Builder.<RiftBoltEntity>create(RiftBoltEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f).maxTrackingRange(8).trackingTickInterval(1));

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        return Registry.register(Registries.ENTITY_TYPE, Riftborn.id(name), builder.build(Riftborn.id(name).toString()));
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(RIFT_STALKER, RiftStalkerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(VOID_BRUTE, VoidBruteEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(RIFT_WISP, RiftWispEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(RIFT_GUARDIAN, RiftGuardianEntity.createAttributes());
        SpawnRestriction.register(RIFT_STALKER, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(VOID_BRUTE, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        // Ground spawn keeps wisps near islands; their AI takes flight immediately.
        SpawnRestriction.register(RIFT_WISP, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        // The boss is altar-only: it is deliberately absent from biome spawn pools.
        SpawnRestriction.register(RIFT_GUARDIAN, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
    }
    private ModEntities() { }
}
