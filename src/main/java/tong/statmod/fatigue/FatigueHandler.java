package tong.statmod.fatigue;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.network.FatiguePacket;
import tong.statmod.network.NetworkHandler;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class FatigueHandler {
    private static float lastSync = 0;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                if (player.isSleeping()) {
                    fatigue.reset();
                    return;
                }
                if (fatigue.getFatigue() > 0) {
                    if (player.isShiftKeyDown()) {
                        fatigue.reduceFatigue(3.0f);
                    } else if (!player.isSprinting() && !player.swinging) {
                        fatigue.reduceFatigue(2.0f);
                    }
                }
                if (player.isSprinting()) {
                    fatigue.addFatigue(1.0f);
                }
                if (!player.onGround() && player.getDeltaMovement().y > 0.08) {
                    fatigue.addFatigue(2.0f);
                }
                syncIfChanged(player, fatigue);
            });
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                fatigue.addFatigue(5.0f);
                syncIfChanged(player, fatigue);
            });
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                fatigue.addFatigue(1.0f);
                syncIfChanged(player, fatigue);
            });
        }
    }

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                fatigue.reset();
                syncIfChanged(player, fatigue);
            });
        }
    }

    private static void syncIfChanged(ServerPlayer player, FatigueManager fatigue) {
        float current = fatigue.getFatigue();
        if (Math.abs(current - lastSync) > 5.0f) {
            NetworkHandler.sendToPlayer(new FatiguePacket(current), player);
            lastSync = current;
        }
    }
}
