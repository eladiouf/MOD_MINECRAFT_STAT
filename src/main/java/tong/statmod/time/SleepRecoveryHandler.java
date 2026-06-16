package tong.statmod.time;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.stamina.StaminaData;
import tong.statmod.stamina.StaminaManager;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;

public final class SleepRecoveryHandler {
    private SleepRecoveryHandler() {}

    public static void applyWakeBonus(StaminaData data, int enduranceLevel) {
        StaminaManager.restore(data, 35.0f, enduranceLevel);
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        StaminaData data = player.getData(ModAttachments.STAMINA);
        int endurance = RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
        applyWakeBonus(data, endurance);
        SyncHelper.syncStamina(player);
    }
}
