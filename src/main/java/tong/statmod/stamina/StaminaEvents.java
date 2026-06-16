package tong.statmod.stamina;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;

public final class StaminaEvents {
    private StaminaEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        StaminaData data = player.getData(ModAttachments.STAMINA);
        if (shouldMeditate(player)) {
            MeditationManager.start(data);
        } else {
            MeditationManager.stop(data);
        }

        int endurance = RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
        StaminaManager.tickPassive(data, endurance, data.meditating(), player.isSleeping());

        if (player.tickCount % 20 == 0 && player instanceof ServerPlayer serverPlayer) {
            SyncHelper.syncStamina(serverPlayer);
        }
    }

    private static boolean shouldMeditate(Player player) {
        return player.onGround()
                && player.isShiftKeyDown()
                && !player.isSprinting()
                && player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4;
    }
}
