package dev.riftborn.awakening;
import dev.riftborn.Riftborn;
import dev.riftborn.awakening.entity.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.*;
import net.minecraft.world.Heightmap;
public final class AbyssEntities {
    public static final EntityType<AbyssStalkerEntity> STALKER = register("abyss_stalker",
        EntityType.Builder.create(AbyssStalkerEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.95f, 1.5f)
            .maxTrackingRange(9));
    public static final EntityType<VoidReaverEntity> REAVER = register("void_reaver",
        EntityType.Builder.create(VoidReaverEntity::new, SpawnGroup.MONSTER)
            .dimensions(1.2f, 1.1f)
            .maxTrackingRange(10));
    public static final EntityType<AbyssalBruteEntity> BRUTE = register("abyssal_brute",
        EntityType.Builder.create(AbyssalBruteEntity::new, SpawnGroup.MONSTER)
            .dimensions(1.8f, 4.2f)
            .maxTrackingRange(11));
    public static final EntityType<RiftEchoEntity> ECHO = register("rift_echo",
        EntityType.Builder.create(RiftEchoEntity::new, SpawnGroup.MONSTER).dimensions(0.7f, 2.3f).maxTrackingRange(10));
    public static final EntityType<AbyssalWardenEntity> WARDEN = register("abyssal_warden",
        EntityType.Builder.create(AbyssalWardenEntity::new, SpawnGroup.MONSTER)
            .dimensions(1, 3.2f)
            .maxTrackingRange(11));
    public static final EntityType<AbyssalColossusEntity> COLOSSUS = register("abyssal_colossus",
        EntityType.Builder.create(AbyssalColossusEntity::new, SpawnGroup.MONSTER)
            .dimensions(3.8f, 6.6f)
            .maxTrackingRange(14)
            .makeFireImmune());
    public static final EntityType<RiftArchitectEntity> ARCHITECT = register("rift_architect",
        EntityType.Builder.create(RiftArchitectEntity::new, SpawnGroup.MONSTER)
            .dimensions(1.6f, 3.8f)
            .maxTrackingRange(14)
            .makeFireImmune());
    public static final EntityType<AbyssSovereignEntity> SOVEREIGN = register("abyss_sovereign",
        EntityType.Builder.create(AbyssSovereignEntity::new, SpawnGroup.MONSTER)
            .dimensions(2.4f, 5)
            .maxTrackingRange(14)
            .makeFireImmune());
    public static final EntityType<CollapseHeraldEntity> HERALD = register("collapse_herald",
        EntityType.Builder.create(CollapseHeraldEntity::new, SpawnGroup.MONSTER)
            .dimensions(1.3f, 3.2f)
            .maxTrackingRange(12)
            .makeFireImmune());
    public static final EntityType<AbyssBoltEntity> BOLT = register("abyss_bolt",
        EntityType.Builder.<AbyssBoltEntity>create(AbyssBoltEntity::new, SpawnGroup.MISC)
            .dimensions(0.3f, 0.3f)
            .maxTrackingRange(10)
            .trackingTickInterval(2));
    private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> builder) {
        return Registry.register(Registries.ENTITY_TYPE, Riftborn.id(id), builder.build());
    }
    public static void initialize() {
        FabricDefaultAttributeRegistry.register(STALKER, AbyssStalkerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(REAVER, VoidReaverEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BRUTE, AbyssalBruteEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ECHO, RiftEchoEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WARDEN, AbyssalWardenEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(COLOSSUS, AbyssalColossusEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ARCHITECT, RiftArchitectEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SOVEREIGN, AbyssSovereignEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HERALD, CollapseHeraldEntity.createAttributes());
        ground(STALKER);
        ground(REAVER);
        ground(BRUTE);
        ground(ECHO);
        ground(WARDEN);
        ground(COLOSSUS);
        ground(ARCHITECT);
        ground(SOVEREIGN);
        ground(HERALD);
    }
    private static <T extends HostileEntity> void ground(EntityType<T> t) {
        SpawnRestriction.register(
            t, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
    }
    private AbyssEntities() {}
}
