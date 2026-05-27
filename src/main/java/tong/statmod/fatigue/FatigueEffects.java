package tong.statmod.fatigue;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class FatigueEffects {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
            FatigueManager.FatigueThreshold threshold = fatigue.getThreshold();

            switch (threshold) {
                case MODERATE -> applyDebuff(player, 1, 0, 0);
                case SEVERE -> applyDebuff(player, 2, 0, 1);
                case CRITICAL -> applyDebuff(player, 3, 1, 1);
                case EXHAUSTED -> applyDebuff(player, 4, 2, 2);
                default -> clearDebuffs(player);
            }
        });
    }

    private static void applyDebuff(ServerPlayer player, int weakness, int slowness, int fatigue) {
        if (weakness > 0) player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, weakness - 1, true, false));
        if (slowness > 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, slowness - 1, true, false));
        if (fatigue > 0) player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, fatigue - 1, true, false));
    }

    private static void clearDebuffs(ServerPlayer player) {
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
    }
}
