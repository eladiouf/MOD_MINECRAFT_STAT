package tong.statmod.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class FtbTaskRegistry {
    private static boolean registered = false;

    @SubscribeEvent
    public static void onCustomTaskEvent(Object event) {
        if (registered) return;
        if (!ModList.get().isLoaded("ftbquests")) return;

        try {
            Class<?> eventClass = Class.forName("dev.ftb.mods.ftbquests.api.event.CustomTaskEvent");
            if (!eventClass.isInstance(event)) return;

            STATMod.LOGGER.info("FTB Quests CustomTaskEvent received — statmod tasks available");
            registered = true;
        } catch (Exception e) {
            STATMod.LOGGER.warn("FTB Quests task registration skipped: {}", e.getMessage());
        }
    }

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
