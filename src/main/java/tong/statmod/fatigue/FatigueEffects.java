package tong.statmod.fatigue;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.Config;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.STATMod;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class FatigueEffects {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        CapabilityHelper.withFatigue(player, fatigue -> {
            FatigueManager.FatigueThreshold threshold = fatigue.getThreshold();

            switch (threshold) {
                case WARNING -> applyWarning(player);
                case LIGHT -> applyLight(player);
                case MODERATE -> applyModerate(player);
                case SEVERE -> applySevere(player);
                case CRITICAL -> applyCritical(player);
                case EXHAUSTED -> applyExhausted(player);
                default -> clearAll(player);
            }
        });
    }

    private static void applyWarning(ServerPlayer player) {
        // -5% speed via slowness 0
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false));
    }

    private static void applyLight(ServerPlayer player) {
        // -10% damage, -10% speed
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false));
    }

    private static void applyModerate(ServerPlayer player) {
        // -25% damage, -20% speed, slow dig
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, false));
        if (player.isSprinting()) player.setSprinting(false);
    }

    private static void applySevere(ServerPlayer player) {
        // -40% damage, forced walk, heavy slow
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, true, false));
        if (player.isSprinting()) player.setSprinting(false);
    }

    private static void applyCritical(ServerPlayer player) {
        // Near death debuff - 70% damage, no sprint, mining fatigue
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 3, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 2, true, false));
        if (player.isSprinting()) player.setSprinting(false);
    }

    private static void applyExhausted(ServerPlayer player) {
        // Dying of exhaustion - 90% damage, blocked everything, taking damage
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 4, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 3, true, false));
        if (player.isSprinting()) player.setSprinting(false);

        // Take damage from exhaustion
        if (player.tickCount % Config.FATIGUE_EXHAUSTED_DAMAGE_INTERVAL.get() == 0 && !player.isCreative()) {
            player.hurt(player.damageSources().starve(), Config.FATIGUE_EXHAUSTED_DAMAGE.get().floatValue());
        }
    }

    private static void clearAll(ServerPlayer player) {
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
    }
}
