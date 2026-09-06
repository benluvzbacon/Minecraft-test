package dev.riftborn.awakening.item;
import dev.riftborn.Riftborn;import dev.riftborn.awakening.*;import dev.riftborn.dimension.RiftDimensions;
import net.minecraft.component.DataComponentTypes;import net.minecraft.component.type.NbtComponent;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.text.Text;import net.minecraft.util.*;import net.minecraft.util.math.*;import net.minecraft.world.World;
public final class LocatorItem extends ArtifactItem {
    private final boolean key;
    public LocatorItem(Settings s,boolean key){super(s);this.key=key;}
    private static final String[] TARGETS={"colossus","architect","sovereign","fortress","laboratory","observatory"};
    @Override public TypedActionResult<ItemStack> use(World w,PlayerEntity user,Hand hand){
        var stack=user.getStackInHand(hand);if(!(user instanceof ServerPlayerEntity p))return TypedActionResult.success(stack);
        if(user.getItemCooldownManager().isCoolingDown(this))return TypedActionResult.fail(stack);
        String target="gateway";
        var data=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
        if(!key&&w.getRegistryKey().equals(AbyssWorlds.ABYSS)) {
            int mode=Math.floorMod(data.getInt("AbyssTarget"),TARGETS.length);
            if(user.isSneaking()){mode=(mode+1)%TARGETS.length;data.putInt("AbyssTarget",mode);stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(data));}
            target=TARGETS[mode];
        } else if(!w.getRegistryKey().equals(RiftDimensions.WORLD)){p.sendMessage(Text.translatable("awakening.riftborn.locator_world"),true);return TypedActionResult.fail(stack);}
        user.getItemCooldownManager().set(this,100);
        if(target.equals("observatory")&&AwakeningEvents.kind(p.getServerWorld())==0){p.sendMessage(Text.translatable("awakening.riftborn.observatory_veiled"),false);return TypedActionResult.success(stack);}
        var found=p.getServerWorld().locateStructure(AbyssWorlds.sites(target),p.getBlockPos(),Riftborn.CONFIG.locateRadius,false);
        if(found==null){p.sendMessage(Text.translatable("awakening.riftborn.locator_none",Text.translatable("location.riftborn."+target)),false);return TypedActionResult.success(stack);}
        int distance=(int)Math.sqrt(found.getSquaredDistance(p.getBlockPos()));
        p.sendMessage(Text.translatable("awakening.riftborn.locator_found",Text.translatable("location.riftborn."+target),found.getX(),found.getZ(),distance),false);
        Vec3d end=Vec3d.ofCenter(found).subtract(p.getPos()).normalize().multiply(5).add(p.getEyePos());AbyssFx.line(p.getServerWorld(),p.getEyePos(),end,12);
        return TypedActionResult.success(stack);
    }
}
