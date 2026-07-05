package tong.statmod.storage;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import tong.statmod.network.SyncHelper;
import tong.statmod.stamina.StaminaData;

public final class PlayerCloneDataHandler {
    private PlayerCloneDataHandler() {}

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        PlayerStatData originalStats = event.getOriginal().getData(ModAttachments.STATS);
        PlayerStatData clonedStats = event.getEntity().getData(ModAttachments.STATS);
        clonedStats.copyFrom(originalStats);

        StaminaData originalStamina = event.getOriginal().getData(ModAttachments.STAMINA);
        StaminaData clonedStamina = event.getEntity().getData(ModAttachments.STAMINA);
        clonedStamina.copyFrom(originalStamina);

        if (event.getEntity() instanceof ServerPlayer player) {
            SyncHelper.syncAll(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SyncHelper.syncAll(player);
        }
    }
}
