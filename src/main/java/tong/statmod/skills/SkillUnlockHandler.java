package tong.statmod.skills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class SkillUnlockHandler {
    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            for (StatType stat : StatType.values()) {
                int level = stats.getLevel(stat.index);
                checkAndUnlock(player, stat, level);
            }
        });
    }

    private static final int[] TIERS = {10, 25, 50, 75};

    private static void checkAndUnlock(ServerPlayer player, StatType stat, int level) {
        for (int i = 0; i < TIERS.length; i++) {
            if (level >= TIERS[i]) {
                var skill = SkillUnlockRegistry.getSkill(stat, i);
                if (skill != null) {
                    tong.statmod.integration.EpicFightCompat.grantSkill(player, skill);
                }
            }
        }
    }
}
