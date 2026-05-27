package tong.statmod.client;

import tong.statmod.client.notification.LevelUpToast;
import tong.statmod.stats.StatType;

public class ClientStatsCache {
    private static final int[] levels = new int[23];
    private static final int[] xp = new int[23];
    private static float fatigue = 0;

    public static void updateAll(int[] newLevels, int[] newXp) {
        System.arraycopy(newLevels, 0, levels, 0, Math.min(newLevels.length, 23));
        System.arraycopy(newXp, 0, xp, 0, Math.min(newXp.length, 23));
    }

    public static void updateStat(int index, int level, int statXp) {
        if (index >= 0 && index < 23) {
            int oldLevel = levels[index];
            levels[index] = level;
            xp[index] = statXp;
            if (level > oldLevel) {
                LevelUpToast.onLevelUp(StatType.byIndex(index).displayName, level);
            }
        }
    }

    public static void updateFatigue(float f) { fatigue = f; }

    public static int getLevel(int index) { return levels[index]; }
    public static int getXp(int index) { return xp[index]; }
    public static int getLevel(StatType type) { return levels[type.index]; }
    public static int getXp(StatType type) { return xp[type.index]; }
    public static float getFatigue() { return fatigue; }
}
