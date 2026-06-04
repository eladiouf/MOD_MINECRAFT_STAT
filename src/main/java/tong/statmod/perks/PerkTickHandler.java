package tong.statmod.perks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class PerkTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        CapabilityHelper.withPerks(player, perks -> {
            if (perks.isUnlocked(Perk.KEEN_NIGHT) && player.isShiftKeyDown()) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            }

            if (perks.isUnlocked(Perk.WILL_ENDURANCE)
                    && player.hasEffect(MobEffects.POISON) && player.getHealth() < player.getMaxHealth()) {
                player.heal(0.5f);
            }

            if (perks.isUnlocked(Perk.AGILITY_JUMP)) {
                boolean inCombat = !player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(12), m -> m.getTarget() == player).isEmpty();
                if (inCombat) {
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false));
                }
            }

            if (perks.isUnlocked(Perk.INTIMIDATE_AURA)) {
                player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(6), m -> m.getTarget() == player).forEach(mob -> {
                    if (mob.getMaxHealth() <= 20) {
                        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                        mob.setTarget(null);
                    }
                });
            }

            if (perks.isUnlocked(Perk.ENDURANCE_MOBILE) && player.isBlocking() && !player.isSprinting()) {
                player.setSprinting(true);
            }

            if (perks.isUnlocked(Perk.AGILITY_ELYTRA) && player.isFallFlying()) {
                boolean inCombat = !player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(16), m -> m.getTarget() == player).isEmpty();
                if (inCombat) {
                    var vel = player.getDeltaMovement();
                    double maxSpeed = 2.0;
                    double bx = Math.min(vel.x + 0.02, maxSpeed);
                    double bz = Math.min(vel.z + 0.02, maxSpeed);
                    player.setDeltaMovement(bx, vel.y, bz);
                }
            }

            if (perks.isUnlocked(Perk.TRACKING_SCENT) && player.isShiftKeyDown()) {
                player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(20)).forEach(mob -> {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
                });
            }

            if (perks.isUnlocked(Perk.TRACKING_STALKER)) {
                player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(16), m -> m.getTarget() == player).forEach(mob -> {
                    if (mob.distanceTo(player) > 12) mob.setTarget(null);
                });
            }

            if (perks.isUnlocked(Perk.KEEN_EXPLORER)) {
                var mainHand = player.getMainHandItem();
                var offHand = player.getOffhandItem();
                boolean holdingMap = mainHand.getItem() == Items.FILLED_MAP || offHand.getItem() == Items.FILLED_MAP;
                if (holdingMap) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
                }
            }

            if (perks.isUnlocked(Perk.KEEN_DANGER)) {
                boolean targeted = !player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(16), m -> m.getTarget() == player).isEmpty();
                if (targeted && player.tickCount % 60 == 0) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.5f, 1.5f);
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("⚠ Danger!").withStyle(net.minecraft.ChatFormatting.YELLOW),
                        true);
                }
            }
        });
    }
}
