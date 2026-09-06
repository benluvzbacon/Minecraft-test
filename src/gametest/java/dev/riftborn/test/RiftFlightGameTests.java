package dev.riftborn.test;

import dev.riftborn.Riftborn;
import dev.riftborn.effect.RiftFlight;
import dev.riftborn.effect.SafeTeleport;
import dev.riftborn.registry.ModArmorMaterials;
import dev.riftborn.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** New Ascension tests; the original twenty gameplay regressions are retained unchanged. */
public final class RiftFlightGameTests implements FabricGameTest {
    private static final String ARENA = "riftborn_test:arena";
    private static final Map<EquipmentSlot, Item> ARMOR = Map.of(EquipmentSlot.HEAD, ModItems.RIFT_HELMET,
            EquipmentSlot.CHEST, ModItems.RIFT_CHESTPLATE, EquipmentSlot.LEGS, ModItems.RIFT_LEGGINGS, EquipmentSlot.FEET, ModItems.RIFT_BOOTS);

    @SuppressWarnings("removal")
    private static ServerPlayerEntity player(TestContext context, GameMode mode) {
        var player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(mode);
        player.setPosition(Vec3d.ofBottomCenter(context.getAbsolutePos(new BlockPos(3, 2, 7))));
        return player;
    }
    private static void equip(ServerPlayerEntity player) {
        ARMOR.forEach((slot, item) -> player.equipStack(slot, new ItemStack(item)));
        RiftFlight.refresh(player);
    }
    private static void dispose(ServerPlayerEntity player) {
        ARMOR.keySet().forEach(slot -> player.equipStack(slot, ItemStack.EMPTY));
        RiftFlight.refresh(player);
        player.discard();
    }
    private static void noFlight(TestContext context, ServerPlayerEntity player) {
        context.assertFalse(player.getAbilities().allowFlying, "Incomplete/dead Survival player must not have mayfly");
        context.assertFalse(player.getAbilities().flying, "Active flight must stop when entitlement disappears");
        context.assertFalse(RiftFlight.ownsFlight(player), "No stale Rift flight grant may remain");
        context.assertTrue(Math.abs(player.getAbilities().getFlySpeed() - new PlayerAbilities().getFlySpeed()) < 0.00001,
                "Normal flight speed must be restored");
    }
    private static void remove(TestContext context, EquipmentSlot slot) {
        var player = player(context, GameMode.SURVIVAL);
        equip(player);
        player.getAbilities().flying = true;
        player.equipStack(slot, ItemStack.EMPTY);
        RiftFlight.refresh(player);
        noFlight(context, player);
        dispose(player); context.complete();
    }

