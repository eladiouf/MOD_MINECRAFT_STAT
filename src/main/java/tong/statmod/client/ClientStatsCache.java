package tong.statmod.client;

import net.minecraft.client.Minecraft;
import tong.statmod.client.feedback.CombatFeedbackRenderer;
import tong.statmod.client.notification.LevelUpToast;
import tong.statmod.stats.StatType;

public class ClientStatsCache {
    private static final int[] levels = new int[23];
    private static final int[] xp = new int[23];
    private static float fatigue = 0;
    private static int maxFatigue = 500;
    private static float thirst = 100;
    private static float mana = 0;
    private static int[] perkIds = new int[0];
    private static int perkPoints = 0;
    private static boolean syncReceived = false;

    public static void updateAll(int[] newLevels, int[] newXp) {
        System.arraycopy(newLevels, 0, levels, 0, Math.min(newLevels.length, 23));
        System.arraycopy(newXp, 0, xp, 0, Math.min(newXp.length, 23));
        syncReceived = true;
    }

    public static void updateStat(int index, int level, int statXp) {
        if (index >= 0 && index < 23) {
            int oldLevel = levels[index];
            int oldXp = xp[index];
            levels[index] = level;
            xp[index] = statXp;
            if (level > oldLevel) {
                LevelUpToast.onLevelUp(StatType.byIndex(index).displayName, level);
            }
            if (syncReceived && statXp > oldXp) {
                int xpGained = statXp - oldXp;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    CombatFeedbackRenderer.addXpPopup(mc.player, xpGained, StatType.byIndex(index).displayName);
                }
            }
        }
    }

    public static void updateFatigue(float f) { fatigue = f; }

    public static void updateFatigue(float f, int max) {
        fatigue = f;
        maxFatigue = Math.max(50, max);
    }

    public static void updateThirst(float t) { thirst = t; }

    public static int getLevel(int index) { return levels[index]; }
    public static int getXp(int index) { return xp[index]; }
    public static int getLevel(StatType type) { return levels[type.index]; }
    public static int getXp(StatType type) { return xp[type.index]; }
    public static float getFatigue() { return fatigue; }
    public static int getMaxFatigue() { return maxFatigue; }
    public static float getThirst() { return thirst; }

    public static void updateMana(float m) { mana = m; }
    public static float getMana() { return mana; }
    public static void updatePerks(int[] ids, int points) {
        perkIds = ids;
        perkPoints = points;
    }
    public static int[] getPerkIds() { return perkIds; }
    public static int getPerkPoints() { return perkPoints; }

    public static int getGlobalLevel() {
        int sum = 0;
        for (int i = 0; i < 23; i++) sum += levels[i];
        return Math.round(sum / 23.0f);
    }

    public static float getGlobalXpProgress() {
        float total = 0;
        for (int i = 0; i < 23; i++) {
            int needed = tong.statmod.stats.StatCalculator.getXpForNextLevel(levels[i]);
            total += (needed > 0) ? (float) xp[i] / needed : 1.0f;
        }
        return total / 23.0f;
    }
}
