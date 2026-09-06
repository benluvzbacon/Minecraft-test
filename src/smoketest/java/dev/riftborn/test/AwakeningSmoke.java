package dev.riftborn.test;
import dev.riftborn.Riftborn;import dev.riftborn.awakening.*;import dev.riftborn.awakening.block.ResonanceLockBlock;import dev.riftborn.awakening.client.AwakeningClient;import dev.riftborn.awakening.entity.*;import dev.riftborn.awakening.item.RiftStaffItem;import dev.riftborn.dimension.RiftDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;import net.minecraft.client.option.Perspective;import net.minecraft.client.util.ScreenshotRecorder;import net.minecraft.entity.*;import net.minecraft.item.Item;import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;import net.minecraft.util.*;import net.minecraft.util.hit.BlockHitResult;import net.minecraft.util.math.*;import java.util.*;
/** Continues the unchanged 1.5 integration sequence with the complete 2.0 endgame. */
public final class AwakeningSmoke {
    private int ticks,stage,since,bossIndex;private Vec3d before,after;private boolean done;
    public static void begin(){var test=new AwakeningSmoke();ClientTickEvents.END_CLIENT_TICK.register(test::tick);Riftborn.LOGGER.info("RIFTBORN_20_BEGIN");}
    private static void require(boolean b,String msg){if(!b)throw new IllegalStateException("RIFTBORN_SMOKE_FAILURE: "+msg);}
    private void next(int n){stage=n;since=ticks;}
    private int age(){return ticks-since;}
    private static void marker(String name){Riftborn.LOGGER.info(name);}
    private static void block(MinecraftClient c,BlockPos p){c.interactionManager.interactBlock(c.player,Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(p),Direction.UP,p,false));}
    private static void use(MinecraftClient c){c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);}
    private static void fly(MinecraftClient c){if(c.player.isOnGround())c.player.jump();c.player.getAbilities().flying=true;c.player.sendAbilitiesUpdate();}
    private static void photo(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,name+".png",c.getFramebuffer(),m->Riftborn.LOGGER.info("Awakening image: {}",m.getString()));}
    private static void cycle(MinecraftClient c){c.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(c.player,ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));use(c);c.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(c.player,ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));}
    private AbyssBossEntity boss(MinecraftClient c){for(var e:c.world.getEntities())if(e instanceof AbyssBossEntity b&&b.isAlive())return b;return null;}
    private void tick(MinecraftClient c){if(done)return;if(++ticks>15000)throw new IllegalStateException("RIFTBORN_SMOKE_FAILURE: Awakening timed out at "+stage);if(ticks%200==0)Riftborn.LOGGER.info("Awakening stage {} age={} world={} pos={}",stage,age(),c.world==null?"none":c.world.getRegistryKey().getValue(),c.player==null?"none":c.player.getPos());
        if(c.world==null||c.player==null||c.interactionManager==null)return;boolean abyss=c.world.getRegistryKey().equals(AbyssWorlds.ABYSS);
        switch(stage){
            case 0->{if(c.world.getRegistryKey().equals(RiftDimensions.WORLD)&&c.player.squaredDistanceTo(16.5,141,6.5)<2&&c.world.getBlockState(new BlockPos(16,141,4)).isOf(AbyssBlocks.GATE)&&c.currentScreen==null){block(c,new BlockPos(16,141,4));next(1);}}
            case 1->{if(age()>25){require(!abyss,"No key must mean no Abyss entry");marker("RIFTBORN_20_ENTRY_LOCK_OK");next(2);}}
            case 2->{if(c.player.squaredDistanceTo(16.5,141,15.5)<2&&c.world.getBlockState(new BlockPos(16,141,12)).isOf(AbyssBlocks.RESONANCE_LOCK)){block(c,new BlockPos(16,141,12));next(3);}}
            case 3->{if(age()>25){require(c.world.getBlockState(new BlockPos(16,141,12)).get(ResonanceLockBlock.OPEN),"Resonance lock must open through a real interaction");marker("RIFTBORN_20_PUZZLE_OK");next(4);}}
            case 4->{if(c.player.getMainHandStack().isOf(AbyssItems.ABYSSAL_KEY)&&c.player.squaredDistanceTo(16.5,141,6.5)<2){block(c,new BlockPos(16,141,4));next(5);}}
            case 5->{if(abyss&&c.currentScreen==null&&age()>30){require(!c.player.getMainHandStack().isOf(AbyssItems.ABYSSAL_KEY),"First entry consumes the key");marker("RIFTBORN_20_ABYSS_ENTRY_OK");next(6);}}
            case 6->{if(abyss&&AbyssGear.fullSet(c.player)&&c.player.squaredDistanceTo(0.5,325,-5.5)<2&&age()>40){
                require(c.player.getAbilities().allowFlying&&Math.abs(c.player.getAbilities().getFlySpeed()-0.22f)<0.0001,"Abyssal Ascension upgrades synchronized flight");
                var types=new HashSet<EntityType<?>>();for(var entity:c.world.getEntities()){types.add(entity.getType());if(entity instanceof AbyssHostileEntity)require(c.getEntityRenderDispatcher().getRenderer(entity)!=null,"Abyss mob renderer");}
                require(types.containsAll(Set.of(AbyssEntities.STALKER,AbyssEntities.REAVER,AbyssEntities.BRUTE,AbyssEntities.ECHO,AbyssEntities.WARDEN)),"All five new mob roles must reach the client");
                c.player.setYaw(0);c.player.setPitch(-6);next(7);}}
            case 7->{if(age()>60){photo(c,"abyss-showcase");marker("RIFTBORN_20_ABYSS_RENDER_OK");fly(c);c.options.jumpKey.setPressed(true);next(8);}}
            case 8->{if(age()>8){c.options.jumpKey.setPressed(false);c.player.setYaw(-90);before=c.player.getPos();ClientPlayNetworking.send(new AwakeningNetwork.Dash());next(9);}}
            case 9->{if(age()>20){require(c.player.getX()>before.x+10.5,"Networked Rift Dash moves forward");require(c.player.getItemCooldownManager().isCoolingDown(AbyssItems.ABYSSAL_BOOTS),"Server synchronizes dash cooldown");after=c.player.getPos();ClientPlayNetworking.send(new AwakeningNetwork.Dash());next(10);}}
            case 10->{if(age()>20){require(Math.abs(c.player.getX()-after.x)<0.1,"Repeated dash during cooldown cannot move");c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);c.options.hudHidden=true;next(11);}}
            case 11->{if(age()>50){photo(c,"abyssal-ascension");c.options.setPerspective(Perspective.FIRST_PERSON);c.options.hudHidden=false;marker("RIFTBORN_20_DASH_OK");next(12);}}
            case 12->{if(c.player.getMainHandStack().isOf(AbyssItems.GREATBLADE)&&c.player.squaredDistanceTo(0.5,325,0.5)<2&&age()>15){use(c);next(13);}}
            case 13->{if(age()>25){c.interactionManager.stopUsingItem(c.player);next(14);}}
            case 14->{if(age()>15){require(c.player.getMainHandStack().getDamage()>=4&&c.player.getItemCooldownManager().isCoolingDown(AbyssItems.GREATBLADE),"Greatblade cleave synchronizes durability/cooldown");marker("RIFTBORN_20_GREATBLADE_OK");next(15);}}
            case 15->{if(c.player.getMainHandStack().isOf(AbyssItems.VOIDBOW)&&age()>15){use(c);next(16);}}
            case 16->{if(age()>25){photo(c,"voidbow-draw");c.interactionManager.stopUsingItem(c.player);next(17);}}
            case 17->{if(age()>8){require(c.player.getMainHandStack().getDamage()>=1,"Voidbow consumes durability on a real shot");marker("RIFTBORN_20_VOIDBOW_OK");next(18);}}
            case 18->{if(c.player.getMainHandStack().isOf(AbyssItems.RIFT_STAFF)&&age()>15){use(c);next(19);}}
            case 19->{if(age()>18){require(c.player.getMainHandStack().getDamage()>=1,"Staff lance costs durability");marker("RIFTBORN_20_STAFF_LANCE_OK");cycle(c);next(20);}}
            case 20->{if(RiftStaffItem.spell(c.player.getMainHandStack())==1&&!c.player.getItemCooldownManager().isCoolingDown(AbyssItems.RIFT_STAFF)&&age()>30){use(c);next(21);}}
            case 21->{if(age()>20){require(c.player.getMainHandStack().getDamage()>=3,"Staff pulse uses the second ability");marker("RIFTBORN_20_STAFF_PULSE_OK");cycle(c);next(22);}}
            case 22->{if(RiftStaffItem.spell(c.player.getMainHandStack())==2&&!c.player.getItemCooldownManager().isCoolingDown(AbyssItems.RIFT_STAFF)){use(c);next(23);}}
            case 23->{if(age()>110){require(c.player.getHealth()>10,"Staff mend restores health");marker("RIFTBORN_20_STAFF_MEND_OK");next(24);}}
            case 24->{if(c.player.getOffHandStack().isOf(AbyssItems.VOID_CORE)&&c.player.getAbilities().getFlySpeed()>0.24f){marker("RIFTBORN_20_ARTIFACT_OK");next(25);}}
            case 25->{if(c.player.getMainHandStack().isOf(AbyssItems.ABYSSAL_CORE)&&c.player.squaredDistanceTo(0.5,325,5.5)<2){block(c,new BlockPos(0,325,8));next(26);}}
            case 26->{if(boss(c)!=null){marker("RIFTBORN_20_BOSS_SEEN");next(27);}}
            case 27->{if(age()>45){photo(c,bossIndex==0?"abyss-colossus":bossIndex==1?"rift-architect":"abyss-sovereign");
                if(bossIndex==2){marker("RIFTBORN_20_SOVEREIGN_TRANSFORM");next(28);}else{marker("RIFTBORN_20_BOSS_COMBAT");next(29);}}}
            case 28->{if(boss(c)!=null&&boss(c).phase()==3&&age()>35){photo(c,"abyss-sovereign-awakened");marker("RIFTBORN_20_BOSS_COMBAT");next(29);}}
            case 29->{if(age()==10)fly(c);if(age()>440){marker("RIFTBORN_20_BOSS_DEFEAT_REQUEST");next(30);}}
            case 30->{if(boss(c)==null&&age()>30){if(bossIndex==0){bossIndex=1;next(31);}else if(bossIndex==1){next(32);}else{marker("RIFTBORN_20_FINAL_BOSS_OK");next(39);}}}
            case 31->{if(c.player.getMainHandStack().isOf(AbyssItems.TITAN_CORE)&&c.player.squaredDistanceTo(0.5,325,5.5)<2){block(c,new BlockPos(0,325,8));next(26);}}
            case 32->{if(AwakeningClient.eventKind==1&&age()>160){photo(c,"abyss-storm");marker("RIFTBORN_20_STORM_OK");next(33);}}
            case 33->{if(AwakeningClient.eventKind==0&&c.player.getMainHandStack().isOf(AbyssItems.COLLAPSE_CATALYST)&&c.player.squaredDistanceTo(0.5,325,5.5)<2){block(c,new BlockPos(0,325,8));next(34);}}
            case 34->{if(AwakeningClient.eventKind==2&&boss(c)!=null&&boss(c).bossKind()==3&&age()>140){photo(c,"abyss-collapse");marker("RIFTBORN_20_HERALD_DEFEAT_REQUEST");next(35);}}
            case 35->{if(AwakeningClient.eventKind==0&&boss(c)==null&&age()>30)next(36);}
            case 36->{if(c.player.getMainHandStack().isOf(AbyssItems.SOVEREIGN_SIGIL)&&c.player.squaredDistanceTo(0.5,325,5.5)<2){bossIndex=2;block(c,new BlockPos(0,325,8));next(26);}}
            case 39->{if(abyss&&c.player.squaredDistanceTo(0.5,325,-2.5)<2&&age()>30){block(c,new BlockPos(0,325,-5));next(40);}}
            case 40->{if(c.world.getRegistryKey().equals(RiftDimensions.WORLD)&&c.currentScreen==null){marker("RIFTBORN_20_ABYSS_RETURN_OK");next(41);}}
            case 41->{if(age()>340){block(c,new BlockPos(16,141,4));next(42);}}
            case 42->{if(abyss&&c.currentScreen==null){marker("RIFTBORN_20_ATTUNEMENT_OK");next(43);}}
            case 43->{if(age()>40){marker("RIFTBORN_20_CLIENT_OK");marker("RIFTBORN_MULTIPLAYER_SMOKE_OK");done=true;c.scheduleStop();}}
            default->{}
        }
    }
}
