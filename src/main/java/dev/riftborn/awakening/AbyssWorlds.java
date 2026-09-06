package dev.riftborn.awakening;
import dev.riftborn.Riftborn;
import dev.riftborn.dimension.RiftDimensions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;
public final class AbyssWorlds {
    public static final RegistryKey<World> ABYSS = RegistryKey.of(RegistryKeys.WORLD, Riftborn.id("the_abyss"));
    public static final RegistryKey<Biome> BIOME = RegistryKey.of(RegistryKeys.BIOME, Riftborn.id("abyssal_expanse"));
    public static TagKey<Structure> sites(String name) {
        return TagKey.of(RegistryKeys.STRUCTURE, Riftborn.id("awakening/" + name));
    }
    public static boolean isRealm(World world) {
        return world.getRegistryKey().equals(ABYSS) || world.getRegistryKey().equals(RiftDimensions.WORLD);
    }
    private AbyssWorlds() {}
}
