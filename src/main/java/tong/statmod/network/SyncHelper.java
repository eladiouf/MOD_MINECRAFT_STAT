package tong.statmod.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class SyncHelper {
    private SyncHelper() {}

    public static void syncStats(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            StatNetwork.sendSnapshot(serverPlayer);
        }
    }
}
