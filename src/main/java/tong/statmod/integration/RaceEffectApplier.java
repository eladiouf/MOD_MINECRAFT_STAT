package tong.statmod.integration;

import net.minecraft.world.entity.player.Player;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public final class RaceEffectApplier {
    private RaceEffectApplier() {}

    public static boolean addLevels(Player player, int statIndex, int amount, PlayerStats data, boolean showFeedback) {
        if (data == null || amount == 0) return false;
        if (statIndex < 0 || statIndex >= StatType.values().length) return false;
        StatType type = StatType.values()[statIndex];
        int currentLevel = data.get(type).level();
        data.setLevel(type, Math.min(100, currentLevel + amount));
        tong.statmod.network.SyncHelper.syncStats(player);
        return true;
    }
}
