package tong.statmod.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import tong.statmod.STATMod;
import java.util.*;

public class FtbTeamsIntegration {
    private static boolean ftbTeamsLoaded = false;

    public static void init() {
        ftbTeamsLoaded = ModList.get().isLoaded("ftbteams");
        if (ftbTeamsLoaded) {
            STATMod.LOGGER.info("FTB Teams detected — team integration enabled");
        }
    }

    public static boolean isLoaded() { return ftbTeamsLoaded; }

    public static boolean areAllied(ServerPlayer p1, ServerPlayer p2) {
        if (!ftbTeamsLoaded) {
            Set<UUID> party = tong.statmod.party.PartyManager.getPartyMembers(p1.getUUID());
            return party.contains(p2.getUUID());
        }
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Object api = apiClass.getMethod("api").invoke(null);
            Object manager = api.getClass().getMethod("getManager").invoke(api);
            return (boolean) manager.getClass()
                .getMethod("arePlayersInSameTeam", UUID.class, UUID.class)
                .invoke(manager, p1.getUUID(), p2.getUUID());
        } catch (Exception e) {
            return false;
        }
    }

    public static Collection<ServerPlayer> getAllies(ServerPlayer player, List<ServerPlayer> allPlayers) {
        List<ServerPlayer> allies = new ArrayList<>();
        for (ServerPlayer other : allPlayers) {
            if (other != player && areAllied(player, other)) {
                allies.add(other);
            }
        }
        return allies;
    }

    public static String getTeamName(ServerPlayer player) {
        if (!ftbTeamsLoaded) return "Party";
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Object api = apiClass.getMethod("api").invoke(null);
            Object manager = api.getClass().getMethod("getManager").invoke(api);
            Object opt = manager.getClass()
                .getMethod("getTeamForPlayerID", UUID.class)
                .invoke(manager, player.getUUID());
            if ((boolean) opt.getClass().getMethod("isPresent").invoke(opt)) {
                Object team = opt.getClass().getMethod("get").invoke(opt);
                return team.getClass().getMethod("getShortName").invoke(team).toString();
            }
            return "None";
        } catch (Exception e) {
            return "None";
        }
    }
}
