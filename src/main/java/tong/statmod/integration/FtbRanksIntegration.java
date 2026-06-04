package tong.statmod.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;

public class FtbRanksIntegration {
    private static boolean ftbRanksLoaded = false;

    public static void init() {
        ftbRanksLoaded = ModList.get().isLoaded("ftbranks");
        if (ftbRanksLoaded) {
            STATMod.LOGGER.info("FTB Ranks detected — rank integration enabled");
        }
    }

    public static boolean isLoaded() { return ftbRanksLoaded; }

    public static int getGlobalLevel(ServerPlayer player) {
        int[] sum = {0};
        CapabilityHelper.withStats(player, s -> {
            for (int i = 0; i < PlayerStats.STAT_COUNT; i++) sum[0] += s.getLevel(i);
        });
        return Math.round(sum[0] / (float) PlayerStats.STAT_COUNT);
    }

    public static int getStatLevel(ServerPlayer player, String statName) {
        return FtbTaskRegistry.getStatLevel(player, statName);
    }
}
