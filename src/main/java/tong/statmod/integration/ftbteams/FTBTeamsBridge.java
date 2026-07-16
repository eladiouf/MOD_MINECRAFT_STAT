package tong.statmod.integration.ftbteams;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class FTBTeamsBridge {
    private FTBTeamsBridge() {}

    public static boolean loaded() {
        return false;
    }

    public static boolean sameTeam(ServerPlayer a, ServerPlayer b) {
        return true;
    }

    public static Component teamName(ServerPlayer player) {
        return null;
    }
}
