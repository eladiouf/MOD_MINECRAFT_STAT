package tong.statmod.integration.ftbteams;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class FTBTeamsBridge {
    private FTBTeamsBridge() {}

    public static boolean loaded() {
        try {
            return FTBTeamsAPI.api().isManagerLoaded();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean sameTeam(ServerPlayer a, ServerPlayer b) {
        if (a.getUUID().equals(b.getUUID())) return true;
        if (!loaded()) return false;
        try {
            return FTBTeamsAPI.api().getManager().arePlayersInSameTeam(a.getUUID(), b.getUUID());
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static Component teamName(ServerPlayer player) {
        if (!loaded()) return null;
        try {
            return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                    .map(team -> team.getColoredName())
                    .orElse(null);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static List<ServerPlayer> teammatesOnFloor(ServerPlayer anchor, ServerLevel level, int floor) {
        return tong.statmod.dungeon.DungeonTeleportHandler.playersOnFloor(level, floor).stream()
                .filter(candidate -> sameTeam(anchor, candidate))
                .toList();
    }
}
