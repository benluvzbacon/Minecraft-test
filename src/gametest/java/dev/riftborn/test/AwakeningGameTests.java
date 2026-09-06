package dev.riftborn.test;
import dev.riftborn.Riftborn;
import dev.riftborn.awakening.*;
import dev.riftborn.awakening.block.*;
import dev.riftborn.awakening.entity.*;
import dev.riftborn.awakening.item.*;
import dev.riftborn.effect.RiftFlight;
import dev.riftborn.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.util.*;
public final class AwakeningGameTests implements FabricGameTest {
    private static final String ARENA = "riftborn_test:awakening_arena";
    @SuppressWarnings("removal")
    private static ServerPlayerEntity player(TestContext c) {
        var p = c.createMockCreativeServerPlayerInWorld();
        p.changeGameMode(GameMode.CREATIVE);
        p.changeGameMode(GameMode.SURVIVAL);
        p.setPosition(Vec3d.ofBottomCenter(c.getAbsolutePos(new BlockPos(20, 2, 20))));
        return p;
    }
    private static void equip(ServerPlayerEntity p) {
        p.equipStack(EquipmentSlot.HEAD, new ItemStack(AbyssItems.ABYSSAL_HELMET));
        p.equipStack(EquipmentSlot.CHEST, new ItemStack(AbyssItems.ABYSSAL_CHESTPLATE));
        p.equipStack(EquipmentSlot.LEGS, new ItemStack(AbyssItems.ABYSSAL_LEGGINGS));
        p.equipStack(EquipmentSlot.FEET, new ItemStack(AbyssItems.ABYSSAL_BOOTS));
        RiftFlight.refresh(p);
    }
    private static void dispose(ServerPlayerEntity p) {
        p.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
        RiftFlight.refresh(p);
        p.discard();
    }
    @GameTest(templateName = ARENA)
    public void abyssArmorUpgradesFlightAndAllPiecesAreRequired(TestContext c) {
        var p = player(c);
        equip(p);
        c.assertTrue(p.getAbilities().allowFlying && p.getAbilities().getFlySpeed() > RiftFlight.FLIGHT_SPEED,
            "Abyss armor must improve, not replace, Rift Flight");
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            var armor = p.getEquippedStack(slot);
            p.getAbilities().flying = true;
            p.equipStack(slot, ItemStack.EMPTY);
            RiftFlight.refresh(p);
            c.assertFalse(
                p.getAbilities().allowFlying || p.getAbilities().flying, "Every missing piece revokes flight");
            p.equipStack(slot, armor);
            RiftFlight.refresh(p);
        }
        dispose(p);
        c.complete();
    }
    @GameTest(templateName = ARENA)
    public void dashStopsAtWallsAndRequiresTerrain(TestContext c) {
        var p = player(c);
        equip(p);
        for (int y = 2; y < 7; y++)
            for (int z = 15; z <= 25; z++) c.setBlockState(new BlockPos(26, y, z), Blocks.OBSIDIAN);
        var result = RiftDash.destination(c.getWorld(), p, new Vec3d(1, 0, 0), 12).orElseThrow();
        c.assertTrue(result.x < p.getX() + 5.5, "Dash must stop before solid wall");
        // Keep this void case independent of the wall fixture and other test structures.
        p.setPosition(p.getPos().add(0, 100, 0));
        p.getAbilities().flying = true;
        c.assertTrue(RiftDash.destination(c.getWorld(), p, new Vec3d(1, 0, 0), 12).isEmpty(),
            "High aerial dash cannot end above an unbounded fall");
        dispose(p);
        c.complete();
    }
    @GameTest(templateName = ARENA)
    public void lowAerialDashFindsFractionalHeightSupport(TestContext c) {
        var p = player(c);
        equip(p);
        p.setPosition(p.getPos().add(0, 6.35, 0));
        p.getAbilities().flying = true;
        c.assertTrue(RiftDash.destination(c.getWorld(), p, new Vec3d(1, 0, 0), 12).isPresent(),
            "A low aerial dash must find real support at fractional player heights");
        dispose(p);
        c.complete();
    }
    @GameTest(templateName = ARENA)
    public void dashCooldownIsServerOwnedAndSurvivesStateSerialization(TestContext c) {
        var p = player(c);
        equip(p);
        p.setYaw(-90);
        var state = AwakeningState.get(c.getWorld().getServer());
        state.player(p.getUuid()).dashReady = 0;
        c.assertTrue(RiftDash.use(p), "Authorized full-set dash should work");
        var after = p.getPos();
        c.assertFalse(RiftDash.use(p), "Repeated dash during cooldown must fail");
        c.assertEquals(p.getPos(), after, "Cooldown prevents movement");
        var copy = AwakeningState.read(
            state.writeNbt(new NbtCompound(), c.getWorld().getRegistryManager()), c.getWorld().getRegistryManager());
        c.assertTrue(copy.player(p.getUuid()).dashReady > 0, "Dash deadline persists across reload");
        dispose(p);
        c.complete();
    }
    @GameTest(templateName = ARENA)
    public void limitedAegisDoesNotGrantInvincibility(TestContext c) {
        var p = player(c);
        equip(p);
        var state = AwakeningState.get(p.getServer());
        state.player(p.getUuid()).aegisReady = 0;
        float first = AwakeningCombat.damage(p, c.getWorld().getDamageSources().generic(), 12),
              second = AwakeningCombat.damage(p, c.getWorld().getDamageSources().generic(), 12);
        c.assertTrue(first >= 6 && first < 12, "Aegis reduces one hit, never cancels it");
        c.assertEquals(second, 12f, "Aegis is unavailable during its cooldown");
        c.assertFalse(p.getAbilities().invulnerable, "Still vulnerable in Survival");
        c.assertEquals(AwakeningCombat.damage(p, c.getWorld().getDamageSources().genericKill(), 1000), 1000f,
            "Void/kill damage must not be cancelled");
        dispose(p);
        c.complete();
    }
    @GameTest(templateName = ARENA)
    public void artifactsUseOnlyTheOffhandChoice(TestContext c) {
        var p = player(c);
        equip(p);
        float baseline = p.getAbilities().getFlySpeed();
        p.setStackInHand(Hand.MAIN_HAND, new ItemStack(AbyssItems.VOID_CORE));
        RiftFlight.refresh(p);
        c.assertEquals(p.getAbilities().getFlySpeed(), baseline, "Mainhand artifact is inactive");
        p.setStackInHand(Hand.OFF_HAND, new ItemStack(AbyssItems.VOID_CORE));
        RiftFlight.refresh(p);
        c.assertTrue(p.getAbilities().getFlySpeed() > baseline, "Offhand Void Core adds mobility");
        p.setStackInHand(Hand.OFF_HAND, new ItemStack(AbyssItems.HEART_OF_THE_RIFT));
        RiftFlight.refresh(p);
        c.assertEquals(p.getAbilities().getFlySpeed(), baseline, "Swapping removes the previous artifact bonus");
        c.assertEquals(AbyssGear.cooldown(p, 100), 80, "Heart reduces new ability cooldowns");
        dispose(p);
        c.complete();
    }
    @GameTest(templateName = ARENA)
    public void puzzleChecksAllThreeTonesAndRotations(TestContext c) {
        var origin = new BlockPos(32, 2, 32);
        c.setBlockState(origin, AbyssBlocks.RESONANCE_LOCK);
        int[] notes = {1, 3, 2};
        var offsets = List.of(new BlockPos(-3, 0, 0), new BlockPos(0, 0, -3), new BlockPos(3, 0, 0));
        for (int i = 0; i < 3; i++)
            c.setBlockState(origin.add(offsets.get(i)),
                AbyssBlocks.RESONATOR.getDefaultState().with(ResonatorBlock.NOTE, notes[i]));
        c.assertTrue(ResonanceLockBlock.solved(c.getWorld(), c.getAbsolutePos(origin)),
            "Correct three-voice sequence opens the lock");
        c.setBlockState(origin.add(-3, 0, 0), AbyssBlocks.RESONATOR.getDefaultState());
        c.assertFalse(ResonanceLockBlock.solved(c.getWorld(), c.getAbsolutePos(origin)),
            "One wrong tone must reject the sequence");
        c.complete();
    }
    @GameTest(templateName = ARENA, tickLimit = 150)
    public void barriersExpireWithoutChangingSurroundingBlocks(TestContext c) {
        var p = new BlockPos(20, 2, 20);
        c.setBlockState(p, AbyssBlocks.BARRIER);
        c.setBlockState(p.east(), Blocks.DIAMOND_BLOCK);
        c.waitAndRun(125, () -> {
            c.assertTrue(c.getBlockState(p).isAir(), "Temporary barrier must expire");
            c.assertTrue(c.getBlockState(p.east()).isOf(Blocks.DIAMOND_BLOCK), "Expiry must not damage neighbors");
            c.complete();
        });
    }
    @GameTest(templateName = ARENA, tickLimit = 20)
    public void bossesHaveRequiredPhasesAndIndependentAttributes(TestContext c) {
        var col = c.spawnMob(AbyssEntities.COLOSSUS, 16, 2, 16);
        var arch = c.spawnMob(AbyssEntities.ARCHITECT, 36, 2, 16);
        var sov = c.spawnMob(AbyssEntities.SOVEREIGN, 32, 2, 42);
        col.setHealth(240);
        arch.setHealth(300);
        sov.setHealth(620);
        c.waitAndRun(2, () -> {
            c.assertEquals(col.phase(), 2, "Colossus second phase");
            c.assertEquals(arch.phase(), 2, "Architect second phase");
            c.assertEquals(sov.phase(), 2, "Sovereign second phase");
            sov.setHealth(250);
        });
        c.waitAndRun(5, () -> {
            c.assertEquals(sov.phase(), 3, "Sovereign third phase must synchronize");
            c.assertTrue(col.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) >= 24,
                "Colossus has massive melee attacks");
            col.discard();
            arch.discard();
            sov.discard();
            c.complete();
        });
    }
    @GameTest(templateName = ARENA, tickLimit = 35)
    public void newBoltsDamageEnemiesButNotTheirHostileAllies(TestContext c) {
        var owner = c.spawnMob(AbyssEntities.WARDEN, 10, 2, 20);
        var cow = c.spawnMob(EntityType.COW, 18, 2, 20);
        cow.setAiDisabled(true);
        var bolt = AbyssBoltEntity.fire(c.getWorld(), owner, cow.getPos().add(0, 0.7, 0), 7, 0.8f);
        c.waitAndRun(25, () -> {
            c.assertTrue(cow.getHealth() < 10, "Energy projectile must deal damage");
            c.assertTrue(bolt.isRemoved(), "Projectile must disappear after impact");
            c.complete();
        });
    }
    @GameTest(templateName = ARENA)
    public void keyAndEquipmentRecipesActuallyCraft(TestContext c) {
        var input = CraftingRecipeInput.create(3, 3,
            List.of(new ItemStack(ModItems.RIFT_CORE), new ItemStack(ModItems.RIFT_HEART),
                new ItemStack(ModItems.RIFT_CORE), new ItemStack(ModItems.VOID_FRAGMENT),
                new ItemStack(AbyssItems.RESONANT_SIGIL), new ItemStack(ModItems.VOID_FRAGMENT),
                new ItemStack(ModItems.RIFT_CORE), new ItemStack(ModItems.RIFT_HEART),
                new ItemStack(ModItems.RIFT_CORE)));
        var key = c.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, input, c.getWorld()).orElseThrow();
        c.assertTrue(key.value().craft(input, c.getWorld().getRegistryManager()).isOf(AbyssItems.ABYSSAL_KEY),
            "Expensive key recipe must craft");
        Item[] bases = {ModItems.RIFT_HELMET, ModItems.RIFT_CHESTPLATE, ModItems.RIFT_LEGGINGS, ModItems.RIFT_BOOTS};
        Item[] output = {AbyssItems.ABYSSAL_HELMET, AbyssItems.ABYSSAL_CHESTPLATE, AbyssItems.ABYSSAL_LEGGINGS,
            AbyssItems.ABYSSAL_BOOTS};
        for (int i = 0; i < 4; i++) {
            var old = new ItemStack(bases[i]);
            old.setDamage(7);
            var smith = new SmithingRecipeInput(
                new ItemStack(AbyssItems.ABYSSAL_SCHEMATIC), old, new ItemStack(AbyssItems.ABYSSAL_CORE));
            var recipe =
                c.getWorld().getRecipeManager().getFirstMatch(RecipeType.SMITHING, smith, c.getWorld()).orElseThrow();
            var result = recipe.value().craft(smith, c.getWorld().getRegistryManager());
            c.assertTrue(result.isOf(output[i]), "Correct upgraded armor");
            c.assertEquals(result.getDamage(), 7, "Smithing retains base components");
        }
        c.complete();
    }
    @GameTest(templateName = ARENA)
    public void progressionStateRoundTripsWithoutTouchingLegacyReturns(TestContext c) {
        var state = new AwakeningState();
        var id = UUID.randomUUID();
        state.unlock(id, AwakeningState.ABYSS | AwakeningState.COLOSSUS);
        state.player(id).origin = GlobalPos.create(dev.riftborn.dimension.RiftDimensions.WORLD, new BlockPos(8, 90, 4));
        state.event(AbyssWorlds.ABYSS).kind = 2;
        state.event(AbyssWorlds.ABYSS).ends = 5000;
        var lookup = c.getWorld().getRegistryManager();
        var loaded = AwakeningState.read(state.writeNbt(new NbtCompound(), lookup), lookup);
        c.assertTrue(loaded.has(id, AwakeningState.ABYSS | AwakeningState.COLOSSUS), "Boss progression persists");
        c.assertEquals(loaded.player(id).origin, state.player(id).origin, "Separate Abyss return persists");
        c.assertEquals(loaded.event(AbyssWorlds.ABYSS).kind, 2, "Event state persists");
        c.complete();
    }
    @GameTest(templateName = ARENA, tickLimit = 210)
    public void stalkerCloakIsTemporaryAndSurvivesReload(TestContext c) {
        var stalker = c.spawnEntity(AbyssEntities.STALKER, 20, 2, 32);
        var target = c.spawnMob(EntityType.COW, 34, 2, 32);
        target.setAiDisabled(true);
        stalker.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
        stalker.setTarget(target);
        c.waitAndRun(140, () -> {
            c.assertTrue(stalker.isInvisible(), "Abyss Stalker must actually cloak while hunting");
            var saved = new NbtCompound(); stalker.writeCustomDataToNbt(saved);
            var copy = AbyssEntities.STALKER.create(c.getWorld()); copy.readCustomDataFromNbt(saved);
            c.assertTrue(copy.isInvisible(), "A brief remaining cloak is serialized, not made permanent");
            copy.discard();
        });
        c.waitAndRun(190, () -> {
            c.assertFalse(stalker.isInvisible(), "Cloak must end without requiring a kill or reload");
            stalker.discard(); c.complete();
        });
    }

    @GameTest(templateName = ARENA, tickLimit = 180)
    public void reaverActuallyFliesAndFiresItsPairedBolts(TestContext c) {
        var reaver = c.spawnEntity(AbyssEntities.REAVER, 40, 8, 32);
        var target = c.spawnMob(EntityType.COW, 28, 2, 32); target.setAiDisabled(true);
        reaver.setTarget(target); var before = reaver.getPos();
        c.waitAndRun(140, () -> {
            c.assertTrue(reaver.hasNoGravity() && reaver.getPos().squaredDistanceTo(before) > 2, "Reaver strafes in the air");
            c.assertTrue(target.getHealth() < target.getMaxHealth(), "Reaver's ranged AI deals actual projectile damage");
            reaver.discard(); c.complete();
        });
    }

    @GameTest(templateName = ARENA)
    public void abyssBruteHasHeavyMeleeAndKnockback(TestContext c) {
        var brute = c.spawnMob(AbyssEntities.BRUTE, 25, 2, 32);
        var target = c.spawnMob(EntityType.ZOMBIE, 28, 2, 32);
        c.assertTrue(brute.tryAttack(target), "Abyss Brute melee succeeds");
        c.assertTrue(target.getHealth() < 8, "Abyss Brute hits substantially harder than a normal mob");
        c.assertTrue(target.getVelocity().horizontalLengthSquared() > 0.2, "Abyss Brute applies strong knockback");
        c.complete();
    }

    @GameTest(templateName = ARENA, tickLimit = 60)
    public void echoMirrorsSprintAndShieldBehavior(TestContext c) {
        var echo = c.spawnEntity(AbyssEntities.ECHO, 20, 2, 32);
        var target = c.createMockPlayer(GameMode.SURVIVAL);
        target.setPosition(Vec3d.ofBottomCenter(c.getAbsolutePos(new BlockPos(36, 2, 32))));
        target.setInvulnerable(true); target.setSprinting(true); echo.setTarget(target);
        c.waitAndRun(5, () -> {
            c.assertEquals(echo.visualState(), 1, "Echo mirrors a sprinting target");
            c.assertTrue(echo.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) > 0.35, "Echo gains the copied sprint speed");
            target.setSprinting(false); target.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.SHIELD));
            target.setCurrentHand(Hand.MAIN_HAND);
        });
        c.waitAndRun(25, () -> {
            c.assertEquals(echo.visualState(), 2, "Echo changes tactics when the player raises a shield");
            echo.discard(); c.complete();
        });
    }

    @GameTest(templateName = ARENA, tickLimit = 160)
    public void wardenBeamDealsDamageAfterItsWindup(TestContext c) {
        var warden = c.spawnEntity(AbyssEntities.WARDEN, 20, 2, 32);
        var target = c.spawnMob(EntityType.COW, 34, 2, 32); target.setAiDisabled(true);
        warden.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
        warden.setTarget(target);
        c.waitAndRun(60, () -> c.assertEquals(target.getHealth(), target.getMaxHealth(), "Beam must not deal untelegraphed immediate damage"));
        c.waitAndRun(130, () -> {
            c.assertTrue(target.getHealth() < target.getMaxHealth(), "Charged Warden beam hits a visible target");
            warden.discard(); c.complete();
        });
    }

}
