package tong.statmod.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;
import tong.statmod.stats.StatType;

public class FtbTaskRegistry {
    private static boolean ftbQuestsLoaded = false;

    public static void init() {
        ftbQuestsLoaded = ModList.get().isLoaded("ftbquests");
        if (ftbQuestsLoaded) {
            STATMod.LOGGER.info("FTB Quests detected - stat task helpers enabled");
        }
    }

    public static boolean isLoaded() { return ftbQuestsLoaded; }

    public static int getStatLevel(Player player, String statName) {
        ServerPlayer sp = (ServerPlayer) player;
        if (statName.equals("any") || statName.equals("global")) {
            int[] sum = {0};
            CapabilityHelper.withStats(player, s -> {
                for (int i = 0; i < PlayerStats.STAT_COUNT; i++) sum[0] += s.getLevel(i);
            });
            int avg = sum[0] / PlayerStats.STAT_COUNT;
            if (statName.equals("any")) {
                int[] max = {0};
                CapabilityHelper.withStats(player, s -> {
                    for (int i = 0; i < PlayerStats.STAT_COUNT; i++)
                        if (s.getLevel(i) > max[0]) max[0] = s.getLevel(i);
                });
                return max[0];
            }
            return avg;
        }
        try {
            StatType st = StatType.valueOf(statName.toUpperCase());
            int[] level = {0};
            CapabilityHelper.withStats(player, s -> level[0] = s.getLevel(st.index));
            return level[0];
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getWeaponMastery(Player player) {
        int[] max = {0};
        CapabilityHelper.withWeaponMastery(player, wm -> {
            for (int i = 0; i < tong.statmod.weapon.WeaponMasteryManager.WEAPON_COUNT; i++)
                if (wm.getLevel(i) > max[0]) max[0] = wm.getLevel(i);
        });
        return max[0];
    }
}
