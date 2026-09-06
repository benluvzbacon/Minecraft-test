package dev.riftborn.awakening;
import dev.riftborn.Riftborn;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import java.util.*;
public final class AbyssGear {
    public static final RegistryEntry<ArmorMaterial> MATERIAL =
        Registry.registerReference(Registries.ARMOR_MATERIAL, Riftborn.id("abyssal"),
            new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 4, ArmorItem.Type.CHESTPLATE, 9, ArmorItem.Type.LEGGINGS, 7,
                                  ArmorItem.Type.BOOTS, 4, ArmorItem.Type.BODY, 0),
                18, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
                ()
                    -> Ingredient.ofItems(AbyssItems.ABYSSAL_CRYSTAL),
                List.of(new ArmorMaterial.Layer(Riftborn.id("abyssal"))), 4, 0.1f));
    public static boolean fullSet(PlayerEntity p) {
        return p.getEquippedStack(EquipmentSlot.HEAD).isOf(AbyssItems.ABYSSAL_HELMET)
            && p.getEquippedStack(EquipmentSlot.CHEST).isOf(AbyssItems.ABYSSAL_CHESTPLATE)
            && p.getEquippedStack(EquipmentSlot.LEGS).isOf(AbyssItems.ABYSSAL_LEGGINGS)
            && p.getEquippedStack(EquipmentSlot.FEET).isOf(AbyssItems.ABYSSAL_BOOTS);
    }
    public static boolean artifact(PlayerEntity p, Item item) {
        return p.getOffHandStack().isOf(item);
    }
    public static float flightSpeed(PlayerEntity p) {
        float speed = fullSet(p) ? Riftborn.CONFIG.abyssalFlightSpeed : Riftborn.CONFIG.riftFlightSpeed;
        return Math.min(0.5f, speed * (artifact(p, AbyssItems.VOID_CORE) ? 1.12f : 1));
    }
    public static int cooldown(PlayerEntity p, int base) {
        return Math.max(5, Math.round(base * (artifact(p, AbyssItems.HEART_OF_THE_RIFT) ? 0.8f : 1)));
    }
    public static boolean creative(PlayerEntity p) {
        return p instanceof net.minecraft.server.network.ServerPlayerEntity s
            ? s.interactionManager.getGameMode() == net.minecraft.world.GameMode.CREATIVE
            : p.isCreative();
    }
    public static boolean consume(PlayerEntity p, Item item, int count) {
        if (creative(p))
            return true;
        int total = 0;
        for (int i = 0; i < p.getInventory().size(); i++)
            if (p.getInventory().getStack(i).isOf(item))
                total += p.getInventory().getStack(i).getCount();
        if (total < count)
            return false;
        for (int i = 0; i < p.getInventory().size() && count > 0; i++) {
            var s = p.getInventory().getStack(i);
            if (s.isOf(item)) {
                int n = Math.min(count, s.getCount());
                s.decrement(n);
                count -= n;
            }
        }
        return true;
    }
    public static void syncCooldowns(net.minecraft.server.network.ServerPlayerEntity p) {
        var data = AwakeningState.get(p.getServer()).player(p.getUuid());
        long now = p.getServer().getOverworld().getTime();
        if (data.dashReady > now)
            p.getItemCooldownManager().set(AbyssItems.ABYSSAL_BOOTS, (int) Math.min(1200, data.dashReady - now));
        if (data.aegisReady > now)
            p.getItemCooldownManager().set(AbyssItems.ABYSSAL_CHESTPLATE, (int) Math.min(200, data.aegisReady - now));
    }
    public static void initialize() {}
    private AbyssGear() {}
}
