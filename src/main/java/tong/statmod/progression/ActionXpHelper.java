package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import tong.statmod.capability.PlayerStats;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;

public class ActionXpHelper {

    public enum XpTier {
        COMMON(2, 3),
        INTERMEDIATE(5, 8),
        RARE(12, 20);

        public final int minXp;
        public final int maxXp;

        XpTier(int min, int max) {
            this.minXp = min;
            this.maxXp = max;
        }
    }

    public static void awardXp(ServerPlayer player, int statIndex, XpTier tier) {
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            int oldLevel = stats.getLevel(statIndex);
            int xp = tier.minXp + player.getRandom().nextInt(tier.maxXp - tier.minXp + 1);
            stats.addXp(statIndex, xp);
            int newLevel = stats.getLevel(statIndex);
            if (newLevel > oldLevel && newLevel > 0) {
                LevelUpHandler.onLevelUp(player, statIndex, newLevel);
            }
            NetworkHandler.sendToPlayer(
                new StatUpdatePacket(statIndex, newLevel, stats.getXp(statIndex)),
                player);
        });
    }
}
