package dev.riftborn.test;

import dev.riftborn.Riftborn;
import dev.riftborn.dimension.RiftDimensions;
import dev.riftborn.dimension.RiftTravelState;
import dev.riftborn.effect.SafeTeleport;
import dev.riftborn.item.RiftCompassItem;
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
import net.minecraft.registry.RegistryKeys;
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
        // NBT templates are placed one block above the GameTest control block.
        // The template's floor is therefore at relative Y=1, and feet at Y=2.
        context.assertTrue(context.getBlockState(new BlockPos(2, 1, 7)).isOf(ModBlocks.RIFT_STONE), "Test arena floor must be present");
        player.setPosition(Vec3d.ofBottomCenter(context.getAbsolutePos(new BlockPos(2, 2, 7))));
        return player;
    }
    private static void wall(TestContext context, Block material, int yMin, int yMax) {
        for (int y = yMin; y <= yMax; y++) for (int z = 0; z < 16; z++) context.setBlockState(6, y, z, material);
    }

    @GameTest(templateName = ARENA) public void blinkTraversesClearSpace(TestContext context) {
        PlayerEntity player = player(context);
        var landing = SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8);
        context.assertTrue(landing.isPresent(), "Clear floor should have a landing: pos=" + player.getPos()
                + ", below=" + context.getWorld().getBlockState(player.getBlockPos().down())
                + ", clear=" + SafeTeleport.isClear(context.getWorld(), player, player.getBoundingBox())
                + ", floor=" + SafeTeleport.hasFloor(context.getWorld(), player, player.getBoundingBox())
                + ", chunk=" + context.getWorld().getChunkManager().isChunkLoaded(player.getChunkPos().x, player.getChunkPos().z));
        context.assertTrue(landing.get().x - player.getX() > 7.7, "Blink should reach its full range");
        context.complete();
    }
    @GameTest(templateName = ARENA) public void blinkStopsBeforeSolidWalls(TestContext context) {
        PlayerEntity player = player(context);
        wall(context, Blocks.STONE, 2, 4);
        var landing = SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8);
        context.assertTrue(landing.isPresent(), "Should still land before the wall");
        context.assertTrue(context.getRelative(landing.get()).x < 5.71, "Player AABB may not cross the wall");
        context.complete();
    }
    @GameTest(templateName = ARENA) public void blinkStopsAtThinPanes(TestContext context) {
        PlayerEntity player = player(context);
        wall(context, Blocks.GLASS_PANE, 2, 4);
        var landing = SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8);
        context.assertTrue(landing.isPresent(), "Should land before glass");
        context.assertTrue(context.getRelative(landing.get()).x < 6.2, "Swept collision must not tunnel through panes");
        context.complete();
    }
    @GameTest(templateName = ARENA) public void blinkRespectsHeadClearance(TestContext context) {
        PlayerEntity player = player(context);
        wall(context, Blocks.STONE, 3, 3);
        var landing = SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8);
        context.assertTrue(landing.isPresent(), "Should land before low ceiling");
        context.assertTrue(context.getRelative(landing.get()).x < 5.71, "Cannot place the player's head in stone");
        context.complete();
    }
    @GameTest(templateName = ARENA) public void blinkRefusesVoidLanding(TestContext context) {
        PlayerEntity player = player(context);
        for (int x = 3; x < 16; x++) for (int z = 0; z < 16; z++) context.setBlockState(x, 1, z, Blocks.AIR);
        context.assertTrue(SafeTeleport.findBlinkDestination(context.getWorld(), player, new Vec3d(1, 0, 0), 8).isEmpty(), "No floor means no blink");
        context.complete();
    }
    @GameTest(templateName = ARENA) public void blinkStopsBeforeFluids(TestContext context) {
        PlayerEntity player = player(context);
        wall(context, Blocks.LAVA, 2, 3);
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
        // NBT templates are placed one block above the GameTest control block.
        // The template's floor is therefore at relative Y=1, and feet at Y=2.
        context.assertTrue(context.getBlockState(new BlockPos(2, 1, 7)).isOf(ModBlocks.RIFT_STONE), "Test arena floor must be present");
        player.setPosition(Vec3d.ofBottomCenter(context.getAbsolutePos(new BlockPos(2, 2, 7))));
        player.setYaw(-90);
        ItemStack blade = new ItemStack(ModItems.RIFTBLADE);
        player.setStackInHand(Hand.MAIN_HAND, blade);
        double oldX = player.getX();
        var result = ModItems.RIFTBLADE.use(context.getWorld(), player, Hand.MAIN_HAND);
        context.assertTrue(player.getX() > oldX + 7.7, "Server must actually move the player: result=" + result.getResult()
                + ", beforeX=" + oldX + ", after=" + player.getPos() + ", yaw=" + player.getYaw());
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
        // TestServer constructs only vanilla worlds. The normal dedicated-server
        // multiplayer harness separately verifies the actual custom world transition.
        context.assertTrue(context.getWorld().getRegistryManager().get(RegistryKeys.DIMENSION_TYPE)
                .containsId(Riftborn.id("the_rift")), "Rift dimension type must decode and register");
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
        RiftStalkerEntity owner = context.spawnMob(ModEntities.RIFT_STALKER, 2, 2, 7);
        owner.setAiDisabled(true);
        CowEntity cow = context.spawnMob(EntityType.COW, 9, 2, 7);
        cow.setAiDisabled(true);
        RiftBoltEntity bolt = new RiftBoltEntity(context.getWorld(), owner, 5);
        bolt.setPosition(Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(3, 2, 7))));
        bolt.setVelocity(1, 0, 0, 0.6f, 0);
        context.getWorld().spawnEntity(bolt);
        context.waitAndRun(25, () -> {
            context.assertTrue(cow.getHealth() < cow.getMaxHealth(), "Energy bolt must damage its target");
            context.assertTrue(bolt.isRemoved(), "Energy bolt must be discarded after impact");
            context.complete();
        });
    }

    @GameTest(templateName = ARENA, tickLimit = 40) public void guardianEntersSecondPhase(TestContext context) {
        RiftGuardianEntity boss = context.spawnMob(ModEntities.RIFT_GUARDIAN, 8, 2, 8);
        boss.setHealth(130);
        context.waitAndRun(10, () -> {
            context.assertTrue(boss.isEnraged(), "Guardian should enter phase two below half health");
            context.assertTrue(boss.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) > 0.25, "Phase two should increase movement speed");
            boss.discard();
            context.complete();
        });
    }

    @GameTest(templateName = ARENA, tickLimit = 40) public void guardianDropsHeartAndDismissesWisps(TestContext context) {
        RiftGuardianEntity boss = context.spawnMob(ModEntities.RIFT_GUARDIAN, 8, 2, 8);
        boss.setAiDisabled(true);
        var wisp = context.spawnMob(ModEntities.RIFT_WISP, 10, 3, 8);
        wisp.setAiDisabled(true);
        wisp.setSummoner(boss.getUuid());
        boss.damage(context.getWorld().getDamageSources().genericKill(), 1000);
        context.waitAndRun(3, () -> {
            context.expectItemAt(ModItems.RIFT_HEART, new BlockPos(8, 2, 8), 4);
            context.assertTrue(wisp.isRemoved(), "Guardian death must dismiss its summoned Wisps");
            context.complete();
        });
    }

    @GameTest(templateName = ARENA) public void structureTemplatesAndDynamicRegistriesLoad(TestContext context) {
        var registry = context.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);
        for (String name : List.of("overworld_ruin", "rift_ruin", "guardian_shrine")) {
            context.assertTrue(registry.containsId(Riftborn.id(name)), "Structure codec must load: " + name);
            var template = context.getWorld().getStructureTemplateManager().getTemplate(Riftborn.id(name));
            context.assertTrue(template.isPresent(), "NBT template must load: " + name);
            context.assertTrue(template.get().getSize().getX() >= 17, "Template must have the expected dimensions");
        }
        context.complete();
    }

    @GameTest(templateName = ARENA, tickLimit = 40) public void compassFindsRealNearbyAnchors(TestContext context) {
        BlockPos anchor = new BlockPos(6, 2, 7);
        context.setBlockState(anchor, ModBlocks.RIFT_ANCHOR);
        context.waitAndRun(5, () -> {
            BlockPos found = RiftCompassItem.locate(context.getWorld(), context.getAbsolutePos(new BlockPos(2, 2, 7)), false);
            context.assertEquals(found, context.getAbsolutePos(anchor), "Compass should track real anchors through POI storage");
            context.complete();
        });
    }

    @GameTest(templateName = ARENA) public void bruteAttacksWithHeavyKnockback(TestContext context) {
        var brute = context.spawnMob(ModEntities.VOID_BRUTE, 5, 2, 8);
        brute.setAiDisabled(true);
        var target = context.spawnMob(EntityType.ZOMBIE, 8, 2, 8);
        target.setAiDisabled(true);
        context.assertTrue(brute.tryAttack(target), "Brute melee must deal damage");
        context.assertTrue(target.getHealth() < 12, "Brute should deal substantial melee damage");
        context.assertTrue(target.getVelocity().horizontalLengthSquared() > 0.2, "Brute attack must apply knockback");
        context.complete();
    }

    @GameTest(templateName = ARENA, tickLimit = 160) public void wispFliesAndShootsFromRange(TestContext context) {
        var wisp = context.spawnMob(ModEntities.RIFT_WISP, 12, 5, 8);
        var target = context.spawnMob(EntityType.COW, 6, 2, 8);
        target.setAiDisabled(true);
        wisp.setTarget(target);
        context.waitAndRun(100, () -> {
            context.assertTrue(target.getHealth() < target.getMaxHealth(), "Wisp AI must fire damaging energy bolts");
            context.assertTrue(wisp.squaredDistanceTo(target) > 16, "Wisp should keep its distance rather than melee");
            context.assertTrue(wisp.hasNoGravity(), "Wisp should remain airborne");
            wisp.discard();
            context.complete();
        });
    }

    @GameTest(templateName = ARENA, tickLimit = 160) public void stalkerCanTeleportDuringCombat(TestContext context) {
        var stalker = context.spawnMob(ModEntities.RIFT_STALKER, 8, 2, 8);
        var target = context.spawnMob(EntityType.COW, 2, 2, 8);
        target.setAiDisabled(true);
        // Hold walking still so this specifically measures the teleport, not pathfinding.
        stalker.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
        stalker.setTarget(target);
        Vec3d before = stalker.getPos();
        context.waitAndRun(125, () -> {
            double distance = stalker.getPos().squaredDistanceTo(before);
            context.assertTrue(distance >= 16 && distance <= 40, "Stalker should make a short, collision-checked teleport");
            stalker.discard();
            context.complete();
        });
    }

}
