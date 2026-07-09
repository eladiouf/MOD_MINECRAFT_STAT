package tong.statmod.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class SyncLifecycleHandler {
    private SyncLifecycleHandler() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        SyncHelper.clearPlayerCache(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            SyncHelper.syncAll(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SyncHelper.clearPlayerCache(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        SyncHelper.clearPlayerCache(event.getOriginal().getUUID());
        SyncHelper.clearPlayerCache(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        SyncHelper.clearPlayerCache(event.getEntity().getUUID());
    }
}
