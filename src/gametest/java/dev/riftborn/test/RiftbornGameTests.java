package dev.riftborn.test;

import dev.riftborn.Riftborn;
import dev.riftborn.dimension.RiftDimensions;
import dev.riftborn.dimension.RiftTravelState;
import dev.riftborn.effect.SafeTeleport;
import dev.riftborn.entity.RiftBoltEntity;
import dev.riftborn.entity.RiftGuardianEntity;
import dev.riftborn.entity.RiftStalkerEntity;
import dev.riftborn.registry.ModBlocks;
import dev.riftborn.registry.ModEntities;
import dev.riftborn.registry.ModItems;
import dev.riftborn.world.RiftWorldgen;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public final class RiftbornGameTests implements FabricGameTest {
    private static final String ARENA = "riftborn_test:arena";

    private static PlayerEntity player(TestContext context) {
        PlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);
        player.setPosition(context.getAbsolute(new Vec3d(2.5, 1, 7.5)));
        return player;
    }
    private static void wall(TestContext context, Block material, int yMin, int yMax) {
        for (int y = yMin; y <= yMax; y++) for (int z = 0; z < 16; z++) context.setBlockState(6, y, z, material);
    }

    @GameTest(templateName = ARENA) public void blinkTraversesClearSpace(TestContext context) {
        PlayerEntity player = player(context);
        var landing = SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8);
        context.assertTrue(landing.isPresent(), "Clear floor should have a landing");
        context.assertTrue(landing.get().x - player.getX() > 7.7, "Blink should reach its full range");
        context.complete();
    }
    @GameTest(templateName = ARENA) public void blinkStopsBeforeSolidWalls(TestContext context) {
        PlayerEntity player = player(context);
        wall(context, Blocks.STONE, 1, 3);
        var landing = SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8);
        context.assertTrue(landing.isPresent(), "Should still land before the wall");
        context.assertTrue(context.getRelative(landing.get()).x < 5.71, "Player AABB may not cross the wall");
        context.complete();
    }
    @GameTest(templateName = ARENA) public void blinkStopsAtThinPanes(TestContext context) {
        PlayerEntity player = player(context);
        wall(context, Blocks.GLASS_PANE, 1, 3);
        var landing = SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8);
        context.assertTrue(landing.isPresent(), "Should land before glass");
        context.assertTrue(context.getRelative(landing.get()).x < 6.2, "Swept collision must not tunnel through panes");
        context.complete();
    }
    @GameTest(templateName = ARENA) public void blinkRespectsHeadClearance(TestContext context) {
        PlayerEntity player = player(context);
        wall(context, Blocks.STONE, 2, 2);
        var landing = SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8);
        context.assertTrue(landing.isPresent(), "Should land before low ceiling");
        context.assertTrue(context.getRelative(landing.get()).x < 5.71, "Cannot place the player's head in stone");
        context.complete();
    }
    @GameTest(templateName = ARENA) public void blinkRefusesVoidLanding(TestContext context) {
        PlayerEntity player = player(context);
        for (int x = 3; x < 16; x++) for (int z = 0; z < 16; z++) context.setBlockState(x, 0, z, Blocks.AIR);
        context.assertTrue(SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8).isEmpty(), "No floor means no blink");
        context.complete();
    }
    @GameTest(templateName = ARENA) public void blinkStopsBeforeFluids(TestContext context) {
        PlayerEntity player = player(context);
        wall(context, Blocks.LAVA, 1, 2);
        var landing = SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8);
        context.assertTrue(landing.isPresent(), "Should have a landing before lava");
        context.assertTrue(context.getRelative(landing.get()).x < 5.71, "Blink must not place players in lava");
        context.complete();
    }

    // Vanilla's 1.21.1 GameTest API has no replacement for this server-player factory.
    @SuppressWarnings("removal")
    @GameTest(templateName = ARENA) public void bladeCooldownAndDurabilityAreServerSide(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        player.setPosition(context.getAbsolute(new Vec3d(2.5, 1, 7.5)));
        player.setYaw(-90);
        ItemStack blade = new ItemStack(ModItems.RIFTBLADE);
        player.setStackInHand(Hand.MAIN_HAND, blade);
        double oldX = player.getX();
        ModItems.RIFTBLADE.use(context.getWorld(), player, Hand.MAIN_HAND);
        context.assertTrue(player.getX() > oldX + 7.7, "Server must actually move the player");
        context.assertTrue(player.getItemCooldownManager().isCoolingDown(ModItems.RIFTBLADE), "Server cooldown must be active");
        context.assertEquals(blade.getDamage(), 2, "Ability durability cost");
        Vec3d after = player.getPos();
        ModItems.RIFTBLADE.use(context.getWorld(), player, Hand.MAIN_HAND);
        context.assertEquals(player.getPos(), after, "A second use during cooldown cannot move the player");
        player.discard();
        context.complete();
    }

    @GameTest(templateName = ARENA) public void materialsCraftThroughVanillaRecipeManager(TestContext context) {
        var world = context.getWorld();
        CraftingRecipeInput input = CraftingRecipeInput.create(3, 3, List.of(
                new ItemStack(ModBlocks.RIFT_STONE), new ItemStack(ModItems.RIFT_SHARD), new ItemStack(ModBlocks.RIFT_STONE),
                new ItemStack(ModItems.RIFT_SHARD), new ItemStack(Items.ENDER_PEARL), new ItemStack(ModItems.RIFT_SHARD),
                new ItemStack(ModBlocks.RIFT_STONE), new ItemStack(ModItems.RIFT_SHARD), new ItemStack(ModBlocks.RIFT_STONE)));
        var coreRecipe = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, input, world);
        context.assertTrue(coreRecipe.isPresent(), "Rift Core recipe must load and match");
        context.assertTrue(coreRecipe.get().value().craft(input, world.getRegistryManager()).isOf(ModItems.RIFT_CORE), "Rift Core recipe result");
        for (String id : List.of("riftblade", "rift_compass", "rift_shard_from_smelting", "rift_shard_from_blasting", "rift_dust_from_blooms", "rift_shard_from_dust")) {
            context.assertTrue(world.getRecipeManager().get(Riftborn.id(id)).isPresent(), "Recipe missing: " + id);
        }
        context.complete();
    }

    @GameTest(templateName = ARENA) public void mobAttributesAndDimensionLoad(TestContext context) {
        var stalker = ModEntities.RIFT_STALKER.create(context.getWorld());
        var brute = ModEntities.VOID_BRUTE.create(context.getWorld());
        var wisp = ModEntities.RIFT_WISP.create(context.getWorld());
        var guardian = ModEntities.RIFT_GUARDIAN.create(context.getWorld());
        context.assertEquals(stalker.getMaxHealth(), 28.0f, "Stalker health");
        context.assertEquals(brute.getMaxHealth(), 100.0f, "Brute health");
        context.assertTrue(brute.getAttributeValue(EntityAttributes.GENERIC_ATTACK_KNOCKBACK) >= 2, "Brute knockback");
        context.assertTrue(wisp.hasNoGravity(), "Wisps fly");
        context.assertEquals(guardian.getMaxHealth(), 280.0f, "Guardian health");
        context.assertTrue(context.getWorld().getServer().getWorld(RiftDimensions.WORLD) != null, "The Rift dimension must load on a dedicated server");
        context.complete();
    }

    @GameTest(templateName = ARENA) public void returnAnchorsSurviveSaveReload(TestContext context) {
        UUID player = new UUID(0, 1);
        GlobalPos origin = GlobalPos.create(World.OVERWORLD, new BlockPos(10, 80, 20));
        RiftTravelState before = new RiftTravelState();
        before.setOrigin(player, origin);
        before.setLanding(new BlockPos(0, 72, 0));
        var lookup = context.getWorld().getRegistryManager();
        RiftTravelState after = RiftTravelState.fromNbt(before.writeNbt(new NbtCompound(), lookup), lookup);
        context.assertEquals(after.getOrigin(player), origin, "Return anchor should persist per player");
        context.assertEquals(after.getLanding(), before.getLanding(), "Arrival platform must not be rebuilt on every visit");
        context.complete();
    }

    @GameTest(templateName = ARENA, tickLimit = 60) public void projectilesDamageTargetsAndDisappear(TestContext context) {
        RiftStalkerEntity owner = context.spawnMob(ModEntities.RIFT_STALKER, 2, 1, 7);
        owner.setAiDisabled(true);
        CowEntity cow = context.spawnMob(EntityType.COW, 9, 1, 7);
        cow.setAiDisabled(true);
        RiftBoltEntity bolt = new RiftBoltEntity(context.getWorld(), owner, 5);
        bolt.setPosition(context.getAbsolute(new Vec3d(3.5, 1.5, 7.5)));
        bolt.setVelocity(1, 0, 0, 0.6f, 0);
        context.getWorld().spawnEntity(bolt);
        context.waitAndRun(25, () -> {
            context.assertTrue(cow.getHealth() < cow.getMaxHealth(), "Energy bolt must damage its target");
            context.assertTrue(bolt.isRemoved(), "Energy bolt must be discarded after impact");
            context.complete();
        });
    }

    @GameTest(templateName = ARENA, tickLimit = 40) public void guardianEntersSecondPhase(TestContext context) {
        RiftGuardianEntity boss = context.spawnMob(ModEntities.RIFT_GUARDIAN, 8, 1, 8);
        boss.setHealth(130);
        context.waitAndRun(10, () -> {
            context.assertTrue(boss.isEnraged(), "Guardian should enter phase two below half health");
            context.assertTrue(boss.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) > 0.25, "Phase two should increase movement speed");
            boss.discard();
            context.complete();
        });
    }

    @GameTest(templateName = ARENA, tickLimit = 40) public void guardianDropsHeartAndDismissesWisps(TestContext context) {
        RiftGuardianEntity boss = context.spawnMob(ModEntities.RIFT_GUARDIAN, 8, 1, 8);
        boss.setAiDisabled(true);
        var wisp = context.spawnMob(ModEntities.RIFT_WISP, 10, 3, 8);
        wisp.setAiDisabled(true);
        wisp.setSummoner(boss.getUuid());
        boss.damage(context.getWorld().getDamageSources().genericKill(), 1000);
        context.waitAndRun(3, () -> {
            context.expectItemAt(ModItems.RIFT_HEART, new BlockPos(8, 1, 8), 4);
            context.assertTrue(wisp.isRemoved(), "Guardian death must dismiss its summoned Wisps");
            context.complete();
        });
    }

    @GameTest(templateName = ARENA, tickLimit = 200) public void vanillaWorldgenCanLocateGuardianShrine(TestContext context) {
        ServerWorld rift = context.getWorld().getServer().getWorld(RiftDimensions.WORLD);
        context.assertTrue(rift != null, "Rift world must exist");
        BlockPos shrine = rift.locateStructure(RiftWorldgen.GUARDIAN_SHRINES, new BlockPos(0, 72, 0), 24, false);
        context.assertTrue(shrine != null, "Normal structure placement must find a Guardian shrine");
        context.assertTrue(shrine.getY() >= 32, "Shrines must not generate on the void floor");
        context.complete();
    }
}
