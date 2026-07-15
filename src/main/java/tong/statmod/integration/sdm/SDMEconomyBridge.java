package tong.statmod.integration.sdm;

import net.minecraft.server.level.ServerPlayer;

public final class SDMEconomyBridge {
    private SDMEconomyBridge() {}

    public static boolean available() {
        return false;
    }

    public static long getCoins(ServerPlayer player) {
        return 0;
    }

    public static boolean addCoins(ServerPlayer player, long amount) {
        return false;
    }

    public static boolean addCoinsTransactional(ServerPlayer player, long amount) {
        return false;
    }
}