    @GameTest(templateName = ARENA) public void fullSetGrantsFastFlightWithoutCreative(TestContext context) {
        var player = player(context, GameMode.SURVIVAL);
        equip(player);
        context.assertTrue(player.getAbilities().allowFlying && RiftFlight.ownsFlight(player), "Full set must grant server flight");
        context.assertFalse(player.getAbilities().flying, "Equipping should not force flight on; normal double-jump controls apply");
        context.assertTrue(player.getAbilities().getFlySpeed() >= new PlayerAbilities().getFlySpeed() * 2.99f, "Rift Flight must be 3x normal Creative speed");
        context.assertEquals(player.interactionManager.getGameMode(), GameMode.SURVIVAL, "No game-mode change");
        context.assertFalse(player.getAbilities().invulnerable, "The bonus must not grant Creative invulnerability");
        dispose(player); context.complete();
    }
    @GameTest(templateName = ARENA) public void partialSetNeverGrantsFlight(TestContext context) {
        var player = player(context, GameMode.SURVIVAL);
        player.equipStack(EquipmentSlot.HEAD, new ItemStack(ModItems.RIFT_HELMET));
        player.equipStack(EquipmentSlot.CHEST, new ItemStack(ModItems.RIFT_CHESTPLATE));
        player.equipStack(EquipmentSlot.LEGS, new ItemStack(ModItems.RIFT_LEGGINGS));
        player.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        RiftFlight.refresh(player); noFlight(context, player);
        dispose(player); context.complete();
    }
    @GameTest(templateName = ARENA) public void removingHelmetRevokesFlight(TestContext context) { remove(context, EquipmentSlot.HEAD); }
    @GameTest(templateName = ARENA) public void removingChestplateRevokesFlight(TestContext context) { remove(context, EquipmentSlot.CHEST); }
    @GameTest(templateName = ARENA) public void removingLeggingsRevokesFlight(TestContext context) { remove(context, EquipmentSlot.LEGS); }
    @GameTest(templateName = ARENA) public void removingBootsRevokesFlight(TestContext context) { remove(context, EquipmentSlot.FEET); }
    @GameTest(templateName = ARENA) public void brokenArmorRevokesFlight(TestContext context) {
        var player = player(context, GameMode.SURVIVAL); equip(player); player.getAbilities().flying = true;
        player.getEquippedStack(EquipmentSlot.CHEST).setCount(0);
        RiftFlight.refresh(player); noFlight(context, player);
        dispose(player); context.complete();
    }
    @GameTest(templateName = ARENA) public void adventureFlightKeepsAdventureRestrictions(TestContext context) {
        var player = player(context, GameMode.ADVENTURE); equip(player);
        context.assertTrue(player.getAbilities().allowFlying, "Adventure supports Rift Flight");
        context.assertFalse(player.getAbilities().allowModifyWorld, "Adventure block restrictions remain");
        context.assertEquals(player.interactionManager.getGameMode(), GameMode.ADVENTURE, "Still Adventure");
        dispose(player); context.complete();
    }
    @GameTest(templateName = ARENA) public void creativeAndSpectatorKeepNativeFlight(TestContext context) {
        var player = player(context, GameMode.CREATIVE); equip(player); player.getAbilities().flying = true;
        player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY); RiftFlight.refresh(player);
        context.assertTrue(player.getAbilities().allowFlying && player.getAbilities().flying, "Creative flight is not armor-dependent");
        context.assertTrue(Math.abs(player.getAbilities().getFlySpeed() - 0.05f) < 0.00001, "Creative speed stays normal");
        context.assertFalse(RiftFlight.ownsFlight(player), "Riftborn must not own Creative flight");
        player.changeGameMode(GameMode.SPECTATOR); RiftFlight.refresh(player);
        context.assertTrue(player.getAbilities().allowFlying && player.getAbilities().flying, "Spectator flight stays intact");
        dispose(player); context.complete();
    }
    @GameTest(templateName = ARENA) public void modeSwitchDoesNotLeakBonusSpeedOrFlight(TestContext context) {
        var player = player(context, GameMode.SURVIVAL); equip(player); player.getAbilities().flying = true;
        player.changeGameMode(GameMode.CREATIVE);
        context.assertTrue(player.getAbilities().allowFlying, "Creative retains flight");
        context.assertTrue(Math.abs(player.getAbilities().getFlySpeed() - 0.05f) < 0.00001, "Creative does not retain the armor speed boost");
        player.changeGameMode(GameMode.SURVIVAL);
        context.assertTrue(player.getAbilities().allowFlying, "Full set re-enables Survival flight");
        player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY); RiftFlight.refresh(player);
        player.changeGameMode(GameMode.CREATIVE); player.changeGameMode(GameMode.SURVIVAL);
        noFlight(context, player); dispose(player); context.complete();
    }
    @GameTest(templateName = ARENA) public void saveAndReloadValidateArmorInsteadOfSavingPermanentFlight(TestContext context) {
        var player = player(context, GameMode.SURVIVAL); equip(player); player.getAbilities().flying = true;
        NbtCompound saved = new NbtCompound(); player.writeCustomDataToNbt(saved);
        context.assertFalse(saved.getCompound("abilities").getBoolean("mayfly"), "Player data must not persist our mayfly flag");
        context.assertFalse(saved.getCompound("abilities").getBoolean("flying"), "Player data must not persist unconditional flying");
        context.assertTrue(Math.abs(saved.getCompound("abilities").getFloat("flySpeed") - 0.05f) < 0.00001, "Player data stores baseline speed");
        context.assertTrue(player.getAbilities().flying, "Saving must not interrupt current flight");
        var rejoined = player(context, GameMode.SURVIVAL); rejoined.readCustomDataFromNbt(saved);
        RiftFlight.refresh(rejoined);
        context.assertTrue(rejoined.getAbilities().allowFlying && rejoined.getAbilities().flying, "A valid full set resumes flight on reconnect");
        dispose(rejoined);
        var inventory = saved.getList("Inventory", 10);
        for (int i = inventory.size() - 1; i >= 0; i--) if (inventory.getCompound(i).getByte("Slot") == 103) inventory.remove(i);
        var withoutHelmet = player(context, GameMode.SURVIVAL); withoutHelmet.readCustomDataFromNbt(saved);
        RiftFlight.refresh(withoutHelmet); noFlight(context, withoutHelmet);
        dispose(withoutHelmet); dispose(player); context.complete();
    }
    @GameTest(templateName = ARENA) public void deadPlayerCannotKeepFlight(TestContext context) {
        var player = player(context, GameMode.SURVIVAL); equip(player); player.getAbilities().flying = true;
        player.setHealth(0); RiftFlight.refresh(player); noFlight(context, player);
        dispose(player); context.complete();
    }
    @GameTest(templateName = ARENA) public void existingIndependentFlightIsNotConfiscated(TestContext context) {
        var player = player(context, GameMode.SURVIVAL);
        player.getAbilities().allowFlying = true; player.getAbilities().setFlySpeed(0.07f);
        equip(player); player.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY); RiftFlight.refresh(player);
        context.assertTrue(player.getAbilities().allowFlying, "An independent pre-existing flight grant is not Riftborn's to remove");
        context.assertTrue(Math.abs(player.getAbilities().getFlySpeed() - 0.07f) < 0.00001, "Independent speed is restored, not the bonus");
        dispose(player); context.complete();
    }
    @GameTest(templateName = ARENA) public void flightDoesNotBypassRiftbladeVoidSafety(TestContext context) {
        var player = player(context, GameMode.SURVIVAL); equip(player); player.getAbilities().flying = true;
        player.setPosition(player.getPos().add(0, 6, 0));
        context.assertTrue(SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8).isEmpty(),
                "Even with flight, the unchanged Riftblade requires a supported landing");
        dispose(player); context.complete();
    }
    @GameTest(templateName = ARENA) public void allArmorRecipesAndMaterialStatsWork(TestContext context) {
        var patterns = Map.of(ModItems.RIFT_HELMET, List.of("FFF", "FCF", "   "),
                ModItems.RIFT_CHESTPLATE, List.of("FCF", "FHF", "FFF"),
                ModItems.RIFT_LEGGINGS, List.of("FFF", "FCF", "F F"), ModItems.RIFT_BOOTS, List.of("F F", "FCF", "   "));
        patterns.forEach((item, pattern) -> {
            var stacks = new ArrayList<ItemStack>();
            for (String row : pattern) for (char ingredient : row.toCharArray()) stacks.add(switch (ingredient) {
                case 'F' -> new ItemStack(ModItems.VOID_FRAGMENT); case 'C' -> new ItemStack(ModItems.RIFT_CORE);
                case 'H' -> new ItemStack(ModItems.RIFT_HEART); default -> ItemStack.EMPTY;
            });
            var input = CraftingRecipeInput.create(3, 3, stacks);
            var recipe = context.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, input, context.getWorld());
            context.assertTrue(recipe.isPresent(), "Armor recipe must match: " + item);
            context.assertTrue(recipe.orElseThrow().value().craft(input, context.getWorld().getRegistryManager()).isOf(item), "Correct armor recipe result");
            var armor = (ArmorItem) item;
            context.assertEquals(new ItemStack(item).getMaxDamage(), armor.getType().getMaxDamage(45), "Armor durability");
            context.assertTrue(ModArmorMaterials.RIFT.value().getProtection(armor.getType()) >= 3, "Armor protection");
        });
        context.assertTrue(ModArmorMaterials.RIFT.value().toughness() == 3.0f, "Rift armor toughness");
        context.complete();
    }
}
