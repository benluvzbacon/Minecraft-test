package dev.riftborn.awakening.item;
import dev.riftborn.awakening.*;
import dev.riftborn.awakening.entity.AbyssHostileEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.*;
import net.minecraft.block.Block;
import java.util.List;
public final class AbyssalGreatbladeItem extends SwordItem {
    public static final ToolMaterial MATERIAL = new ToolMaterial() {
        public int getDurability() {
            return 2800;
        }
        public float getMiningSpeedMultiplier() {
            return 9;
        }
        public float getAttackDamage() {
            return 9;
        }
        public TagKey<Block> getInverseTag() {
            return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        }
        public int getEnchantability() {
            return 18;
        }
        public Ingredient getRepairIngredient() {
            return Ingredient.ofItems(AbyssItems.ABYSSAL_CRYSTAL);
        }
    };
    public AbyssalGreatbladeItem() {
        super(MATERIAL,
            new Settings()
                .fireproof()
                .rarity(Rarity.EPIC)
                .attributeModifiers(SwordItem.createAttributeModifiers(MATERIAL, 5, -3.1f)));
    }
    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }
    @Override
    public TypedActionResult<ItemStack> use(World w, PlayerEntity p, Hand h) {
        if (p.getItemCooldownManager().isCoolingDown(this))
            return TypedActionResult.fail(p.getStackInHand(h));
        p.setCurrentHand(h);
        return TypedActionResult.consume(p.getStackInHand(h));
    }
    @Override
    public void onStoppedUsing(ItemStack stack, World w, LivingEntity user, int remaining) {
        if (!(user instanceof ServerPlayerEntity p) || 72000 - remaining < 20
            || p.getItemCooldownManager().isCoolingDown(this))
            return;
        Vec3d look = Vec3d.fromPolar(0, p.getYaw());
        for (var target :
            w.getEntitiesByClass(HostileEntity.class, p.getBoundingBox().expand(5), e -> e.isAlive() && p.canSee(e))) {
            var toward = target.getPos().subtract(p.getPos()).normalize();
            if (look.dotProduct(toward) > 0.35) {
                target.damage(w.getDamageSources().playerAttack(p), 16);
                target.takeKnockback(1.2, -toward.x, -toward.z);
            }
        }
        p.getItemCooldownManager().set(this, AbyssGear.cooldown(p, 100));
        stack.damage(4, p, p.getActiveHand() == Hand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
        AbyssFx.ring(p.getServerWorld(), p.getPos().add(0, 0.3, 0), 4, 24);
        p.playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.55f);
    }
    @Override
    public void appendTooltip(ItemStack s, TooltipContext c, List<Text> t, TooltipType tt) {
        t.add(Text.translatable("item.riftborn.abyssal_greatblade.tooltip").formatted(Formatting.AQUA));
    }
}
