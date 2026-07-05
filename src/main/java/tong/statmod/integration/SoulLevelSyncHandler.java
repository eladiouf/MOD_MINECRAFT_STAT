package tong.statmod.integration;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.network.SyncHelper;
import tong.statmod.storage.PlayerStatData;
import tong.statmod.storage.ModAttachments;

public class SoulLevelSyncHandler {
    private static final int INTERVAL = 40;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        var player = event.getEntity();
        if (player.tickCount % INTERVAL != 0) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        int tensuraSoul = PlayerDataBridge.getSoulLevel(player);
        if (data.getSoulLevel() != tensuraSoul) {
            data.setSoulLevel(tensuraSoul);
            if (player instanceof ServerPlayer serverPlayer) {
                SyncHelper.syncStats(serverPlayer);
            }
        }
    }
}
