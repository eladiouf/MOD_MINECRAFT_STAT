package tong.statmod.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public final class SyncHelper {
    private SyncHelper() {}

    public static void syncStats(ServerPlayer player) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PacketDistributor.sendToPlayer(player,
                new StatUpdatePayload(data.getLevels(), data.getXp(), data.getSoulLevel()));
    }

    public static void syncPerks(ServerPlayer player) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PacketDistributor.sendToPlayer(player,
                new SyncPerksPayload(data.getUnlockedPerks(), data.getPerkPoints()));
    }

    public static void syncAll(ServerPlayer player) {
        syncStats(player);
        syncPerks(player);
    }
}
